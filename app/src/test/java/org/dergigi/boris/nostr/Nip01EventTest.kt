package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Test

class Nip01EventTest {
    @Test
    fun pPubkeysReadsFollowsAndIgnoresOtherTags() {
        val friend = "bb".repeat(32)
        val event = Nip01Event(
            id = "1".padStart(64, '0'),
            pubkey = "aa".repeat(32),
            createdAt = 1,
            kind = Nip01Event.KIND_CONTACTS,
            tags = listOf(
                listOf("p", friend, "wss://relay.example"),
                listOf("p", friend.uppercase()),
                listOf("p", "not-a-key"),
                listOf("d", "contacts"),
            ),
            content = "",
            sig = "cc".repeat(32),
        )
        assertEquals(setOf(friend), event.pPubkeys())
    }
}
