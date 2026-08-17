package org.dergigi.boris.ui.home

import android.app.Application
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
import org.dergigi.boris.data.ArchivedArticles
import org.dergigi.boris.data.ArticlePreview
import org.dergigi.boris.data.BookmarkCatalog
import org.dergigi.boris.data.ContinueReading
import org.dergigi.boris.data.HighlightedArticle
import org.dergigi.boris.data.HighlightedArticles
import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.OgMetaClient
import org.dergigi.boris.data.OgPreview
import org.dergigi.boris.data.RandomArticles
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.BookmarkRefKind
import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery

sealed interface HomeHighlightsState {
    data object Loading : HomeHighlightsState
    data object Empty : HomeHighlightsState
    data object Error : HomeHighlightsState
    data class Ready(
        val yours: List<HighlightedArticle>,
        val friends: List<HighlightedArticle>,
        val others: List<HighlightedArticle>,
        val loggedIn: Boolean,
        val archivedKeys: Set<String> = emptySet(),
        val continueReading: List<HighlightedArticle> = emptyList(),
        val mostHighlighted: List<HighlightedArticle> = emptyList(),
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

    private var loadJob: Job? = null

    fun refresh() {
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
                        val friendKeysDeferred = async {
                            if (pubkey == null) {
                                emptySet()
                            } else {
                                RelayQuery.fetchContactPubkeys(pubkey) - pubkey.lowercase()
                            }
                        }
                        val relaysDeferred = async {
                            buildList {
                                addAll(RelayList.FALLBACK)
                                if (pubkey != null) addAll(RelayQuery.fetchRelayList(pubkey).read)
                            }.distinct()
                        }
                        val friendKeys = friendKeysDeferred.await()
                        val relays = relaysDeferred.await()
                        val yoursDeferred = async {
                            if (pubkey == null) emptyList() else loadYours(relays, pubkey)
                        }
                        val friendsDeferred = async { loadFriends(relays, friendKeys) }
                        val othersDeferred = async { loadOthers(pubkey, friendKeys) }
                        val archiveDeferred = async {
                            if (pubkey == null) {
                                emptySet()
                            } else {
                                ArchivedArticles.keys(RelayQuery.fetchArchiveReactions(pubkey, relays))
                            }
                        }
                        val rawYours = HighlightedArticles.hydrate(yoursDeferred.await())
                        val rawFriends = HighlightedArticles.hydrate(friendsDeferred.await())
                        val rawOthers = HighlightedArticles.hydrate(othersDeferred.await())
                        val rawContinue = HighlightedArticles.hydrate(ContinueReading.articles(ARTICLE_LIMIT))
                        val rawMost = HighlightedArticles.hydrate(
                            HighlightedArticles.mostHighlighted(
                                EventCache.byKind(Nip01Event.KIND_HIGHLIGHT),
                                ARTICLE_LIMIT,
                            ),
                        )
                        val archivedKeys = archiveDeferred.await()
                        val rawRandom = if (pubkey == null) {
                            emptyList()
                        } else {
                            HighlightedArticles.hydrate(
                                RandomArticles.articles(
                                    libraryItems(pubkey, relays),
                                    archivedKeys,
                                    ARTICLE_LIMIT,
                                ),
                            )
                        }
                        LoadedRows(
                            rawYours,
                            rawFriends,
                            rawOthers,
                            archivedKeys,
                            rawContinue,
                            rawMost,
                            rawRandom,
                        )
                    }
                }
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

