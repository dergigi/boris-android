package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineOutboxTest {
    @Before
    fun setUp() {
        OfflineOutbox.reset()
    }

    @Test
    fun addAndRemoveKeepOrder() {
        val first = event("aa01")
        val second = event("bb02")
        OfflineOutbox.add(first)
        OfflineOutbox.add(second)
        assertEquals(listOf(first, second), OfflineOutbox.pending())
        OfflineOutbox.remove("AA01")
        assertEquals(listOf(second), OfflineOutbox.pending())
        OfflineOutbox.remove("missing")
        assertEquals(listOf(second), OfflineOutbox.pending())
    }

    @Test
    fun addReplacesSameId() {
        OfflineOutbox.add(event("cc03", content = "old"))
        OfflineOutbox.add(event("cc03", content = "new"))
        assertEquals(1, OfflineOutbox.pending().size)
        assertEquals("new", OfflineOutbox.pending().single().content)
    }

    @Test
    fun startsEmpty() {
        assertTrue(OfflineOutbox.pending().isEmpty())
    }

    private fun event(id: String, content: String = ""): Nip01Event =
        Nip01Event(
            id = id,
            pubkey = "ab".repeat(32),
            createdAt = 1_000,
            kind = Nip01Event.KIND_HIGHLIGHT,
            tags = emptyList(),
            content = content,
            sig = "cd".repeat(32),
        )
}
