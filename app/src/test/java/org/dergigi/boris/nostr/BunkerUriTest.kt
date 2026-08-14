package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BunkerUriTest {
    private val remote = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"

    @Test
    fun parseAcceptsTwoRelaysAndSecret() {
        val uri = BunkerUri.parse(
            "bunker://$remote?relay=wss://relay.one&relay=wss://relay.two&secret=abc123",
        )
        assertEquals(remote, uri?.remoteSignerPubkey)
        assertEquals(listOf("wss://relay.one", "wss://relay.two"), uri?.relays)
        assertEquals("abc123", uri?.secret)
    }

    @Test
    fun parseRejectsSecretKeyBech32Prefix() {
        val nsec = "nsec1vl029mgpspedwwrv24u3lxfx3s3qqc3e34xq9y"
        assertNull(BunkerUri.parse("bunker://$remote?relay=wss://relay.one&secret=$nsec"))
        assertNull(BunkerUri.parse("$nsec bunker://$remote?relay=wss://relay.one"))
    }

    @Test
    fun parseRejectsMissingRelay() {
        assertNull(BunkerUri.parse("bunker://$remote"))
        assertNull(BunkerUri.parse("bunker://$remote?secret=abc"))
    }

    @Test
    fun parseRejectsNonWssRelay() {
        assertNull(BunkerUri.parse("bunker://$remote?relay=ws://relay.one"))
        assertNull(BunkerUri.parse("bunker://$remote?relay=https://relay.one"))
    }

    @Test
    fun parseRejectsShortHost() {
        assertNull(BunkerUri.parse("bunker://abcd?relay=wss://relay.one"))
    }

    @Test
    fun parseDecodesPercentEncodedWssRelay() {
        val uri = BunkerUri.parse(
            "bunker://$remote?relay=wss%3A%2F%2Frelay.encoded.example",
        )
        assertEquals(listOf("wss://relay.encoded.example"), uri?.relays)
    }
}