    private fun loadCached(pubkey: String?): HomeHighlightsState.Ready? {
        val friendKeys = if (pubkey == null) {
            emptySet()
        } else {
            RelayQuery.cachedContactPubkeys(pubkey) - pubkey.lowercase()
        }
        val yours = if (pubkey == null) {
            emptyList()
        } else {
            HighlightedArticles.fromEvents(
                RelayQuery.cachedRecentHighlights(HIGHLIGHT_LIMIT, pubkey),
                ARTICLE_LIMIT,
            )
        }
        val friends = if (friendKeys.isEmpty()) {
            emptyList()
        } else {
            HighlightedArticles.fromEvents(
                RelayQuery.cachedRecentHighlights(HIGHLIGHT_LIMIT, authors = friendKeys),
                ARTICLE_LIMIT,
            )
        }
        val others = HighlightedArticles.fromEvents(
            RelayQuery.cachedRecentHighlights(HIGHLIGHT_LIMIT)
                .filter { event -> isNetworkHighlight(event.pubkey, pubkey, friendKeys) },
            ARTICLE_LIMIT,
        )
        val continueReading = ContinueReading.articles(ARTICLE_LIMIT)
        val mostHighlighted = HighlightedArticles.mostHighlighted(
            EventCache.byKind(Nip01Event.KIND_HIGHLIGHT),
            ARTICLE_LIMIT,
        )
        val archivedKeys = if (pubkey == null) {
            emptySet()
        } else {
            ArchivedArticles.keys(RelayQuery.cachedArchiveReactions(pubkey))
        }
        val randomArticles = if (pubkey == null) {
            emptyList()
        } else {
            RandomArticles.articles(
                cachedLibraryItems(pubkey),
                archivedKeys,
                ARTICLE_LIMIT,
            )
        }
        if (yours.isEmpty() && friends.isEmpty() && others.isEmpty() &&
            continueReading.isEmpty() && mostHighlighted.isEmpty() && randomArticles.isEmpty()
        ) {
            return null
        }
        val previews = (yours + friends + others + continueReading + mostHighlighted + randomArticles)
            .map { it.url }
            .distinct()
            .associateWith { ArticlePreview.get(it) }
        return HomeHighlightsState.Ready(
            applyPreviews(yours, previews),
            applyPreviews(friends, previews),
            applyPreviews(others, previews),
            loggedIn = pubkey != null,
            archivedKeys = archivedKeys,
            continueReading = applyPreviews(continueReading, previews),
            mostHighlighted = applyPreviews(mostHighlighted, previews),
            randomArticles = applyPreviews(randomArticles, previews),
        )
    }

    private fun loadYours(relays: List<String>, pubkeyHex: String): List<HighlightedArticle> {
        return HighlightedArticles.fromEvents(
            RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT, pubkeyHex),
            ARTICLE_LIMIT,
        )
    }

    private fun loadFriends(relays: List<String>, friendPubkeys: Set<String>): List<HighlightedArticle> {
        if (friendPubkeys.isEmpty()) return emptyList()
        return HighlightedArticles.fromEvents(
            RelayQuery.fetchRecentHighlightsByAuthors(friendPubkeys, relays, HIGHLIGHT_LIMIT),
            ARTICLE_LIMIT,
        )
    }

    private fun loadOthers(excludeHex: String?, friendPubkeys: Set<String>): List<HighlightedArticle> {
        val events = RelayQuery.fetchRecentHighlights(RelayQuery.globalReadRelays(), HIGHLIGHT_LIMIT)
            .filter { event -> isNetworkHighlight(event.pubkey, excludeHex, friendPubkeys) }
        return HighlightedArticles.fromEvents(events, ARTICLE_LIMIT)
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
        urls.map { url ->
            async {
                val cached = ArticlePreview.get(url)
                if (NostrLink.parse(url) != null) return@async url to cached
                if (cached?.title != null && cached.imageUrl != null) return@async url to cached
                val fetched = runCatching { OgMetaClient.fetch(url) }.getOrNull()
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

    private data class LoadedRows(
        val yours: List<HighlightedArticle>,
        val friends: List<HighlightedArticle>,
        val others: List<HighlightedArticle>,
        val archivedKeys: Set<String>,
        val continueReading: List<HighlightedArticle>,
        val mostHighlighted: List<HighlightedArticle>,
        val randomArticles: List<HighlightedArticle>,
    ) {
        fun isEmpty(): Boolean =
            yours.isEmpty() && friends.isEmpty() && others.isEmpty() &&
                continueReading.isEmpty() && mostHighlighted.isEmpty() && randomArticles.isEmpty()

        fun urls(): List<String> =
            (yours + friends + others + continueReading + mostHighlighted + randomArticles)
                .map { it.url }
                .distinct()
    }

    private fun LoadedRows.toReady(
        pubkey: String?,
        previews: Map<String, OgPreview?>,
    ): HomeHighlightsState.Ready = HomeHighlightsState.Ready(
        applyPreviews(yours, previews),
        applyPreviews(friends, previews),
        applyPreviews(others, previews),
        loggedIn = pubkey != null,
        archivedKeys = archivedKeys,
        continueReading = applyPreviews(continueReading, previews),
        mostHighlighted = applyPreviews(mostHighlighted, previews),
        randomArticles = applyPreviews(randomArticles, previews),
    )

    companion object {
        private const val HIGHLIGHT_LIMIT = 80
        private const val ARTICLE_LIMIT = 12
    }
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
): Boolean {
    val author = authorHex.lowercase()
    if (sessionHex != null && author == sessionHex.lowercase()) return false
    return author !in friendPubkeys
}
