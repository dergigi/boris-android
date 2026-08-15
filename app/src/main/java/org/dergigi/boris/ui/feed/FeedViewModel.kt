package org.dergigi.boris.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.data.ArticleUrl
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery

data class FeedItem(
    val id: String,
    val quote: String,
    val url: String?,
    val host: String?,
    val authorHex: String,
    val authorName: String,
    val authorPicture: String?,
)

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Ready(val items: List<FeedItem>) : FeedUiState
    data object Empty : FeedUiState
    data object Error : FeedUiState
}

class FeedViewModel : ViewModel() {
    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        val keepItems = _state.value is FeedUiState.Ready
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (keepItems) {
                _refreshing.value = true
            } else {
                _state.value = FeedUiState.Loading
            }
            try {
                val seed = withContext(Dispatchers.IO) {
                    RelayQuery.fetchRecentHighlights(RelayList.FALLBACK, HIGHLIGHT_LIMIT)
                }
                if (!keepItems && seed.isNotEmpty()) {
                    _state.value = FeedUiState.Ready(toItems(seed, emptyMap()))
                }
                val items = withContext(Dispatchers.IO) {
                    val relays = RelayQuery.discoverContentRelays()
                    val more = RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT)
                    val merged = (seed + more)
                        .distinctBy { it.id }
                        .sortedByDescending { it.createdAt }
                        .take(HIGHLIGHT_LIMIT)
                    val authors = merged.map { it.pubkey }.distinct().take(PROFILE_LIMIT)
                    val profiles = RelayQuery.fetchProfiles(relays, authors)
                    toItems(merged, profiles)
                }
                _state.value = if (items.isEmpty()) FeedUiState.Empty else FeedUiState.Ready(items)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (_state.value !is FeedUiState.Ready) {
                    _state.value = FeedUiState.Error
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    private fun toItems(
        events: List<Nip01Event>,
        profiles: Map<String, Profile>,
    ): List<FeedItem> {
        return events.map { event ->
            val url = Nip84.articleUrl(event)
            val profile = profiles[event.pubkey.lowercase()]
            FeedItem(
                id = event.id,
                quote = event.content.trim(),
                url = url,
                host = url?.let { ArticleUrl.host(it) },
                authorHex = event.pubkey,
                authorName = displayName(event.pubkey, profile),
                authorPicture = profile?.picture,
            )
        }
    }

    private fun displayName(pubkeyHex: String, profile: Profile?): String {
        profile?.name?.takeIf { it.isNotBlank() }?.let { return it }
        return try {
            val npub = Nip19.npubEncode(pubkeyHex)
            if (npub.length > 16) npub.take(12) + "…" else npub
        } catch (_: Exception) {
            pubkeyHex.take(8)
        }
    }

    companion object {
        private const val HIGHLIGHT_LIMIT = 80
        private const val PROFILE_LIMIT = 40
    }
}
