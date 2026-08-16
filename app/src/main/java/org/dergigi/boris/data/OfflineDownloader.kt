package org.dergigi.boris.data

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dergigi.boris.nostr.Archive
import org.dergigi.boris.nostr.BookmarkRef
import org.dergigi.boris.nostr.BookmarkRefKind
import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Lookmarks
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.NipB0
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import java.util.concurrent.atomic.AtomicBoolean

enum class OfflineShelf(val settingsKey: String) {
    Bookmarks("offlineDownloadBookmarks"),
    Web("offlineDownloadWeb"),
    Lookmarks("offlineDownloadLookmarks"),
    Archive("offlineDownloadArchive"),
    Highlights("offlineDownloadHighlights"),
}

data class OfflineProgress(
    val total: Int = 0,
    val downloaded: Int = 0,
    val running: Boolean = false,
)

/**
 * Prefetches library articles for offline reading. Walks every shelf,
 * fetches missing article text through the reader pipeline (which caches
 * web bodies and nostr events), and prefetches cover images.
 */
object OfflineDownloader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = AtomicBoolean(false)
    private val repository by lazy { ReaderRepository() }

    private val _progress = MutableStateFlow<Map<OfflineShelf, OfflineProgress>>(emptyMap())
    val progress: StateFlow<Map<OfflineShelf, OfflineProgress>> = _progress.asStateFlow()

    fun kickoff(context: Context) {
        if (!active.compareAndSet(false, true)) return
        val app = context.applicationContext
        scope.launch {
            try {
                run(app)
            } finally {
                active.set(false)
            }
        }
    }

    private suspend fun run(app: Context) {
        val session = SessionStore.load(app) ?: return
        val pubkey = session.pubkeyHex
        val readRelays = buildList {
            addAll(RelayList.FALLBACK)
            runCatching { RelayQuery.fetchRelayList(pubkey) }.getOrNull()?.read?.let(::addAll)
        }.distinct()
        val shelves = collectShelves(pubkey, readRelays)
        _progress.value = shelves.mapValues { (_, urls) ->
            OfflineProgress(urls.size, OfflineStore.downloadedCount(urls))
        }
        for ((shelf, urls) in shelves) {
            if (!enabled(shelf)) continue
            update(shelf) { it.copy(running = true) }
            for (url in urls) {
                if (!enabled(shelf)) break
                if (OfflineStore.isDownloaded(url)) continue
                runCatching {
                    val content = repository.fetch(url)
                    content.imageUrl?.let { prefetchImage(app, it) }
                }
                update(shelf) { it.copy(downloaded = OfflineStore.downloadedCount(urls)) }
                delay(100)
            }
            update(shelf) {
                it.copy(downloaded = OfflineStore.downloadedCount(urls), running = false)
            }
        }
    }

    private fun enabled(shelf: OfflineShelf): Boolean =
        SettingsSync.settings.value.offlineDownloadEnabled(shelf.settingsKey)

    private fun update(shelf: OfflineShelf, transform: (OfflineProgress) -> OfflineProgress) {
        val current = _progress.value
        val next = transform(current[shelf] ?: OfflineProgress())
        _progress.value = current + (shelf to next)
    }

    private fun collectShelves(
        pubkey: String,
        readRelays: List<String>,
    ): Map<OfflineShelf, List<String>> {
        val list = runCatching { RelayQuery.fetchBookmarkList(pubkey, readRelays) }.getOrNull()
            ?: EventCache.latest(Nip01Event.KIND_BOOKMARKS, pubkey)
        val web = runCatching { RelayQuery.fetchWebBookmarks(pubkey, readRelays) }
            .getOrDefault(emptyList())
            .ifEmpty { RelayQuery.cachedWebBookmarks(pubkey) }
        val look = runCatching { RelayQuery.fetchLookmarks(pubkey, readRelays) }
            .getOrDefault(emptyList())
            .ifEmpty { RelayQuery.cachedLookmarks(pubkey) }
        val archive = runCatching { RelayQuery.fetchArchiveReactions(pubkey, readRelays) }
            .getOrDefault(emptyList())
            .ifEmpty { RelayQuery.cachedArchiveReactions(pubkey) }
        val highlights = runCatching {
            RelayQuery.fetchRecentHighlights(readRelays, limit = HIGHLIGHT_LIMIT, authors = listOf(pubkey))
        }
            .getOrDefault(emptyList())
            .ifEmpty { RelayQuery.cachedRecentHighlights(limit = HIGHLIGHT_LIMIT, authors = listOf(pubkey)) }

        return mapOf(
            OfflineShelf.Bookmarks to list?.let(Nip51::publicRefs).orEmpty()
                .mapNotNull(::refUrl).distinct(),
            OfflineShelf.Web to web.mapNotNull(NipB0::url).distinct(),
            OfflineShelf.Lookmarks to look.filter(Lookmarks::isLook)
                .mapNotNull(Lookmarks::targetRef).mapNotNull(::refUrl).distinct(),
            OfflineShelf.Archive to archive.filter(Archive::isArchive)
                .mapNotNull(Archive::targetRef).mapNotNull(::refUrl).distinct(),
            OfflineShelf.Highlights to highlights
                .filter { it.pubkey.equals(pubkey, ignoreCase = true) }
                .mapNotNull(Nip84::articleUrl).distinct(),
        )
    }

    private fun refUrl(ref: BookmarkRef): String? = when (ref.kind) {
        BookmarkRefKind.Article -> NostrArticle.fromCoordinate(ref.value)?.uri
        BookmarkRefKind.Url -> ref.value
        BookmarkRefKind.Note -> runCatching { "nostr:${Nip19.noteEncode(ref.value.lowercase())}" }.getOrNull()
    }

    private fun prefetchImage(context: Context, url: String) {
        SingletonImageLoader.get(context).enqueue(
            ImageRequest.Builder(context).data(url).build(),
        )
    }

    private const val HIGHLIGHT_LIMIT = 400
}
