package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun verifyRejectsOversizedEventsBeforeSerializing() {
        val event = Nip01Event(
            id = "1".padStart(64, '0'),
            pubkey = "aa".repeat(32),
            createdAt = 1,
            kind = Nip01Event.KIND_TEXT_NOTE,
            tags = listOf(listOf("x", "a".repeat(Nip01Event.MAX_VERIFY_SERIALIZED_CHARS.toInt()))),
            content = "",
            sig = "cc".repeat(32),
        )

        assertFalse(event.verify())
    }

    @Test
    fun serializedEstimateAccountsForEscapedCharacters() {
        val plain = Nip01Event.estimatedSerializedCharCount(
            pubkey = "aa",
            tags = listOf(listOf("x", "abc")),
            content = "hello",
        )
        val escaped = Nip01Event.estimatedSerializedCharCount(
            pubkey = "aa",
            tags = listOf(listOf("x", "a\"c")),
            content = "hello\nthere",
        )

        assertTrue(escaped > plain)
    }
}
