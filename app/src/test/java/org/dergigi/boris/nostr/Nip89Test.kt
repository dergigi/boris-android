package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Nip89Test {
    @Test
    fun handlerPubkeyMatchesGigiNpub() {
        assertEquals(
            Nip89.HANDLER_PUBKEY,
            Nip19.npubDecode("npub1dergggklka99wwrs92yz8wdjs952h2ux2ha2ed598ngwu9w7a6fsh9xzpc"),
        )
    }

    @Test
    fun recommendationAddsBorisAndKeepsOtherApps() {
        val existing = listOf(
            listOf("d", "30023"),
            listOf("a", "31990:abc:other", "wss://relay.example.com", "web"),
        )
        val tags = Nip89.recommendationTags(existing)
        assertEquals("30023", tags.first { it[0] == "d" }[1])
        assertEquals(1, tags.count { it[0] == "d" })
        assertTrue(tags.any { it[0] == "a" && it[1] == "31990:abc:other" })
        assertTrue(tags.any { it[0] == "a" && it[1] == Nip89.handlerAddress && it.getOrNull(3) == "android" })
    }

    @Test
    fun alreadyRecommendsIgnoresOtherHandlers() {
        val other = Nip01Event(
            id = "aa".repeat(32),
            pubkey = "bb".repeat(32),
            createdAt = 1,
            kind = Nip89.KIND,
            tags = listOf(listOf("d", "30023"), listOf("a", "31990:abc:other")),
            content = "",
            sig = "cc".repeat(32),
        )
        assertFalse(Nip89.alreadyRecommends(other))
        val mine = other.copy(
            tags = listOf(listOf("d", "30023"), listOf("a", Nip89.handlerAddress, Nip89.HANDLER_RELAY, "web")),
        )
        assertTrue(Nip89.alreadyRecommends(mine))
    }
}
