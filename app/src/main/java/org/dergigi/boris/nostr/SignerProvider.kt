package org.dergigi.boris.nostr

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Null/empty provider replies need approval; remembered denial must never open the signer. */
sealed interface SignerProviderResult {
    data class Value(val event: String? = null, val result: String? = null) : SignerProviderResult
    data object Rejected : SignerProviderResult
    data object Unavailable : SignerProviderResult
    data object Invalid : SignerProviderResult
}

/** Runs blocking provider IPC off the UI thread and drops replies to cancelled/replaced requests. */
internal class BackgroundSignerRequest(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val current = AtomicReference<Any?>(null)

    fun start(
        query: () -> SignerProviderResult,
        onNeedsApproval: () -> Unit,
        onResult: (SignerProviderResult) -> Unit,
    ) {
        val token = Any()
        current.set(token)
        scope.launch {
            val response = withContext(dispatcher) { query() }
            if (!current.compareAndSet(token, null)) return@launch
            if (response == SignerProviderResult.Unavailable) onNeedsApproval() else onResult(response)
        }
    }

    fun cancel() { current.set(null) }
}
