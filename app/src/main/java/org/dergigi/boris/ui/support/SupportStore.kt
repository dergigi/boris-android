package org.dergigi.boris.ui.support

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.ZapReceipts
import org.dergigi.boris.nostr.ZapSplits
import java.util.concurrent.atomic.AtomicBoolean

/** Shared loader for support data used by both the support screen and heart avatar. */
object SupportStore {
    private const val ZAPSTORE_BORIS_APP_ADDRESS =
        "32267:6e468422dfb74a5738702a8823b9b28168abab8655faacb6853cd0ee15deee93:org.dergigi.boris"

    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<SupportUiState>(SupportUiState.Loading)

    /** Current support data, loaded at most once per app process. */
    val state: StateFlow<SupportUiState> = _state.asStateFlow()

    /** Starts the one-shot zap receipt and profile load for the shared support UI state. */
    fun ensureLoaded() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            val (directReceipts, zapstoreReceipts) = coroutineScope {
                val direct = async {
                    runCatching {
                        RelayQuery.fetchZapReceipts(ZapSplits.BORIS_PUBKEY)
                    }.getOrDefault(emptyList())
                }
                val zapstore = async {
                    runCatching {
                        RelayQuery.fetchZapReceiptsByAddress(ZAPSTORE_BORIS_APP_ADDRESS)
                    }.getOrDefault(emptyList())
                }
                direct.await() to zapstore.await()
            }
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
