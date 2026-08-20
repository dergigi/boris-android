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
    private const val ZAPSTORE_BORIS_APP_ADDRESS =
        "32267:6e468422dfb74a5738702a8823b9b28168abab8655faacb6853cd0ee15deee93:org.dergigi.boris"

    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<SupportUiState>(SupportUiState.Loading)
    val state: StateFlow<SupportUiState> = _state.asStateFlow()

    fun ensureLoaded() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            val directReceipts = runCatching {
                RelayQuery.fetchZapReceipts(ZapSplits.BORIS_PUBKEY)
            }.getOrDefault(emptyList())
            val zapstoreReceipts = runCatching {
                RelayQuery.fetchZapReceiptsByAddress(ZAPSTORE_BORIS_APP_ADDRESS)
            }.getOrDefault(emptyList())
            val receipts = directReceipts + zapstoreReceipts
            val supporters = ZapReceipts.supporters(receipts)
            val avatarSupporters = ZapReceipts.allSupporters(receipts)
            val profilePubkeys = (supporters + avatarSupporters).map { it.pubkey }.distinct()
            val profiles = if (profilePubkeys.isEmpty()) {
                emptyMap()
            } else {
                runCatching {
                    RelayQuery.fetchProfiles(
                        RelayQuery.globalReadRelays() + RelayQuery.zapRelays(),
                        profilePubkeys,
                    )
                }.getOrDefault(emptyMap())
            }
            _state.value = SupportUiState.Ready(supporters, profiles, avatarSupporters)
        }
    }
}
