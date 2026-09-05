package org.dergigi.boris.ui.you

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.data.ArticleCache
import org.dergigi.boris.data.ArticlePreview
import org.dergigi.boris.data.ArticleUrl
import org.dergigi.boris.data.BookmarkCatalog
import org.dergigi.boris.data.BookmarkBucket
import org.dergigi.boris.data.BookmarkItem
import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.OgMetaClient
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.BookmarkRefKind
import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.HintedRelays
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.NipB0
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.SocialGraphs
import org.dergigi.boris.ui.ContentTab
import org.dergigi.boris.ui.feed.FeedLevel
import org.dergigi.boris.ui.feed.classifyFeedLevel

data class YouHighlight(
    val id: String,
    val quote: String,
    val context: String? = null,
    val comment: String? = null,
    val url: String?,
    val host: String?,
    val createdAt: Long,
)

data class YouWriting(
    val id: String,
    val title: String,
    val summary: String?,
    val imageUrl: String?,
    val url: String,
    val publishedAt: Long,
)

internal fun YouHighlight.matchesQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return quote.contains(q, ignoreCase = true) ||
        context.orEmpty().contains(q, ignoreCase = true) ||
        comment.orEmpty().contains(q, ignoreCase = true) ||
        host.orEmpty().contains(q, ignoreCase = true)
}

internal fun YouWriting.matchesQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return title.contains(q, ignoreCase = true) ||
        summary.orEmpty().contains(q, ignoreCase = true)
}

internal fun BookmarkItem.matchesQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return title.contains(q, ignoreCase = true) ||
        summary.orEmpty().contains(q, ignoreCase = true) ||
        host.orEmpty().contains(q, ignoreCase = true) ||
        url.orEmpty().contains(q, ignoreCase = true)
}

internal sealed class YouMergedItem {
    abstract val sortAt: Long
    abstract val key: String

    data class Highlight(val item: YouHighlight) : YouMergedItem() {
        override val sortAt: Long get() = item.createdAt
        override val key: String get() = "h:${item.id}"
    }

    data class Writing(val item: YouWriting) : YouMergedItem() {
        override val sortAt: Long get() = item.publishedAt
        override val key: String get() = "w:${item.id}"
    }

    data class Bookmark(val item: BookmarkItem) : YouMergedItem() {
        override val sortAt: Long get() = item.createdAt
        override val key: String get() = "b:${item.id}"
    }
}

internal fun mergeYouItems(
    highlights: List<YouHighlight>,
    writings: List<YouWriting>,
    publicBookmarks: List<BookmarkItem>,
    webBookmarks: List<BookmarkItem>,
    associatedArticles: List<BookmarkItem>,
    deletedIds: Set<String> = emptySet(),
    query: String = "",
): List<YouMergedItem> = buildList {
    val visibleBookmarkUrls = (publicBookmarks + webBookmarks)
        .filter { it.matchesQuery(query) }
        .mapNotNull { item -> item.url?.let(ArticleUrl::normalize) }
        .toSet()
    highlights.filter { it.id !in deletedIds && it.matchesQuery(query) }
        .forEach { add(YouMergedItem.Highlight(it)) }
    writings.filter { it.matchesQuery(query) }
        .forEach { add(YouMergedItem.Writing(it)) }
    publicBookmarks.filter { it.matchesQuery(query) }
        .forEach { add(YouMergedItem.Bookmark(it)) }
    webBookmarks.filter { it.matchesQuery(query) }
        .forEach { add(YouMergedItem.Bookmark(it)) }
    associatedArticles.filter { item ->
        val associatedUrl = item.url?.let(ArticleUrl::normalize)
        item.matchesQuery(query) &&
            (associatedUrl == null || associatedUrl !in visibleBookmarkUrls)
    }
        .forEach { add(YouMergedItem.Bookmark(it)) }
}.sortedByDescending { it.sortAt }

sealed interface YouUiState {
    data object Loading : YouUiState
    data class Ready(
        val highlights: List<YouHighlight>,
        val writings: List<YouWriting>,
        val publicBookmarks: List<BookmarkItem> = emptyList(),
        val webBookmarks: List<BookmarkItem> = emptyList(),
        val associatedArticles: List<BookmarkItem> = emptyList(),
    ) : YouUiState
    data object Error : YouUiState
}

class YouViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<YouUiState>(YouUiState.Loading)
    val state: StateFlow<YouUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _relation = MutableStateFlow(FeedLevel.Nostrverse)
    val relation: StateFlow<FeedLevel> = _relation.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached.asStateFlow()

    private var loadJob: Job? = null
    private var moreJob: Job? = null
    private var pubkeyHex: String = ""

    /**
     * [tab] scopes a pull-to-refresh to the visible tab's kind; null (initial
     * load, error retry) refreshes everything on the page.
     */
    fun refresh(pubkeyHex: String = this.pubkeyHex, tab: ContentTab? = null) {
        val key = pubkeyHex.trim().lowercase().takeIf { it.length == 64 }
            ?: SessionStore.load(getApplication())?.pubkeyHex
        if (key.isNullOrBlank()) {
            loadJob?.cancel()
            moreJob?.cancel()
            this.pubkeyHex = ""
            _profile.value = null
            _relation.value = FeedLevel.Nostrverse
            _state.value = YouUiState.Ready(emptyList(), emptyList())
            _refreshing.value = false
            return
        }
        val samePerson = key == this.pubkeyHex
        if (!samePerson) {
            _profile.value = null
        }
        this.pubkeyHex = key
        _relation.value = relationFor(key, remote = false)
        val keepItems = samePerson && _state.value is YouUiState.Ready
        loadJob?.cancel()
        moreJob?.cancel()
        if (tab == null || tab == ContentTab.All || tab == ContentTab.Highlights) {
            _endReached.value = false
        }
        loadJob = viewModelScope.launch {
            var showing = keepItems
            if (!keepItems) {
                val cached = withContext(Dispatchers.IO) { loadCached(key) }
                if (cached != null) {
                    _profile.value = cached.profile
                    _relation.value = cached.relation
                    _state.value = YouUiState.Ready(
                        cached.highlights,
                        cached.writings,
                        cached.publicBookmarks,
                        cached.webBookmarks,
                        cached.associatedArticles,
                    )
                    showing = true
                }
            }
            if (showing) {
                _refreshing.value = true
            } else {
                _state.value = YouUiState.Loading
            }
            try {
                val loaded = withContext(Dispatchers.IO) {
                    // Their content lives on their write relays (NIP-65 outbox).
                    val list = RelayQuery.fetchRelayList(key)
                    val relays = buildList {
                        addAll(RelayList.FALLBACK)
                        addAll(list.write)
                        addAll(list.read)
                        addAll(HintedRelays.forPubkey(key))
                    }.distinct()
                    val all = tab == null || tab == ContentTab.All
                    val wantHighlights = all || tab == ContentTab.Highlights
                    val wantWritings = all || tab == ContentTab.Writings
                    val wantArticles = all || tab == ContentTab.Articles
                    val wantPublic = all || tab == ContentTab.Public
                    val wantWeb = all || tab == ContentTab.Web
                    val shown = if (keepItems) _state.value as? YouUiState.Ready else null
                    coroutineScope {
                        val highlights = async {
                            if (wantHighlights) {
                                RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT, key)
                                    .map { event -> highlightFrom(event) }
                            } else {
                                // Keep paged-in items instead of truncating to the first page.
                                shown?.highlights ?: cachedHighlights(key)
                            }
                        }
                        val writings = async {
                            if (wantWritings) {
                                RelayQuery.fetchLongFormArticles(key, relays)
                                    .mapNotNull { event -> writingFrom(event) }
                            } else {
                                cachedWritings(key)
                            }
                        }
                        val bookmarks = async {
                            if (wantPublic || wantWeb) {
                                val list = if (wantPublic) {
                                    RelayQuery.fetchBookmarkList(key, relays)
                                } else {
                                    EventCache.latest(Nip01Event.KIND_BOOKMARKS, key)
                                }
                                val web = if (wantWeb) {
                                    RelayQuery.fetchWebBookmarks(key, relays)
                                } else {
                                    RelayQuery.cachedWebBookmarks(key)
                                }
                                val hydrated = hydrateBookmarks(list, web, remote = true)
                                val publicItems = if (wantPublic) {
                                    hydrated.first
                                } else {
                                    shown?.publicBookmarks ?: hydrated.first
                                }
                                val webItems = if (wantWeb) {
                                    hydrated.second
                                } else {
                                    shown?.webBookmarks ?: hydrated.second
                                }
                                publicItems to webItems
                            } else {
                                (shown?.publicBookmarks ?: emptyList()) to
                                    (shown?.webBookmarks ?: emptyList())
                            }
                        }
                        val associatedArticles = async {
                            if (wantArticles) {
                                cachedAssociatedArticles(key)
                            } else {
                                shown?.associatedArticles ?: cachedAssociatedArticles(key)
                            }
                        }
                        // A tab-scoped pull only re-queries that tab's kind;
                        // profile and relation stay on cache.
                        val profile = async {
                            if (tab == null) {
                                RelayQuery.fetchProfile(key)
                            } else {
                                _profile.value ?: RelayQuery.fetchProfile(key)
                            }
                        }
                        val relation = async { relationFor(key, remote = tab == null) }
                        val (publicBookmarks, webBookmarks) = bookmarks.await()
                        Loaded(
                            highlights.await(),
                            writings.await(),
                            publicBookmarks,
                            webBookmarks,
                            associatedArticles.await(),
                            profile.await(),
                            relation.await(),
                        )
                    }
                }
                _profile.value = loaded.profile
                _relation.value = loaded.relation
                _state.value = YouUiState.Ready(
                    loaded.highlights,
                    loaded.writings,
                    loaded.publicBookmarks,
                    loaded.webBookmarks,
                    loaded.associatedArticles,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (_state.value !is YouUiState.Ready) {
                    _state.value = YouUiState.Error
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    /**
     * Pages in highlights older than the oldest one currently shown, using a
     * NIP-01 "until" filter. Marks the end once a page brings nothing new.
     */
    fun loadMoreHighlights() {
        val key = pubkeyHex.takeIf { it.isNotBlank() } ?: return
        val ready = _state.value as? YouUiState.Ready ?: return
        val oldest = ready.highlights.minOfOrNull { it.createdAt } ?: return
        if (_loadingMore.value || _endReached.value) return
        moreJob = viewModelScope.launch {
            _loadingMore.value = true
            try {
                val older = withContext(Dispatchers.IO) {
                    val list = RelayQuery.fetchRelayList(key)
                    val relays = buildList {
                        addAll(RelayList.FALLBACK)
                        addAll(list.write)
                        addAll(list.read)
                    }.distinct()
                    RelayQuery.fetchHighlightsBefore(relays, key, until = oldest, limit = HIGHLIGHT_LIMIT)
                        .map { event -> highlightFrom(event) }
                }
                val current = _state.value as? YouUiState.Ready ?: return@launch
                val known = current.highlights.mapTo(mutableSetOf()) { it.id }
                val fresh = older.filter { it.id !in known }
                if (fresh.isEmpty()) {
                    _endReached.value = true
                } else {
                    _state.value = current.copy(
                        highlights = (current.highlights + fresh).sortedByDescending { it.createdAt },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep what we have; tapping the button again retries.
            } finally {
                _loadingMore.value = false
            }
        }
    }

    private fun loadCached(key: String): Loaded? {
        val highlights = cachedHighlights(key)
        val writings = cachedWritings(key)
        val (publicBookmarks, webBookmarks) = hydrateBookmarks(
            EventCache.latest(Nip01Event.KIND_BOOKMARKS, key),
            RelayQuery.cachedWebBookmarks(key),
            remote = false,
        )
        val associatedArticles = cachedAssociatedArticles(key)
        val profile = RelayQuery.cachedProfiles(listOf(key))[key]
        if (highlights.isEmpty() && writings.isEmpty() &&
            publicBookmarks.isEmpty() && webBookmarks.isEmpty() &&
            associatedArticles.isEmpty() && profile == null
        ) {
            return null
        }
        return Loaded(
            highlights,
            writings,
            publicBookmarks,
            webBookmarks,
            associatedArticles,
            profile,
            relationFor(key, remote = false),
        )
    }

    private fun hydrateBookmarks(
        list: Nip01Event?,
        web: List<Nip01Event>,
        remote: Boolean,
    ): Pair<List<BookmarkItem>, List<BookmarkItem>> {
        val refs = list?.let { Nip51.publicRefs(it) }.orEmpty()
        val articles = linkedMapOf<String, Nip01Event>()
        for (ref in refs.filter { it.kind == BookmarkRefKind.Article }.distinctBy { it.value }.take(ARTICLE_LIMIT)) {
            val article = NostrArticle.fromCoordinate(ref.value) ?: continue
            val event = if (remote) {
                RelayQuery.fetchArticle(article.pointer)
            } else {
                EventCache.latest(article.pointer.kind, article.pointer.pubkey, article.pointer.identifier)
            }
            if (event != null) articles[ref.value] = event
        }
        val noteIds = refs.filter { it.kind == BookmarkRefKind.Note }
            .map { it.value.lowercase() }
            .distinct()
            .take(NOTE_LIMIT)
        val notes = if (remote && noteIds.isNotEmpty()) {
            RelayQuery.fetchEvents(noteIds)
        } else {
            noteIds.mapNotNull { id -> EventCache.event(id)?.let { id to it } }.toMap()
        }
        val httpUrls = buildList {
            refs.filter { it.kind == BookmarkRefKind.Url }.forEach { add(it.value) }
            web.forEach { event -> NipB0.url(event)?.let(::add) }
        }.distinct().take(PREVIEW_LIMIT)
        val previews = httpUrls.associateWith { url ->
            ArticlePreview.get(url)
                ?: if (remote) runCatching { OgMetaClient.fetch(url) }.getOrNull() else null
        }
        val shelves = BookmarkCatalog.build(
            listEvent = list,
            hiddenTags = emptyList(),
            webEvents = web,
            articles = articles,
            notes = notes,
            previews = previews,
        )
        return shelves.public to shelves.web
    }

    private fun cachedHighlights(key: String): List<YouHighlight> =
        RelayQuery.cachedRecentHighlights(HIGHLIGHT_LIMIT, key).map { event -> highlightFrom(event) }

    private fun cachedWritings(key: String): List<YouWriting> =
        RelayQuery.cachedRecentWritings(WRITING_LIMIT, key).mapNotNull { event -> writingFrom(event) }

    private fun cachedAssociatedArticles(key: String): List<BookmarkItem> =
        ArticleCache.byAuthor(key)
            .take(ASSOCIATED_ARTICLE_LIMIT)
            .map { cached ->
                val content = cached.content
                val host = ArticleUrl.host(content.url)
                BookmarkItem(
                    id = "c:${ArticleUrl.normalize(content.url)}",
                    title = content.title?.takeIf { it.isNotBlank() }
                        ?: host
                        ?: content.url,
                    url = content.url,
                    host = host,
                    imageUrl = content.imageUrl,
                    createdAt = content.publishedAt ?: cached.storedAt,
                    bucket = BookmarkBucket.Web,
                    summary = content.summary,
                )
            }

    private fun highlightFrom(event: Nip01Event): YouHighlight {
        val url = Nip84.articleUrl(event)
        return YouHighlight(
            id = event.id,
            quote = event.content.trim(),
            context = event.tagValue("context"),
            comment = Nip84.comment(event),
            url = url,
            host = url?.let { ArticleUrl.host(it) },
            createdAt = event.createdAt,
        )
    }

    private fun relationFor(profileHex: String, remote: Boolean): FeedLevel {
        val sessionHex = SessionStore.load(getApplication())?.pubkeyHex
        val graph = if (remote) SocialGraphs.fetch(sessionHex) else SocialGraphs.cached(sessionHex)
        return classifyFeedLevel(profileHex, graph)
    }

    private data class Loaded(
        val highlights: List<YouHighlight>,
        val writings: List<YouWriting>,
        val publicBookmarks: List<BookmarkItem>,
        val webBookmarks: List<BookmarkItem>,
        val associatedArticles: List<BookmarkItem>,
        val profile: Profile?,
        val relation: FeedLevel,
    )

    companion object {
        private const val HIGHLIGHT_LIMIT = 80
        private const val WRITING_LIMIT = 200
        private const val ARTICLE_LIMIT = 48
        private const val NOTE_LIMIT = 48
        private const val PREVIEW_LIMIT = 20
        private const val ASSOCIATED_ARTICLE_LIMIT = 80
        private const val UNTITLED = "Untitled"

        internal fun writingFrom(event: Nip01Event): YouWriting? {
            val identifier = Nip23.identifier(event) ?: return null
            val article = NostrArticle.fromCoordinate(
                "${Nip01Event.KIND_LONG_FORM}:${event.pubkey}:$identifier",
            ) ?: return null
            return YouWriting(
                id = event.id,
                title = Nip23.title(event) ?: UNTITLED,
                summary = Nip23.summary(event),
                imageUrl = Nip23.image(event),
                url = article.uri,
                publishedAt = Nip23.publishedAt(event),
            )
        }
    }
}
