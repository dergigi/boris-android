package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class EventCacheTest {
    @Before
    fun setUp() {
        EventCache.clear()
    }

    @Test
    fun storesAndFindsEventById() {
        val event = event(id = "AA01", kind = Nip01Event.KIND_TEXT_NOTE)
        EventCache.put(event)
        assertEquals(event, EventCache.event("aa01"))
        assertEquals(event, EventCache.event("AA01"))
    }

    @Test
    fun newestWinsForReplaceableKinds() {
        val old = event(id = "old1", kind = Nip01Event.KIND_METADATA, createdAt = 100)
        val new = event(id = "new1", kind = Nip01Event.KIND_METADATA, createdAt = 200)
        EventCache.put(new)
        EventCache.put(old)
        assertEquals(new, EventCache.latest(Nip01Event.KIND_METADATA, PUBKEY))
        assertNull(EventCache.event("old1"))
    }

    @Test
    fun newerReplaceableEvictsOlderById() {
        val old = event(id = "old2", kind = Nip01Event.KIND_RELAY_LIST, createdAt = 100)
        val new = event(id = "new2", kind = Nip01Event.KIND_RELAY_LIST, createdAt = 200)
        EventCache.put(old)
        EventCache.put(new)
        assertEquals(new, EventCache.latest(Nip01Event.KIND_RELAY_LIST, PUBKEY))
        assertNull(EventCache.event("old2"))
        assertEquals(new, EventCache.event("new2"))
    }

    @Test
    fun addressableKindsKeyedByIdentifier() {
        val one = event(
            id = "art1",
            kind = Nip01Event.KIND_LONG_FORM,
            tags = listOf(listOf("d", "first")),
        )
        val two = event(
            id = "art2",
            kind = Nip01Event.KIND_LONG_FORM,
            tags = listOf(listOf("d", "second")),
        )
        EventCache.putAll(listOf(one, two))
        assertEquals(one, EventCache.latest(Nip01Event.KIND_LONG_FORM, PUBKEY, "first"))
        assertEquals(two, EventCache.latest(Nip01Event.KIND_LONG_FORM, PUBKEY, "second"))
    }

    @Test
    fun byKindReturnsAllOfThatKind() {
        val highlight = event(id = "h001", kind = Nip01Event.KIND_HIGHLIGHT)
        val note = event(id = "n001", kind = Nip01Event.KIND_TEXT_NOTE)
        EventCache.putAll(listOf(highlight, note))
        assertEquals(listOf(highlight), EventCache.byKind(Nip01Event.KIND_HIGHLIGHT))
    }

    @Test
    fun byKindAndAuthorFiltersBoth() {
        val mine = event(id = "e001", kind = Nip01Event.KIND_REACTION)
        val otherKind = event(id = "e002", kind = Nip01Event.KIND_TEXT_NOTE)
        val otherAuthor = event(id = "e003", kind = Nip01Event.KIND_REACTION, pubkey = OTHER)
        EventCache.putAll(listOf(mine, otherKind, otherAuthor))
        val found = EventCache.byKindAndAuthor(setOf(Nip01Event.KIND_REACTION), PUBKEY)
        assertEquals(listOf(mine), found)
    }

    @Test
    fun deletionRemovesReferencedEvent() {
        val reaction = event(id = "dead", kind = Nip01Event.KIND_REACTION)
        EventCache.put(reaction)
        val deletion = event(
            id = "del1",
            kind = Nip01Event.KIND_DELETION,
            tags = listOf(listOf("e", "dead")),
        )
        EventCache.applyDeletion(deletion)
        assertNull(EventCache.event("dead"))
    }

    @Test
    fun deletionIgnoresOtherAuthors() {
        val reaction = event(id = "safe", kind = Nip01Event.KIND_REACTION)
        EventCache.put(reaction)
        val deletion = event(
            id = "del2",
            kind = Nip01Event.KIND_DELETION,
            pubkey = OTHER,
            tags = listOf(listOf("e", "safe")),
        )
        EventCache.applyDeletion(deletion)
        assertNotNull(EventCache.event("safe"))
    }

    @Test
    fun deletionRemovesAddressableByCoordinate() {
        val article = event(
            id = "art3",
            kind = Nip01Event.KIND_LONG_FORM,
            tags = listOf(listOf("d", "gone")),
        )
        EventCache.put(article)
        val deletion = event(
            id = "del3",
            kind = Nip01Event.KIND_DELETION,
            tags = listOf(listOf("a", "${Nip01Event.KIND_LONG_FORM}:$PUBKEY:gone")),
        )
        EventCache.applyDeletion(deletion)
        assertNull(EventCache.latest(Nip01Event.KIND_LONG_FORM, PUBKEY, "gone"))
        assertNull(EventCache.event("art3"))
    }

    private fun event(
        id: String,
        kind: Int,
        pubkey: String = PUBKEY,
        createdAt: Long = 1_000,
        tags: List<List<String>> = emptyList(),
    ): Nip01Event = Nip01Event(
        id = id,
        pubkey = pubkey,
        createdAt = createdAt,
        kind = kind,
        tags = tags,
        content = "",
        sig = "",
    )

    companion object {
        private val PUBKEY = "ab".repeat(32)
        private val OTHER = "cd".repeat(32)
    }
}
