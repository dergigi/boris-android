package org.dergigi.boris.nostr

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class SignerProviderTest {
    @Test
    fun onlyUnavailableRequestsInteractiveApproval() = runBlocking {
        for (response in listOf(
            SignerProviderResult.Value(result = "signed"),
            SignerProviderResult.Rejected,
            SignerProviderResult.Invalid,
            SignerProviderResult.Unavailable,
        )) {
            val finished = CompletableDeferred<Unit>()
            var launches = 0
            var delivered: SignerProviderResult? = null
            val callerThread = Thread.currentThread()
            BackgroundSignerRequest(this).start(
                query = {
                    assertNotSame(callerThread, Thread.currentThread())
                    response
                },
                onNeedsApproval = { launches++; finished.complete(Unit) },
                onResult = { delivered = it; finished.complete(Unit) },
            )
            withTimeout(5000) { finished.await() }
            assertEquals(if (response == SignerProviderResult.Unavailable) 1 else 0, launches)
            assertEquals(if (response == SignerProviderResult.Unavailable) null else response, delivered)
        }
    }

    @Test
    fun cancelledAndReplacedRequestsCannotOpenSigner() = runBlocking {
        val request = BackgroundSignerRequest(this)
        var staleCallbacks = 0
        request.start(
            query = { SignerProviderResult.Unavailable },
            onNeedsApproval = { staleCallbacks++ },
            onResult = { staleCallbacks++ },
        )
        request.cancel()
        request.start(
            query = { SignerProviderResult.Unavailable },
            onNeedsApproval = { staleCallbacks++ },
            onResult = { staleCallbacks++ },
        )
        val finished = CompletableDeferred<Unit>()
        request.start(
            query = { SignerProviderResult.Value(result = "ok") },
            onNeedsApproval = { fail("Approved request opened signer") },
            onResult = { finished.complete(Unit) },
        )
        withTimeout(5000) { finished.await() }
        // runBlocking waits for every child query before returning.
        coroutineContext[kotlinx.coroutines.Job]!!.children.toList().forEach { it.join() }
        assertEquals(0, staleCallbacks)
    }

    @Test
    fun cancellationWhileProviderIsRunningDiscardsReply() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = java.util.concurrent.CountDownLatch(1)
        val request = BackgroundSignerRequest(this)
        var callbacks = 0
        request.start(
            query = {
                started.complete(Unit)
                check(release.await(5, java.util.concurrent.TimeUnit.SECONDS))
                SignerProviderResult.Unavailable
            },
            onNeedsApproval = { callbacks++ },
            onResult = { callbacks++ },
        )
        try {
            withTimeout(5000) { started.await() }
            request.cancel()
        } finally {
            release.countDown()
        }
        coroutineContext[kotlinx.coroutines.Job]!!.children.toList().forEach { it.join() }
        assertEquals(0, callbacks)
    }

    @Test
    fun signedEventAndSignatureOnlyRepliesAreVerified() {
        val key = ClientKeypair.generate()
        val event = Nip01Event.sign(key.privkey, key.pubkeyHex, 1, emptyList(), "hello")
        val unsigned = PendingUnsignedEvent(event.pubkey, event.createdAt, event.kind, event.tags, event.content)
        for (response in listOf(
            SignerProviderResult.Value(event = event.toJsonString()),
            SignerProviderResult.Value(result = event.sig),
        )) {
            val outcome = EventSigner.providerSignOutcome(response, key.pubkeyHex, unsigned)
            assertEquals(event.id, (outcome as SignOutcome.Signed).event.id)
        }
        assertEquals(SignOutcome.Failed, EventSigner.providerSignOutcome(
            SignerProviderResult.Value(event = event.toJsonString()), key.pubkeyHex, unsigned.copy(content = "different"),
        ))
        assertEquals(SignOutcome.Failed, EventSigner.providerSignOutcome(
            SignerProviderResult.Value(result = "00".repeat(64)), key.pubkeyHex, unsigned,
        ))
        assertEquals(SignOutcome.Rejected, EventSigner.providerSignOutcome(
            SignerProviderResult.Rejected, key.pubkeyHex, unsigned,
        ))
    }

    @Test
    fun cryptoPreservesEmptyPlaintextAndRejections() {
        assertEquals(CryptoOutcome.Text(""), EventSigner.providerCryptoOutcome(SignerProviderResult.Value(result = "")))
        assertEquals(CryptoOutcome.Rejected, EventSigner.providerCryptoOutcome(SignerProviderResult.Rejected))
        assertEquals(CryptoOutcome.Failed, EventSigner.providerCryptoOutcome(SignerProviderResult.Invalid))
    }
}
