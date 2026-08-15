package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayListTest {
    @Test
    fun omittedMarkerIsBoth() {
        val parsed = RelayList.parse(
            listOf(
                event(
                    createdAt = 10,
                    tags = listOf(listOf("r", "wss://relay.one")),
                ),
            ),
        )
        assertEquals(listOf("wss://relay.one"), parsed.read)
        assertEquals(listOf("wss://relay.one"), parsed.write)
    }

    @Test
    fun readOnlyAndWriteOnlySplit() {
        val parsed = RelayList.parse(
            listOf(
                event(
                    createdAt = 10,
                    tags = listOf(
                        listOf("r", "wss://read.one", "read"),
                        listOf("r", "wss://write.one", "write"),
                    ),
                ),
            ),
        )
        assertEquals(listOf("wss://read.one"), parsed.read)
        assertEquals(listOf("wss://write.one"), parsed.write)
    }

    @Test
    fun localWsAcceptedAndHttpRejected() {
        val parsed = RelayList.parse(
            listOf(
                event(
                    createdAt = 10,
                    tags = listOf(
                        listOf("r", "ws://127.0.0.1:4869"),
                        listOf("r", "https://relay.one"),
                        listOf("r", "ws://example.com"),
                        listOf("r", "wss://relay.ok"),
                    ),
                ),
            ),
        )
        assertEquals(listOf("ws://127.0.0.1:4869", "wss://relay.ok"), parsed.read)
        assertEquals(listOf("ws://127.0.0.1:4869", "wss://relay.ok"), parsed.write)
        assertFalse(parsed.read.contains("ws://example.com"))
    }

    @Test
    fun emptyListUsesFallbackRelays() {
        val parsed = RelayList.parse(emptyList())
        assertEquals(RelayList.FALLBACK, parsed.read)
        assertEquals(RelayList.FALLBACK, parsed.write)
        assertTrue(parsed.read.contains("wss://relay.damus.io"))
        assertTrue(parsed.read.contains("wss://nos.lol"))
        assertTrue(parsed.read.contains("wss://relay.primal.net"))
        assertTrue(parsed.read.contains("wss://wot.dergigi.com"))
    }

    @Test
    fun newestCreatedAtWins() {
        val parsed = RelayList.parse(
            listOf(
                event(createdAt = 1, tags = listOf(listOf("r", "wss://old.one"))),
                event(createdAt = 9, tags = listOf(listOf("r", "wss://new.one"))),
            ),
        )
        assertEquals(listOf("wss://new.one"), parsed.read)
    }

    private fun event(createdAt: Long, tags: List<List<String>>): Nip01Event {
        return Nip01Event(
            id = createdAt.toString().padStart(64, '0'),
            pubkey = "aa".repeat(32),
            createdAt = createdAt,
            kind = Nip01Event.KIND_RELAY_LIST,
            tags = tags,
            content = "",
            sig = "bb".repeat(32),
        )
    }
}
