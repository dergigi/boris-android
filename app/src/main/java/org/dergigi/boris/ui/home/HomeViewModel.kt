package org.dergigi.boris.ui.home

import android.app.Application
import android.content.Intent
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.dergigi.boris.data.ArchivedArticles
import org.dergigi.boris.data.ArticlePreview
import org.dergigi.boris.data.BookmarkCatalog
import org.dergigi.boris.data.BookmarkItem
import org.dergigi.boris.data.ContinueReading
import org.dergigi.boris.data.HighlightedArticle
import org.dergigi.boris.data.HighlightedArticles
import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.OgMetaClient
import org.dergigi.boris.data.OgPreview
import org.dergigi.boris.data.OgPreviewCache
import org.dergigi.boris.data.HomeFilters
import org.dergigi.boris.data.MostHighlightedWindow
import org.dergigi.boris.data.RandomArticles
import org.dergigi.boris.data.ReadingTimes
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.data.TimedReadKind
import org.dergigi.boris.data.TimedReads
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.Archive
import org.dergigi.boris.nostr.ArticleReactions
import org.dergigi.boris.nostr.BookmarkRefKind
import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.SocialGraphs
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.tts.startListening as startArticleListening
import org.dergigi.boris.ui.MarkAsReadAction
import org.dergigi.boris.ui.feed.foafFetchAuthors

