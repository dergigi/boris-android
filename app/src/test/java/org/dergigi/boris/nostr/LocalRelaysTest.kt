package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRelaysTest {
    @Test
    fun citrineIsLocal() {
        assertTrue(LocalRelays.isLocal(LocalRelays.CITRINE))
        assertEquals(LocalRelays.CITRINE, LocalRelays.canonical("ws://127.0.0.1:4869/"))
        assertTrue(LocalRelays.isLocal("ws://localhost:4869"))
        assertTrue(LocalRelays.isLocal("ws://[::1]:4869"))
        assertEquals("ws://[::1]:4869", LocalRelays.canonical("ws://[::1]:4869"))
    }

    @Test
    fun publicRelaysAreNotLocal() {
        assertFalse(LocalRelays.isLocal("wss://relay.damus.io"))
        assertFalse(LocalRelays.isLocal("ws://example.com"))
        assertNull(LocalRelays.canonical("https://127.0.0.1:4869"))
    }

    @Test
    fun resolveKeepsCitrineAndPublicWss() {
        assertEquals(LocalRelays.CITRINE, LocalRelays.resolve(LocalRelays.CITRINE))
        assertEquals("wss://relay.damus.io", LocalRelays.resolve("wss://relay.damus.io"))
        assertNull(LocalRelays.resolve("https://relay.one"))
    }

    @Test
    fun withLocalPrependsCitrineOnce() {
        val base = RelayList(
            read = listOf("wss://relay.damus.io"),
            write = listOf("wss://nos.lol"),
        )
        val enriched = LocalRelays.withLocal(base, enabled = true, citrineUp = true)
        assertEquals(listOf(LocalRelays.CITRINE, "wss://relay.damus.io"), enriched.read)
        assertEquals(listOf(LocalRelays.CITRINE, "wss://nos.lol"), enriched.write)
        val again = LocalRelays.withLocal(enriched, enabled = true, citrineUp = true)
        assertEquals(enriched.read, again.read)
    }

    @Test
    fun withLocalSkipsWhenDisabledOrDown() {
        val base = RelayList.fallback()
        assertEquals(base, LocalRelays.withLocal(base, enabled = false, citrineUp = true))
        assertEquals(base, LocalRelays.withLocal(base, enabled = true, citrineUp = false))
    }

    @Test
    fun remoteOnlyDropsLocalHosts() {
        val urls = listOf(LocalRelays.CITRINE, "wss://relay.damus.io", "ws://localhost:4869")
        assertEquals(listOf("wss://relay.damus.io"), LocalRelays.remoteOnly(urls))
    }
}
