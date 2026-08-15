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
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery

sealed interface HomeHighlightsState {
    data object Loading : HomeHighlightsState
    data object Empty : HomeHighlightsState
    data object Error : HomeHighlightsState
    data class Ready(
        val yours: List<HighlightedArticle>,
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
                val (yours, others) = withContext(Dispatchers.IO) {
                    coroutineScope {
                        val yoursDeferred = async {
                            if (pubkey == null) emptyList() else loadYours(pubkey)
                        }
                        val othersDeferred = async { loadOthers(pubkey) }
                        val rawYours = yoursDeferred.await()
                        val rawOthers = othersDeferred.await()
                        val previews = loadPreviews((rawYours + rawOthers).map { it.url }.distinct())
                        applyPreviews(rawYours, previews) to applyPreviews(rawOthers, previews)
                    }
                }
                _highlights.value = if (yours.isEmpty() && others.isEmpty()) {
                    HomeHighlightsState.Empty
                } else {
                    HomeHighlightsState.Ready(yours, others, loggedIn = pubkey != null)
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

    private fun loadYours(pubkeyHex: String): List<HighlightedArticle> {
        val relays = buildList {
            addAll(RelayList.FALLBACK)
            addAll(RelayQuery.fetchRelayList(pubkeyHex).read)
        }.distinct()
        return HighlightedArticles.fromEvents(
            RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT, pubkeyHex),
            ARTICLE_LIMIT,
        )
    }

    private fun loadOthers(excludeHex: String?): List<HighlightedArticle> {
        val events = RelayQuery.fetchRecentHighlights(RelayList.FALLBACK, HIGHLIGHT_LIMIT)
            .filter { event -> excludeHex == null || !sameAuthor(event, excludeHex) }
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

    private fun sameAuthor(event: Nip01Event, pubkeyHex: String): Boolean =
        event.pubkey.equals(pubkeyHex, ignoreCase = true)

    companion object {
        private const val HIGHLIGHT_LIMIT = 80
        private const val ARTICLE_LIMIT = 12
    }
}
