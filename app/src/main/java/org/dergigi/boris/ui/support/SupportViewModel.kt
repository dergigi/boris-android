package org.dergigi.boris.ui.support

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.dergigi.boris.nostr.Profile
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
    val state: StateFlow<SupportUiState> = SupportStore.state

    init {
        SupportStore.ensureLoaded()
    }
}
