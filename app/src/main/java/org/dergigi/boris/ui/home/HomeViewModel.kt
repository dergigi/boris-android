package org.dergigi.boris.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.data.HighlightedArticle
import org.dergigi.boris.data.HighlightedArticles
import org.dergigi.boris.data.OgMetaClient
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery

sealed interface HomeHighlightsState {
    data object Loading : HomeHighlightsState
    data object Hidden : HomeHighlightsState
    data class Ready(val items: List<HighlightedArticle>) : HomeHighlightsState
}

class HomeViewModel : ViewModel() {
    private val _highlights = MutableStateFlow<HomeHighlightsState>(HomeHighlightsState.Loading)
    val highlights: StateFlow<HomeHighlightsState> = _highlights.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val keep = _highlights.value is HomeHighlightsState.Ready
            if (!keep) _highlights.value = HomeHighlightsState.Loading
            try {
                val items = withContext(Dispatchers.IO) {
                    val events = RelayQuery.fetchRecentHighlights(RelayList.FALLBACK, HIGHLIGHT_LIMIT)
                    HighlightedArticles.fromEvents(events, ARTICLE_LIMIT)
                }
                if (items.isEmpty()) {
                    if (!keep) _highlights.value = HomeHighlightsState.Hidden
                    return@launch
                }
                _highlights.value = HomeHighlightsState.Ready(items)
                val previews = items.map { article ->
                    async(Dispatchers.IO) {
                        article.url to runCatching { OgMetaClient.fetch(article.url) }.getOrNull()
                    }
                }.awaitAll()
                val byUrl = previews.toMap()
                val updated = items.map { article ->
                    val preview = byUrl[article.url] ?: return@map article
                    article.copy(
                        title = preview.title?.takeIf { it.isNotBlank() } ?: article.title,
                        imageUrl = preview.imageUrl ?: article.imageUrl,
                        host = preview.siteName?.takeIf { it.isNotBlank() } ?: article.host,
                    )
                }
                _highlights.value = HomeHighlightsState.Ready(updated)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (_highlights.value !is HomeHighlightsState.Ready) {
                    _highlights.value = HomeHighlightsState.Hidden
                }
            }
        }
    }

    companion object {
        private const val HIGHLIGHT_LIMIT = 80
        private const val ARTICLE_LIMIT = 12
    }
}
