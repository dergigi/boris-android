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
import org.dergigi.boris.data.HighlightedArticle
import org.dergigi.boris.data.HighlightedArticles
import org.dergigi.boris.data.OgMetaClient
import org.dergigi.boris.data.OgPreview
import org.dergigi.boris.data.SessionStore
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
            val keep = _highlights.value is HomeHighlightsState.Ready
            if (keep) {
                _refreshing.value = true
            } else {
                _highlights.value = HomeHighlightsState.Loading
            }
            try {
                val pubkey = SessionStore.load(getApplication())?.pubkeyHex
                val (yours, friends, others) = withContext(Dispatchers.IO) {
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
                        val rawYours = yoursDeferred.await()
                        val rawFriends = friendsDeferred.await()
                        val rawOthers = othersDeferred.await()
                        val previews = loadPreviews(
                            (rawYours + rawFriends + rawOthers).map { it.url }.distinct(),
                        )
                        Triple(
                            applyPreviews(rawYours, previews),
                            applyPreviews(rawFriends, previews),
                            applyPreviews(rawOthers, previews),
                        )
                    }
                }
                _highlights.value = if (yours.isEmpty() && friends.isEmpty() && others.isEmpty()) {
                    HomeHighlightsState.Empty
                } else {
                    HomeHighlightsState.Ready(yours, friends, others, loggedIn = pubkey != null)
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

    private fun loadYours(relays: List<String>, pubkeyHex: String): List<HighlightedArticle> {
        return HighlightedArticles.fromEvents(
            RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT, pubkeyHex),
            ARTICLE_LIMIT,
        )
    }

    private fun loadFriends(relays: List<String>, friendPubkeys: Set<String>): List<HighlightedArticle> {
        if (friendPubkeys.isEmpty()) return emptyList()
        return HighlightedArticles.fromEvents(
            RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT, authors = friendPubkeys),
            ARTICLE_LIMIT,
        )
    }

    private fun loadOthers(excludeHex: String?, friendPubkeys: Set<String>): List<HighlightedArticle> {
        val events = RelayQuery.fetchRecentHighlights(RelayList.FALLBACK, HIGHLIGHT_LIMIT)
            .filter { event -> isNetworkHighlight(event.pubkey, excludeHex, friendPubkeys) }
        return HighlightedArticles.fromEvents(events, ARTICLE_LIMIT)
    }

    private suspend fun loadPreviews(urls: List<String>): Map<String, OgPreview?> = coroutineScope {
        urls.map { url ->
            async { url to runCatching { OgMetaClient.fetch(url) }.getOrNull() }
        }.awaitAll().toMap()
    }

    private fun applyPreviews(
        items: List<HighlightedArticle>,
        previews: Map<String, OgPreview?>,
    ): List<HighlightedArticle> = items.map { article ->
        val preview = previews[article.url] ?: return@map article
        article.copy(
            title = preview.title?.takeIf { it.isNotBlank() } ?: article.title,
            imageUrl = preview.imageUrl ?: article.imageUrl,
            host = preview.siteName?.takeIf { it.isNotBlank() } ?: article.host,
        )
    }

    companion object {
        private const val HIGHLIGHT_LIMIT = 80
        private const val ARTICLE_LIMIT = 12
    }
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