sealed interface HomeHighlightsState {
    data object Loading : HomeHighlightsState
    data object Empty : HomeHighlightsState
    data object Error : HomeHighlightsState
    data class Ready(
        val yours: List<HighlightedArticle>,
        val friends: List<HighlightedArticle>,
        val likedFriends: List<HighlightedArticle> = emptyList(),
        val readFriends: List<HighlightedArticle> = emptyList(),
        val foaf: List<HighlightedArticle> = emptyList(),
        val likedFoaf: List<HighlightedArticle> = emptyList(),
        val readFoaf: List<HighlightedArticle> = emptyList(),
        val others: List<HighlightedArticle>,
        val likedOthers: List<HighlightedArticle> = emptyList(),
        val readOthers: List<HighlightedArticle> = emptyList(),
        val loggedIn: Boolean,
        val archivedKeys: Set<String> = emptySet(),
        val continueReading: List<HighlightedArticle> = emptyList(),
        val mostHighlighted: List<HighlightedArticle> = emptyList(),
        val hasMostPool: Boolean = false,
        val shortReads: List<HighlightedArticle> = emptyList(),
        val longReads: List<HighlightedArticle> = emptyList(),
        val randomArticles: List<HighlightedArticle> = emptyList(),
    ) : HomeHighlightsState
}

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _highlights = MutableStateFlow<HomeHighlightsState>(HomeHighlightsState.Loading)
    val highlights: StateFlow<HomeHighlightsState> = _highlights.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var loadJob: Job? = null
    private var listenJob: Job? = null
    private var loadedAt: Long? = null
    private val markAsReadAction = MarkAsReadAction(
        app = application,
        scope = viewModelScope,
        onMessage = { _message.value = it },
        onArchived = { key, _, _ ->
            val current = _highlights.value
            if (key != null && current is HomeHighlightsState.Ready) {
                _highlights.value = current.copy(archivedKeys = current.archivedKeys + key)
            }
        },
    )

    fun consumeMessage() {
        _message.value = null
    }

    /**
     * Stale-while-revalidate. Resuming Home or Explore re-reads local state
     * only; relays are hit when the last load is older than
     * [REFRESH_INTERVAL_MS] or [force] is set (pull to refresh, retry).
     */
    fun refresh(force: Boolean = false) {
        val ready = _highlights.value is HomeHighlightsState.Ready
        if (!shouldReload(loadedAt, System.currentTimeMillis(), loadJob?.isActive == true, ready, force)) {
            if (ready) viewModelScope.launch { refreshLocal() }
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val pubkey = SessionStore.load(getApplication())?.pubkeyHex
            val keep = _highlights.value is HomeHighlightsState.Ready
            val cached = withContext(Dispatchers.IO) { loadCached(pubkey) }
            if (cached != null) {
                _highlights.value = cached
            }
            val showing = keep || cached != null
            if (showing) {
                _refreshing.value = true
            } else {
                _highlights.value = HomeHighlightsState.Loading
            }
            try {
                val rows = withContext(Dispatchers.IO) {
                    coroutineScope {
                        val graph = SocialGraphs.fetch(pubkey)
                        val friendKeys = graph.friends.let { keys ->
                            if (pubkey == null) keys else keys - pubkey.lowercase()
                        }
                        val relays = graph.relays
                        val foafKeys = graph.foaf
                        val yoursDeferred = async {
                            if (pubkey == null) emptyList() else loadYours(relays, pubkey)
                        }
                        val friendsDeferred = async { loadFriends(relays, friendKeys) }
                        val foafDeferred = async { loadFoaf(relays, foafKeys) }
                        val othersDeferred = async { loadOthers(pubkey, friendKeys, foafKeys) }
                        val mostDeferred = async { fetchMostHighlighted() }
                        val archiveDeferred = async {
                            if (pubkey == null) {
                                emptySet()
                            } else {
                                ArchivedArticles.keys(RelayQuery.fetchArchiveReactions(pubkey, relays))
                            }
                        }
                        val rawYours = HighlightedArticles.hydrate(yoursDeferred.await())
                        val friends = friendsDeferred.await()
                        val rawFriends = HighlightedArticles.hydrate(friends.highlights)
                        val rawLikedFriends = HighlightedArticles.hydrate(friends.likes)
                        val rawReadFriends = HighlightedArticles.hydrate(friends.reads)
                        val foaf = foafDeferred.await()
                        val rawFoaf = HighlightedArticles.hydrate(foaf.highlights)
                        val rawLikedFoaf = HighlightedArticles.hydrate(foaf.likes)
                        val rawReadFoaf = HighlightedArticles.hydrate(foaf.reads)
                        val others = othersDeferred.await()
                        val rawOthers = HighlightedArticles.hydrate(others.highlights)
                        val rawLikedOthers = HighlightedArticles.hydrate(others.likes)
                        val rawReadOthers = HighlightedArticles.hydrate(others.reads)
                        val rawContinue = HighlightedArticles.hydrate(ContinueReading.articles(ARTICLE_LIMIT))
                        mostDeferred.await()
                        val rawMost = HighlightedArticles.hydrate(mostHighlightedRows())
                        val hasMostPool = hasMostPool()
                        archiveDeferred.await()
                        val feedUrls = (
                            rawYours + rawFriends + rawLikedFriends + rawReadFriends +
                                rawFoaf + rawLikedFoaf + rawReadFoaf +
                                rawOthers + rawLikedOthers + rawReadOthers +
                                rawContinue + rawMost
                            )
                            .map { it.url }
                        if (pubkey != null) {
                            RelayQuery.fetchArchivesForUrls(pubkey, relays, feedUrls)
                        }
                        val archivedKeys = if (pubkey == null) {
                            emptySet()
                        } else {
                            ArchivedArticles.keys(RelayQuery.cachedArchiveReactions(pubkey))
                        }
                        val library = if (pubkey == null) {
                            emptyList()
                        } else {
                            libraryItems(pubkey, relays)
                        }
                        val libraryRows = libraryHighlights(library, archivedKeys, fetchUnknownWeb = true)
                        val rawShort = HighlightedArticles.hydrate(libraryRows.shortReads)
                        val rawLong = HighlightedArticles.hydrate(libraryRows.longReads)
                        val rawRandom = HighlightedArticles.hydrate(libraryRows.randomArticles)
                        val libraryUrls = (rawShort + rawLong + rawRandom).map { it.url }
                        if (pubkey != null && libraryUrls.isNotEmpty()) {
                            RelayQuery.fetchArchivesForUrls(pubkey, relays, libraryUrls)
                        }
                        val keys = if (pubkey == null) {
                            archivedKeys
                        } else {
                            ArchivedArticles.keys(RelayQuery.cachedArchiveReactions(pubkey))
                        }
                        LoadedRows(
                            rawYours,
                            rawFriends,
                            rawLikedFriends,
                            rawReadFriends,
                            rawFoaf,
                            rawLikedFoaf,
                            rawReadFoaf,
                            rawOthers,
                            rawLikedOthers,
                            rawReadOthers,
                            keys,
                            rawContinue,
                            rawMost,
                            hasMostPool,
                            rawShort,
                            rawLong,
                            rawRandom,
                        )
                    }
                }
                loadedAt = System.currentTimeMillis()
                if (rows.isEmpty()) {
                    _highlights.value = HomeHighlightsState.Empty
                } else {
                    // Show rows right away with whatever previews are cached;
                    // OG metadata fetches decorate them in a second pass.
                    _highlights.value = rows.toReady(pubkey, emptyMap())
                    val previews = withContext(Dispatchers.IO) {
                        loadPreviews(rows.urls())
                    }
                    _highlights.value = rows.toReady(pubkey, previews)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (_highlights.value !is HomeHighlightsState.Ready) {
                    _highlights.value = HomeHighlightsState.Error
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    /** Cheap local pass on resume: reading positions and archive marks change while reading. */
    private suspend fun refreshLocal() {
        val pubkey = SessionStore.load(getApplication())?.pubkeyHex
        val (continueReading, archivedKeys) = withContext(Dispatchers.IO) {
            val keys = if (pubkey == null) {
                emptySet()
            } else {
                ArchivedArticles.keys(RelayQuery.cachedArchiveReactions(pubkey))
            }
            applyPreviews(ContinueReading.articles(ARTICLE_LIMIT), emptyMap()) to keys
        }
        val latest = _highlights.value as? HomeHighlightsState.Ready ?: return
        if (latest.continueReading == continueReading && latest.archivedKeys == archivedKeys) return
        _highlights.value = latest.copy(continueReading = continueReading, archivedKeys = archivedKeys)
    }

    fun startListening(url: String) {
        listenJob?.cancel()
        listenJob = viewModelScope.launch {
            startArticleListening(getApplication(), url)?.let { _message.value = it }
        }
    }

    fun refreshMostHighlighted() {
        viewModelScope.launch {
            val current = _highlights.value as? HomeHighlightsState.Ready
            val instant = withContext(Dispatchers.IO) {
                val ranked = HighlightedArticles.hydrate(mostHighlightedRows())
                val previews = ranked
                    .map { it.url }
                    .distinct()
                    .associateWith { ArticlePreview.get(it) }
                applyPreviews(ranked, previews) to hasMostPool()
            }
            val afterCache = _highlights.value as? HomeHighlightsState.Ready ?: current
            if (afterCache != null) {
                _highlights.value = afterCache.copy(
                    mostHighlighted = instant.first,
                    hasMostPool = instant.second,
                )
            }
            val next = withContext(Dispatchers.IO) {
                fetchMostHighlighted()
                val ranked = HighlightedArticles.hydrate(mostHighlightedRows())
                val previews = ranked
                    .map { it.url }
                    .distinct()
                    .associateWith { ArticlePreview.get(it) }
                applyPreviews(ranked, previews) to hasMostPool()
            }
            val latest = _highlights.value as? HomeHighlightsState.Ready ?: return@launch
            _highlights.value = latest.copy(
                mostHighlighted = next.first,
                hasMostPool = next.second,
            )
        }
    }

    fun refreshRandomArticles() {
        viewModelScope.launch {
            val current = _highlights.value as? HomeHighlightsState.Ready ?: return@launch
            val pubkey = SessionStore.load(getApplication())?.pubkeyHex ?: return@launch
            val settings = SettingsSync.settings.value
            val next = withContext(Dispatchers.IO) {
                val pool = HomeFilters.visible(
                    RandomArticles.articles(
                        cachedLibraryItems(pubkey),
                        current.archivedKeys,
                        ARTICLE_LIMIT * 4,
                    ),
                    current.archivedKeys,
                    hideArchived = settings.hideArchivedOnHome,
                    hideCompleted = settings.hideCompletedOnHome,
                    hideNsfw = settings.hideNsfwOnHome,
                ).take(ARTICLE_LIMIT)
                if (pool.isEmpty()) return@withContext emptyList()
                val hydrated = HighlightedArticles.hydrate(pool)
                val previews = hydrated
                    .map { it.url }
                    .distinct()
                    .associateWith { ArticlePreview.get(it) }
                applyPreviews(hydrated, previews)
            }
            val latest = _highlights.value as? HomeHighlightsState.Ready ?: return@launch
            _highlights.value = latest.copy(randomArticles = next)
        }
    }

    fun markAsRead(article: HighlightedArticle): Intent? =
        markAsReadAction.request(article.url, article.title, article.imageUrl)

    fun onSignerResult(resultCode: Int, data: Intent?) {
        markAsReadAction.onSignerResult(resultCode, data)
    }

    private fun loadCached(pubkey: String?): HomeHighlightsState.Ready? {
        val graph = SocialGraphs.cached(pubkey)
        val friendKeys = graph.friends.let { keys ->
            if (pubkey == null) keys else keys - pubkey.lowercase()
        }
        val yours = if (pubkey == null) {
            emptyList()
        } else {
            HighlightedArticles.fromEvents(
                RelayQuery.cachedRecentHighlights(HIGHLIGHT_LIMIT, pubkey),
                ARTICLE_LIMIT,
            )
        }
        val friends = cachedSocial(friendKeys)
        val foafKeys = graph.foaf
        val foaf = cachedSocial(foafFetchAuthors(foafKeys))
        val others = cachedSocial(emptyList()) { event ->
            isNetworkHighlight(event.pubkey, pubkey, friendKeys, foafKeys)
        }
        val continueReading = ContinueReading.articles(ARTICLE_LIMIT)
        val mostHighlighted = mostHighlightedRows()
        val hasMostPool = hasMostPool()
        val archivedKeys = if (pubkey == null) {
            emptySet()
        } else {
            ArchivedArticles.keys(RelayQuery.cachedArchiveReactions(pubkey))
        }
        val libraryRows = if (pubkey == null) {
            LibraryHighlights()
        } else {
            libraryHighlights(cachedLibraryItems(pubkey), archivedKeys, fetchUnknownWeb = false)
        }
        val shortReads = libraryRows.shortReads
        val longReads = libraryRows.longReads
        val randomArticles = libraryRows.randomArticles
        if (yours.isEmpty() && friends.isEmpty() && foaf.isEmpty() && others.isEmpty() &&
            continueReading.isEmpty() && mostHighlighted.isEmpty() && !hasMostPool &&
            shortReads.isEmpty() && longReads.isEmpty() && randomArticles.isEmpty()
        ) {
            return null
        }
        val previews = (
            yours + friends.highlights + friends.likes + friends.reads +
                foaf.highlights + foaf.likes + foaf.reads +
                others.highlights + others.likes + others.reads +
                continueReading + mostHighlighted +
                shortReads + longReads + randomArticles
            )
            .map { it.url }
            .distinct()
            .associateWith { ArticlePreview.get(it) }
        return HomeHighlightsState.Ready(
            applyPreviews(yours, previews),
            applyPreviews(friends.highlights, previews),
            applyPreviews(friends.likes, previews),
            applyPreviews(friends.reads, previews),
            applyPreviews(foaf.highlights, previews),
            applyPreviews(foaf.likes, previews),
            applyPreviews(foaf.reads, previews),
            applyPreviews(others.highlights, previews),
            applyPreviews(others.likes, previews),
            applyPreviews(others.reads, previews),
            loggedIn = pubkey != null,
            archivedKeys = archivedKeys,
            continueReading = applyPreviews(continueReading, previews),
            mostHighlighted = applyPreviews(mostHighlighted, previews),
            hasMostPool = hasMostPool,
            shortReads = applyPreviews(shortReads, previews),
            longReads = applyPreviews(longReads, previews),
            randomArticles = applyPreviews(randomArticles, previews),
        )
    }

    private fun currentWindow(): MostHighlightedWindow =
        SettingsSync.settings.value.mostHighlightedWindow

    private fun mostHighlightedRows(
        window: MostHighlightedWindow = currentWindow(),
    ): List<HighlightedArticle> =
        HighlightedArticles.mostHighlighted(
            EventCache.byKind(Nip01Event.KIND_HIGHLIGHT),
            ExploreRows.CANDIDATES,
            since = window.since(),
        )

    private fun hasMostPool(): Boolean {
        val since = MostHighlightedWindow.Month.since()
        return EventCache.byKind(Nip01Event.KIND_HIGHLIGHT).any { event ->
            event.content.isNotBlank() && event.createdAt >= since
        }
    }

    private fun fetchMostHighlighted() {
        val window = currentWindow()
        RelayQuery.fetchRecentHighlights(
            RelayQuery.globalReadRelays(),
            limit = window.fetchLimit,
            since = window.since(),
        )
    }

    private fun loadYours(relays: List<String>, pubkeyHex: String): List<HighlightedArticle> {
        return HighlightedArticles.fromEvents(
            RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT, pubkeyHex),
            ARTICLE_LIMIT,
        )
    }

    private fun loadFriends(relays: List<String>, friendPubkeys: Set<String>): SocialRows {
        if (friendPubkeys.isEmpty()) return SocialRows()
        return fetchSocial(relays, friendPubkeys, routed = true)
    }

    private fun loadFoaf(relays: List<String>, foafPubkeys: Set<String>): SocialRows {
        val authors = foafFetchAuthors(foafPubkeys)
        if (authors.isEmpty()) return SocialRows()
        return fetchSocial(relays, authors, routed = true)
    }

    private fun loadOthers(
        excludeHex: String?,
        friendPubkeys: Set<String>,
        foafPubkeys: Set<String>,
    ): SocialRows {
        val highlights = RelayQuery.fetchRecentHighlights(RelayQuery.globalReadRelays(), HIGHLIGHT_LIMIT)
            .filter { event -> isNetworkHighlight(event.pubkey, excludeHex, friendPubkeys, foafPubkeys) }
        val reactions = RelayQuery.fetchRecentReactions(RelayQuery.globalReadRelays(), REACTION_LIMIT)
            .filter { event -> isNetworkHighlight(event.pubkey, excludeHex, friendPubkeys, foafPubkeys) }
        return socialRows(highlights, reactions)
    }

    private fun cachedSocial(
        authors: Collection<String>,
        accept: ((Nip01Event) -> Boolean)? = null,
    ): SocialRows {
        if (authors.isEmpty() && accept == null) return SocialRows()
        val keep = accept ?: { true }
        val highlights = RelayQuery.cachedRecentHighlights(
            HIGHLIGHT_LIMIT,
            authors = authors,
        ).filter(keep)
        val reactions = RelayQuery.cachedRecentReactions(
            REACTION_LIMIT,
            authors = authors,
        ).filter(keep)
        return socialRows(highlights, reactions)
    }

    private fun fetchSocial(
        relays: List<String>,
        authors: Collection<String>,
        routed: Boolean,
    ): SocialRows {
        val highlights = if (routed) {
            RelayQuery.fetchRecentHighlightsByAuthors(authors, relays, HIGHLIGHT_LIMIT)
        } else {
            RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT, authors = authors)
        }
        val reactions = if (routed) {
            RelayQuery.fetchRecentReactionsByAuthors(authors, relays, REACTION_LIMIT)
        } else {
            RelayQuery.fetchRecentReactions(relays, REACTION_LIMIT, authors = authors)
        }
        return socialRows(highlights, reactions)
    }

    private fun socialRows(
        highlights: List<Nip01Event>,
        reactions: List<Nip01Event>,
    ): SocialRows = SocialRows(
        highlights = HighlightedArticles.fromEvents(highlights, ExploreRows.CANDIDATES),
        likes = HighlightedArticles.fromReactionEvents(
            reactions.filter { ArticleReactions.isLike(it) },
            ExploreRows.CANDIDATES,
        ),
        reads = HighlightedArticles.fromReactionEvents(
            reactions.filter { Archive.isArchive(it) },
            ExploreRows.CANDIDATES,
        ),
    )

    private fun libraryHighlights(
        items: List<BookmarkItem>,
        archivedKeys: Set<String>,
        fetchUnknownWeb: Boolean,
    ): LibraryHighlights {
        val minutes = ReadingTimes.minutes(items, archivedKeys, fetchUnknownWeb)
        return LibraryHighlights(
            shortReads = TimedReads.articles(
                items,
                archivedKeys,
                TimedReadKind.Short,
                minutes,
                ARTICLE_LIMIT,
            ),
            longReads = TimedReads.articles(
                items,
                archivedKeys,
                TimedReadKind.Long,
                minutes,
                ARTICLE_LIMIT,
            ),
            randomArticles = RandomArticles.articles(items, archivedKeys, ARTICLE_LIMIT),
        )
    }

    /** Public + web library shelves (private stays locked until Library unlocks it). */
    private fun libraryItems(pubkeyHex: String, relays: List<String>) =
        BookmarkCatalog.build(
            listEvent = RelayQuery.fetchBookmarkList(pubkeyHex, relays),
            hiddenTags = null,
            webEvents = RelayQuery.fetchWebBookmarks(pubkeyHex, relays),
            articles = cachedArticlesFor(pubkeyHex),
            notes = emptyMap(),
            previews = emptyMap(),
        ).let { it.public + it.web }

    private fun cachedLibraryItems(pubkeyHex: String) =
        BookmarkCatalog.build(
            listEvent = EventCache.latest(Nip01Event.KIND_BOOKMARKS, pubkeyHex),
            hiddenTags = null,
            webEvents = RelayQuery.cachedWebBookmarks(pubkeyHex),
            articles = cachedArticlesFor(pubkeyHex),
            notes = emptyMap(),
            previews = emptyMap(),
        ).let { it.public + it.web }

    private fun cachedArticlesFor(pubkeyHex: String): Map<String, Nip01Event> {
        val list = EventCache.latest(Nip01Event.KIND_BOOKMARKS, pubkeyHex) ?: return emptyMap()
        return Nip51.publicRefs(list)
            .filter { it.kind == BookmarkRefKind.Article }
            .distinctBy { it.value }
            .mapNotNull { ref ->
                val article = NostrArticle.fromCoordinate(ref.value) ?: return@mapNotNull null
                EventCache.latest(article.pointer.kind, article.pointer.pubkey, article.pointer.identifier)
                    ?.let { ref.value to it }
            }
            .toMap()
    }

    private suspend fun loadPreviews(urls: List<String>): Map<String, OgPreview?> = coroutineScope {
        val gate = Semaphore(PREVIEW_FETCH_PARALLELISM)
        urls.map { url ->
            async {
                val cached = ArticlePreview.get(url)
                if (NostrLink.parse(url) != null) return@async url to cached
                if (cached?.title != null && cached.imageUrl != null) return@async url to cached
                if (OgPreviewCache.recentlyAttempted(url)) return@async url to cached
                val fetched = gate.withPermit {
                    OgPreviewCache.markAttempted(url)
                    runCatching { OgMetaClient.fetch(url) }.getOrNull()
                }
                url to mergePreview(cached, fetched)
            }
        }.awaitAll().toMap()
    }

    private fun applyPreviews(
        items: List<HighlightedArticle>,
        previews: Map<String, OgPreview?>,
    ): List<HighlightedArticle> = items.map { article ->
        HighlightedArticles.decorate(article, previews[article.url] ?: ArticlePreview.get(article.url))
    }

    private data class SocialRows(
        val highlights: List<HighlightedArticle> = emptyList(),
        val likes: List<HighlightedArticle> = emptyList(),
        val reads: List<HighlightedArticle> = emptyList(),
    ) {
        fun isEmpty(): Boolean = highlights.isEmpty() && likes.isEmpty() && reads.isEmpty()
    }

    private data class LibraryHighlights(
        val shortReads: List<HighlightedArticle> = emptyList(),
        val longReads: List<HighlightedArticle> = emptyList(),
        val randomArticles: List<HighlightedArticle> = emptyList(),
    )

    private data class LoadedRows(
        val yours: List<HighlightedArticle>,
        val friends: List<HighlightedArticle>,
        val likedFriends: List<HighlightedArticle>,
        val readFriends: List<HighlightedArticle>,
        val foaf: List<HighlightedArticle>,
        val likedFoaf: List<HighlightedArticle>,
        val readFoaf: List<HighlightedArticle>,
        val others: List<HighlightedArticle>,
        val likedOthers: List<HighlightedArticle>,
        val readOthers: List<HighlightedArticle>,
        val archivedKeys: Set<String>,
        val continueReading: List<HighlightedArticle>,
        val mostHighlighted: List<HighlightedArticle>,
        val hasMostPool: Boolean,
        val shortReads: List<HighlightedArticle>,
        val longReads: List<HighlightedArticle>,
        val randomArticles: List<HighlightedArticle>,
    ) {
        fun isEmpty(): Boolean =
            yours.isEmpty() && friends.isEmpty() && likedFriends.isEmpty() && readFriends.isEmpty() &&
                foaf.isEmpty() && likedFoaf.isEmpty() && readFoaf.isEmpty() &&
                others.isEmpty() && likedOthers.isEmpty() && readOthers.isEmpty() &&
                continueReading.isEmpty() && mostHighlighted.isEmpty() && !hasMostPool &&
                shortReads.isEmpty() && longReads.isEmpty() && randomArticles.isEmpty()

        fun urls(): List<String> =
            (
                yours + friends + likedFriends + readFriends +
                    foaf + likedFoaf + readFoaf +
                    others + likedOthers + readOthers +
                    continueReading + mostHighlighted +
                    shortReads + longReads + randomArticles
                )
                .map { it.url }
                .distinct()
    }

    private fun LoadedRows.toReady(
        pubkey: String?,
        previews: Map<String, OgPreview?>,
    ): HomeHighlightsState.Ready = HomeHighlightsState.Ready(
        applyPreviews(yours, previews),
        applyPreviews(friends, previews),
        applyPreviews(likedFriends, previews),
        applyPreviews(readFriends, previews),
        applyPreviews(foaf, previews),
        applyPreviews(likedFoaf, previews),
        applyPreviews(readFoaf, previews),
        applyPreviews(others, previews),
        applyPreviews(likedOthers, previews),
        applyPreviews(readOthers, previews),
        loggedIn = pubkey != null,
        archivedKeys = archivedKeys,
        continueReading = applyPreviews(continueReading, previews),
        mostHighlighted = applyPreviews(mostHighlighted, previews),
        hasMostPool = hasMostPool,
        shortReads = applyPreviews(shortReads, previews),
        longReads = applyPreviews(longReads, previews),
        randomArticles = applyPreviews(randomArticles, previews),
    )

    companion object {
        // Raw highlight pool per row; many highlights share an article, so
        // this needs headroom above ARTICLE_LIMIT to fill a row.
        private const val HIGHLIGHT_LIMIT = 160
        private const val REACTION_LIMIT = 400
        private const val ARTICLE_LIMIT = 21
        internal const val REFRESH_INTERVAL_MS = 5 * 60_000L
        private const val PREVIEW_FETCH_PARALLELISM = 6
    }
}

/** Whether a resume should hit relays again or just re-read local state. */
internal fun shouldReload(
    loadedAt: Long?,
    now: Long,
    inFlight: Boolean,
    ready: Boolean,
    force: Boolean,
): Boolean {
    if (force) return true
    if (inFlight) return false
    if (!ready || loadedAt == null) return true
    return now - loadedAt >= HomeViewModel.REFRESH_INTERVAL_MS
}

internal fun mergePreview(cached: OgPreview?, fetched: OgPreview?): OgPreview? {
    if (fetched == null) return cached
    if (cached == null) return fetched
    return OgPreview(
        title = fetched.title ?: cached.title,
        imageUrl = fetched.imageUrl ?: cached.imageUrl,
        siteName = fetched.siteName ?: cached.siteName,
        description = fetched.description ?: cached.description,
    )
}

internal fun isNetworkHighlight(
    authorHex: String,
    sessionHex: String?,
    friendPubkeys: Set<String>,
    foafPubkeys: Set<String> = emptySet(),
): Boolean {
    val author = authorHex.lowercase()
    if (sessionHex != null && author == sessionHex.lowercase()) return false
    return author !in friendPubkeys && author !in foafPubkeys
}
