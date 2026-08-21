package org.dergigi.boris.ui.support

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.ZapSupporter

/** Loading state for the support screen and support-heart avatar data. */
sealed interface SupportUiState {
    data object Loading : SupportUiState

    /** Loaded supporters, matching profiles, and the full avatar attribution set. */
    data class Ready(
        val supporters: List<ZapSupporter>,
        val profiles: Map<String, Profile>,
        val avatarSupporters: List<ZapSupporter> = supporters,
    ) : SupportUiState {
        val totalZaps: Int get() = avatarSupporters.sumOf { it.zapCount }
    }
}

/** Exposes the shared support state to Compose screens. */
class SupportViewModel : ViewModel() {
    val state: StateFlow<SupportUiState> = SupportStore.state

    init {
        SupportStore.ensureLoaded()
    }
}
