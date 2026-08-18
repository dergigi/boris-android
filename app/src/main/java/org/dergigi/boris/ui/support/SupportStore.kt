package org.dergigi.boris.ui.support

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.ZapReceipts
import org.dergigi.boris.nostr.ZapSplits
import java.util.concurrent.atomic.AtomicBoolean

object SupportStore {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<SupportUiState>(SupportUiState.Loading)
    val state: StateFlow<SupportUiState> = _state.asStateFlow()

    fun ensureLoaded() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
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
            _state.value = SupportUiState.Ready(supporters, profiles)
        }
    }
}
