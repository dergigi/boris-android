package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NwcUriTest {
    private val wallet = "ab".repeat(32)
    private val secret = "cd".repeat(32)

    @Test
    fun parsesWalletRelaysSecretAndLud16() {
        val uri = NwcUri.parse(
            "nostr+walletconnect://$wallet?relay=wss%3A%2F%2Frelay.getalby.com%2Fv1" +
                "&relay=wss://relay.damus.io&secret=$secret&lud16=gigi%40getalby.com",
        )
        requireNotNull(uri)
        assertEquals(wallet, uri.walletPubkey)
        assertEquals(listOf("wss://relay.getalby.com/v1", "wss://relay.damus.io"), uri.relays)
        assertEquals(secret, uri.secretHex)
        assertEquals("gigi@getalby.com", uri.lud16)
        assertEquals(32, uri.secretBytes().size)
    }

    @Test
    fun acceptsSchemeVariantsAndUppercaseHex() {
        val upper = wallet.uppercase()
        assertEquals(wallet, NwcUri.parse("nostr+walletconnect:$upper?relay=wss://r.io&secret=$secret")?.walletPubkey)
        assertEquals(wallet, NwcUri.parse("nostrwalletconnect://$wallet?relay=wss://r.io&secret=$secret")?.walletPubkey)
    }

    @Test
    fun rejectsMissingSecretRelayOrBadPubkey() {
        assertNull(NwcUri.parse("nostr+walletconnect://$wallet?relay=wss://r.io"))
        assertNull(NwcUri.parse("nostr+walletconnect://$wallet?secret=$secret"))
        assertNull(NwcUri.parse("nostr+walletconnect://$wallet?relay=http://r.io&secret=$secret"))
        assertNull(NwcUri.parse("nostr+walletconnect://abc?relay=wss://r.io&secret=$secret"))
        assertNull(NwcUri.parse("nostr+walletconnect://$wallet?relay=wss://r.io&secret=nsec1abc"))
        assertNull(NwcUri.parse("bunker://$wallet?relay=wss://r.io&secret=$secret"))
    }
}
