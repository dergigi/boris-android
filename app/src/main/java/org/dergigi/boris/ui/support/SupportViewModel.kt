package org.dergigi.boris.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.ZapReceipts
import org.dergigi.boris.nostr.ZapSplits
import org.dergigi.boris.nostr.ZapSupporter

sealed interface SupportUiState {
    data object Loading : SupportUiState
    data class Ready(
        val supporters: List<ZapSupporter>,
        val profiles: Map<String, Profile>,
    ) : SupportUiState {
        val totalZaps: Int get() = supporters.sumOf { it.zapCount }
    }
}

class SupportViewModel : ViewModel() {
    private val _state = MutableStateFlow<SupportUiState>(SupportUiState.Loading)
    val state: StateFlow<SupportUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val ready = withContext(Dispatchers.IO) {
                val receipts = runCatching {
                    RelayQuery.fetchZapReceipts(ZapSplits.BORIS_PUBKEY)
                }.getOrDefault(emptyList())
                val supporters = ZapReceipts.supporters(receipts)
                val profiles = if (supporters.isEmpty()) {
                    emptyMap()
                } else {
                    runCatching {
                        RelayQuery.fetchProfiles(
                            RelayQuery.globalReadRelays(),
                            supporters.map { it.pubkey },
                        )
                    }.getOrDefault(emptyMap())
                }
                SupportUiState.Ready(supporters, profiles)
            }
            _state.value = ready
        }
    }
}
