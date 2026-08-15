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
import org.dergigi.boris.data.ArticleUrl
import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery

data class YouHighlight(
    val id: String,
    val quote: String,
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

sealed interface YouUiState {
    data object Loading : YouUiState
    data class Ready(
        val highlights: List<YouHighlight>,
        val writings: List<YouWriting>,
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

    private var loadJob: Job? = null

    fun refresh() {
        val session = SessionStore.load(getApplication())
        if (session == null) {
            loadJob?.cancel()
            _state.value = YouUiState.Ready(emptyList(), emptyList())
            _refreshing.value = false
            return
        }
        val keepItems = _state.value is YouUiState.Ready
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (keepItems) {
                _refreshing.value = true
            } else {
                _state.value = YouUiState.Loading
            }
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val relays = buildList {
                        addAll(RelayList.FALLBACK)
                        addAll(RelayQuery.fetchRelayList(session.pubkeyHex).read)
                    }.distinct()
                    coroutineScope {
                        val highlights = async {
                            RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT, session.pubkeyHex)
                                .map { event ->
                                    val url = Nip84.articleUrl(event)
                                    YouHighlight(
                                        id = event.id,
                                        quote = event.content.trim(),
                                        url = url,
                                        host = url?.let { ArticleUrl.host(it) },
                                        createdAt = event.createdAt,
                                    )
                                }
                        }
                        val writings = async {
                            RelayQuery.fetchLongFormArticles(session.pubkeyHex, relays)
                                .mapNotNull { event -> writingFrom(event) }
                        }
                        Loaded(highlights.await(), writings.await())
                    }
                }
                _state.value = YouUiState.Ready(loaded.highlights, loaded.writings)
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

    private data class Loaded(
        val highlights: List<YouHighlight>,
        val writings: List<YouWriting>,
    )

    companion object {
        private const val HIGHLIGHT_LIMIT = 80
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
