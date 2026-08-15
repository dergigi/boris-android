package org.dergigi.boris.ui.library

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.BookmarkBucket
import org.dergigi.boris.data.BookmarkCatalog
import org.dergigi.boris.data.BookmarkShelves
import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.OgMetaClient
import org.dergigi.boris.data.OgPreview
import org.dergigi.boris.data.SecretBox
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.BookmarkRefKind
import org.dergigi.boris.nostr.BunkerClient
import org.dergigi.boris.nostr.Archive
import org.dergigi.boris.nostr.Lookmarks
import org.dergigi.boris.nostr.BunkerDecryptResult
import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.NipB0
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.RemoteSignerBridge
import org.dergigi.boris.nostr.SignerResults

sealed interface LibraryUiState {
    data object LoggedOut : LibraryUiState
    data object Loading : LibraryUiState
    data class Ready(val shelves: BookmarkShelves) : LibraryUiState
    data object Error : LibraryUiState
}

class LibraryViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _bucket = MutableStateFlow(BookmarkBucket.Public)
    val bucket: StateFlow<BookmarkBucket> = _bucket.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var loadJob: Job? = null
    private var listEvent: Nip01Event? = null
    private var webEvents: List<Nip01Event> = emptyList()
    private var lookEvents: List<Nip01Event> = emptyList()
    private var archiveEvents: List<Nip01Event> = emptyList()
    private var articles: Map<String, Nip01Event> = emptyMap()
    private var notes: Map<String, Nip01Event> = emptyMap()
    private var previews: Map<String, OgPreview?> = emptyMap()
    private var hiddenTags: List<List<String>>? = null

    fun refresh() {
        val session = SessionStore.load(getApplication())
        if (session == null) {
            loadJob?.cancel()
            listEvent = null
            webEvents = emptyList()
            lookEvents = emptyList()
            archiveEvents = emptyList()
            articles = emptyMap()
            notes = emptyMap()
            previews = emptyMap()
            hiddenTags = null
            _state.value = LibraryUiState.LoggedOut
            _refreshing.value = false
            return
        }
        val keep = _state.value is LibraryUiState.Ready
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            var showingContent = keep
            if (!keep) {
                val cached = withContext(Dispatchers.IO) { loadCached(session) }
                if (cached != null) {
                    apply(cached)
                    showingContent = true
                }
            }
            if (showingContent) {
                _refreshing.value = true
            } else {
                _state.value = LibraryUiState.Loading
            }
            try {
                val loaded = withContext(Dispatchers.IO) { load(session) }
                apply(loaded)
                val ciphertext = loaded.list?.content.orEmpty()
                if (hiddenTags == null &&
                    session is Session.Bunker &&
                    Nip51.looksEncrypted(ciphertext)
                ) {
                    decryptWithBunker(session, ciphertext)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (_state.value !is LibraryUiState.Ready) {
                    _state.value = LibraryUiState.Error
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun select(bucket: BookmarkBucket) {
        _bucket.value = bucket
    }

    fun unlockPrivate(): Intent? {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return null
        val ciphertext = listEvent?.content?.takeIf { Nip51.looksEncrypted(it) } ?: return null
        val nip44 = !Nip51.isNip04(ciphertext)
        return when (session) {
            is Session.Amber -> {
                RemoteSignerBridge.buildDecryptIntent(
                    ciphertext = ciphertext,
                    signerPackage = session.signerPackage,
                    currentUserHex = session.pubkeyHex,
                    peerPubkeyHex = session.pubkeyHex,
                    nip44 = nip44,
                )
            }
            is Session.Bunker -> {
                viewModelScope.launch { decryptWithBunker(session, ciphertext) }
                null
            }
        }
    }

    fun onDecryptResult(resultCode: Int, data: Intent?) {
        val plaintext = SignerResults.parsePlaintext(resultCode, data)
        if (plaintext == null) {
            _message.value = getApplication<Application>().getString(R.string.library_unlock_cancelled)
            return
        }
        applyPlaintext(plaintext)
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun applyPlaintext(plaintext: String) {
        val tags = Nip51.parseTagArray(plaintext)
        if (tags == null) {
            _message.value = getApplication<Application>().getString(R.string.library_unlock_failed)
            return
        }
        hiddenTags = tags
        viewModelScope.launch {
            try {
                val extra = withContext(Dispatchers.IO) {
                    hydrate(Nip51.parseTags(tags), webEvents)
                }
                articles = articles + extra.articles
                notes = notes + extra.notes
                previews = previews + extra.previews
            } catch (_: Exception) {
            }
            publish()
        }
    }

    private fun apply(loaded: Loaded) {
        val previousListId = listEvent?.id
        val previousHidden = hiddenTags
        listEvent = loaded.list
        webEvents = loaded.web
        lookEvents = loaded.look
        archiveEvents = loaded.archive
        articles = loaded.articles
        notes = loaded.notes
        previews = loaded.previews
        hiddenTags = if (loaded.list?.id == previousListId) previousHidden else null
        publish()
    }

    private fun publish() {
        _state.value = LibraryUiState.Ready(
            BookmarkCatalog.build(
                listEvent = listEvent,
                hiddenTags = hiddenTags,
                webEvents = webEvents,
                lookEvents = lookEvents,
                archiveEvents = archiveEvents,
                articles = articles,
                notes = notes,
                previews = previews,
            ),
        )
    }

    private suspend fun decryptWithBunker(session: Session.Bunker, ciphertext: String) {
        val app = getApplication<Application>()
        val privkey = SecretBox.unwrap(app, session.clientPrivkeyCiphertext)
        if (privkey == null) {
            _message.value = app.getString(R.string.library_unlock_failed)
            return
        }
        try {
            val result = withContext(Dispatchers.IO) {
                BunkerClient(onAuthUrl = ::openAuthUrl).decrypt(
                    session.relays,
                    session.remoteSignerPubkey,
                    privkey,
                    session.pubkeyHex,
                    ciphertext,
                    nip44 = !Nip51.isNip04(ciphertext),
                )
            }
            when (result) {
                is BunkerDecryptResult.Plaintext -> applyPlaintext(result.value)
                BunkerDecryptResult.Rejected, BunkerDecryptResult.RelayTimeout -> {
                    _message.value = app.getString(R.string.library_unlock_failed)
                }
            }
        } finally {
            privkey.fill(0)
        }
    }

    /** Builds shelves purely from the local event cache; no sockets are opened. */
    private fun loadCached(session: Session): Loaded? {
        val pubkey = session.pubkeyHex
        val list = EventCache.latest(Nip01Event.KIND_BOOKMARKS, pubkey)
        val web = RelayQuery.cachedWebBookmarks(pubkey)
        val look = RelayQuery.cachedLookmarks(pubkey)
        val archive = RelayQuery.cachedArchiveReactions(pubkey)
        if (list == null && web.isEmpty() && look.isEmpty() && archive.isEmpty()) return null
        val refs = buildList {
            if (list != null) addAll(Nip51.publicRefs(list))
            addAll(look.mapNotNull(Lookmarks::targetRef))
            addAll(archive.mapNotNull(Archive::targetRef))
        }
        val cachedArticles = refs.filter { it.kind == BookmarkRefKind.Article }
            .distinctBy { it.value }
            .mapNotNull { ref ->
                val article = NostrArticle.fromCoordinate(ref.value) ?: return@mapNotNull null
                EventCache.latest(article.pointer.kind, article.pointer.pubkey, article.pointer.identifier)
                    ?.let { ref.value to it }
            }
            .toMap()
        val cachedNotes = refs.filter { it.kind == BookmarkRefKind.Note }
            .map { it.value.lowercase() }
            .distinct()
            .mapNotNull { id -> EventCache.event(id)?.let { id to it } }
            .toMap()
        return Loaded(list, web, look, archive, cachedArticles, cachedNotes, emptyMap())
    }

    private suspend fun load(session: Session): Loaded {
        val readRelays = buildList {
            addAll(RelayList.FALLBACK)
            addAll(RelayQuery.fetchRelayList(session.pubkeyHex).read)
        }.distinct()
        val list = RelayQuery.fetchBookmarkList(session.pubkeyHex, readRelays)
        val web = RelayQuery.fetchWebBookmarks(session.pubkeyHex, readRelays)
        val look = RelayQuery.fetchLookmarks(session.pubkeyHex, readRelays)
        val archive = RelayQuery.fetchArchiveReactions(session.pubkeyHex, readRelays)
        val refs = buildList {
            if (list != null) addAll(Nip51.publicRefs(list))
            addAll(look.mapNotNull(Lookmarks::targetRef))
            addAll(archive.mapNotNull(Archive::targetRef))
        }
        val hydrated = hydrate(refs, web)
        return Loaded(list, web, look, archive, hydrated.articles, hydrated.notes, hydrated.previews)
    }

    private suspend fun hydrate(
        refs: List<org.dergigi.boris.nostr.BookmarkRef>,
        web: List<Nip01Event>,
    ): Hydrated = coroutineScope {
        val articleRefs = refs.filter { it.kind == BookmarkRefKind.Article }
            .distinctBy { it.value }
            .take(ARTICLE_LIMIT)
        val articleJobs = articleRefs.map { ref ->
            async {
                val article = NostrArticle.fromCoordinate(ref.value) ?: return@async null
                ref.value to RelayQuery.fetchArticle(article.pointer)
            }
        }
        val noteIds = refs.filter { it.kind == BookmarkRefKind.Note }
            .map { it.value }
            .distinct()
            .take(NOTE_LIMIT)
        val notesJob = async { RelayQuery.fetchEvents(noteIds) }
        val httpUrls = buildList {
            refs.filter { it.kind == BookmarkRefKind.Url }.forEach { add(it.value) }
            web.forEach { event -> NipB0.url(event)?.let(::add) }
        }.distinct().take(PREVIEW_LIMIT)
        val previewJobs = httpUrls.map { url ->
            async { url to runCatching { OgMetaClient.fetch(url) }.getOrNull() }
        }
        val fetchedArticles = articleJobs.awaitAll()
            .mapNotNull { pair ->
                val (coordinate, event) = pair ?: return@mapNotNull null
                event?.let { coordinate to it }
            }
            .toMap()
        Hydrated(fetchedArticles, notesJob.await(), previewJobs.awaitAll().toMap())
    }

    private fun openAuthUrl(url: String) {
        val app = getApplication<Application>()
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private data class Loaded(
        val list: Nip01Event?,
        val web: List<Nip01Event>,
        val look: List<Nip01Event>,
        val archive: List<Nip01Event>,
        val articles: Map<String, Nip01Event>,
        val notes: Map<String, Nip01Event>,
        val previews: Map<String, OgPreview?>,
    )

    private data class Hydrated(
        val articles: Map<String, Nip01Event>,
        val notes: Map<String, Nip01Event>,
        val previews: Map<String, OgPreview?>,
    )

    companion object {
        private const val ARTICLE_LIMIT = 48
        private const val NOTE_LIMIT = 48
        private const val PREVIEW_LIMIT = 20
    }
}
