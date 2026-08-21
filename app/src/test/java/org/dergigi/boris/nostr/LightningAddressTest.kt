package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LightningAddressTest {
    @Test
    fun parseAcceptsUserAtDomain() {
        assertEquals("boris@dergigi.com", LightningAddress.parse("boris@dergigi.com"))
        assertEquals("boris@dergigi.com", LightningAddress.parse("  boris@dergigi.com  "))
    }

    @Test
    fun parseRejectsJunk() {
        assertNull(LightningAddress.parse(null))
        assertNull(LightningAddress.parse(""))
        assertNull(LightningAddress.parse("   "))
        assertNull(LightningAddress.parse("not-an-address"))
        assertNull(LightningAddress.parse("user@"))
        assertNull(LightningAddress.parse("@dergigi.com"))
        assertNull(LightningAddress.parse("lightning:boris@dergigi.com"))
    }

    @Test
    fun uriBuildsLightningScheme() {
        assertEquals("lightning:boris@dergigi.com", LightningAddress.uri("boris@dergigi.com"))
        assertEquals("lightning:gigi@getalby.com", LightningAddress.uri("  gigi@getalby.com "))
    }

    @Test
    fun uriRejectsJunk() {
        assertNull(LightningAddress.uri("not-an-address"))
        assertNull(LightningAddress.uri(""))
        assertNull(LightningAddress.uri("user@"))
    }
}
