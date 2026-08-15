package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Nip66Test {
    @Test
    fun selectKeepsSeedWhenDiscoveryIsEmpty() {
        val selected = Nip66.select(emptyList(), listOf("wss://relay.damus.io", "wss://nos.lol/"))
        assertEquals(listOf("wss://relay.damus.io", "wss://nos.lol"), selected)
    }

    @Test
    fun selectSkipsOnionAuthPaymentAndTor() {
        val selected = Nip66.select(
            listOf(
                discovery("wss://ok.example", rtt = 40),
                discovery("wss://secret.onion", rtt = 1),
                discovery("wss://login.example", rtt = 2, requirements = listOf("auth", "!payment")),
                discovery("wss://paid.example", rtt = 3, requirements = listOf("payment")),
                discovery("wss://tor.example", rtt = 4, network = "tor"),
                discovery("ws://insecure.example", rtt = 5),
            ),
            seed = listOf("wss://relay.damus.io"),
            limit = 8,
        )
        assertTrue(selected.contains("wss://relay.damus.io"))
        assertTrue(selected.contains("wss://ok.example"))
        assertFalse(selected.contains("wss://secret.onion"))
        assertFalse(selected.contains("wss://login.example"))
        assertFalse(selected.contains("wss://paid.example"))
        assertFalse(selected.contains("wss://tor.example"))
        assertFalse(selected.any { it.startsWith("ws://") })
    }

    @Test
    fun selectPrefersLowerRttAndCapsSize() {
        val selected = Nip66.select(
            listOf(
                discovery("wss://slow.example", rtt = 900),
                discovery("wss://fast.example", rtt = 20),
                discovery("wss://mid.example", rtt = 80),
            ),
            seed = emptyList(),
            limit = 2,
        )
        assertEquals(listOf("wss://fast.example", "wss://mid.example"), selected)
    }

    @Test
    fun normalizeStripsSlashAndRejectsLocalhost() {
        assertEquals("wss://relay.damus.io", Nip66.normalize("wss://relay.damus.io/"))
        assertNull(Nip66.normalize("wss://127.0.0.1"))
        assertNull(Nip66.normalize("wss://localhost"))
    }

    @Test
    fun clearnetMissingNetworkTagIsAccepted() {
        val event = discovery("wss://plain.example", rtt = 10, network = null)
        assertTrue(Nip66.isClearnet(event))
        assertFalse(Nip66.requiresBarrier(event))
    }

    private fun discovery(
        url: String,
        rtt: Int,
        network: String? = "clearnet",
        requirements: List<String> = listOf("!auth", "!payment"),
    ): Nip01Event {
        val tags = mutableListOf(
            listOf("d", url),
            listOf("rtt-open", rtt.toString()),
        )
        if (network != null) tags.add(listOf("n", network))
        requirements.forEach { tags.add(listOf("R", it)) }
        return Nip01Event(
            id = url.hashCode().toUInt().toString().padStart(64, '0'),
            pubkey = "aa".repeat(32),
            createdAt = 1,
            kind = Nip01Event.KIND_RELAY_DISCOVERY,
            tags = tags,
            content = "",
            sig = "bb".repeat(32),
        )
    }
}
