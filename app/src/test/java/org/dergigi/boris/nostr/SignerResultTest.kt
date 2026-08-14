package org.dergigi.boris.nostr

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignerResultTest {
    private val hex = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
    private val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"

    @Test
    fun npubResultBecomesHex() {
        val result = SignerResults.parse(
            resultCode = Activity.RESULT_OK,
            rejected = false,
            result = npub,
            signature = null,
            packageName = "com.greenart7c3.nostrsigner",
        )
        val success = result as SignerResult.Success
        assertEquals(hex, success.pubkeyHex)
        assertEquals("com.greenart7c3.nostrsigner", success.signerPackage)
    }

    @Test
    fun hexResultPassesThrough() {
        val result = SignerResults.parse(
            resultCode = Activity.RESULT_OK,
            rejected = false,
            result = hex,
            signature = null,
            packageName = "com.greenart7c3.nostrsigner",
        )
        assertEquals(hex, (result as SignerResult.Success).pubkeyHex)
    }

    @Test
    fun rejectedMapsToRejected() {
        val result = SignerResults.parse(
            resultCode = Activity.RESULT_OK,
            rejected = true,
            result = npub,
            signature = null,
            packageName = "com.greenart7c3.nostrsigner",
        )
        assertTrue(result is SignerResult.Rejected)
    }

    @Test
    fun canceledMapsToCancelled() {
        val result = SignerResults.parse(
            resultCode = Activity.RESULT_CANCELED,
            rejected = false,
            result = null,
            signature = null,
            packageName = null,
        )
        assertTrue(result is SignerResult.Cancelled)
    }

    @Test
    fun parseSignedEventAcceptsValidHighlight() {
        val key = ClientKeypair.generate()
        val event = Nip01Event.sign(
            privkey = key.privkey,
            pubkeyHex = key.pubkeyHex,
            kind = Nip01Event.KIND_HIGHLIGHT,
            tags = Nip84.tags("https://example.com/article", null),
            content = "a quote",
        )
        val result = SignerResults.parseSignedEvent(
            resultCode = Activity.RESULT_OK,
            rejected = false,
            event = event,
            sessionHex = key.pubkeyHex,
        )
        val signed = result as SignerResult.Signed
        assertEquals(event.id, signed.event.id)
        assertEquals(key.pubkeyHex, signed.event.pubkey)
    }

    @Test
    fun parseSignedEventRejectedMapsToRejected() {
        val result = SignerResults.parseSignedEvent(
            resultCode = Activity.RESULT_OK,
            rejected = true,
            eventJson = "{}",
            resultJson = null,
            sessionHex = hex,
        )
        assertTrue(result is SignerResult.Rejected)
    }

    @Test
    fun parseSignedEventCancelMapsToCancelled() {
        val result = SignerResults.parseSignedEvent(
            resultCode = Activity.RESULT_CANCELED,
            rejected = false,
            eventJson = null,
            resultJson = null,
            sessionHex = hex,
        )
        assertTrue(result is SignerResult.Cancelled)
    }

    @Test
    fun parseSignedEventWrongKindIsCancelled() {
        val key = ClientKeypair.generate()
        val event = Nip01Event.sign(
            privkey = key.privkey,
            pubkeyHex = key.pubkeyHex,
            kind = 1,
            tags = emptyList(),
            content = "note",
        )
        val result = SignerResults.parseSignedEvent(
            resultCode = Activity.RESULT_OK,
            rejected = false,
            event = event,
            sessionHex = key.pubkeyHex,
        )
        assertTrue(result is SignerResult.Cancelled)
    }

    @Test
    fun parseSignedEventFailedVerifyIsCancelled() {
        val key = ClientKeypair.generate()
        val event = Nip01Event.sign(
            privkey = key.privkey,
            pubkeyHex = key.pubkeyHex,
            kind = Nip01Event.KIND_HIGHLIGHT,
            tags = Nip84.tags("https://example.com/article", null),
            content = "a quote",
        )
        val tampered = event.copy(sig = "00".repeat(64))
        val result = SignerResults.parseSignedEvent(
            resultCode = Activity.RESULT_OK,
            rejected = false,
            event = tampered,
            sessionHex = key.pubkeyHex,
        )
        assertTrue(result is SignerResult.Cancelled)
    }
}
