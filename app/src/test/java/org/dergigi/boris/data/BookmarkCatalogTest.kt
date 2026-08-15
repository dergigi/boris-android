package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip01Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkCatalogTest {
    private val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
    private val coordinate = "30023:$pubkey:my-article"

    @Test
    fun splitsPublicPrivateAndWebShelves() {
        val list = event(
            kind = Nip01Event.KIND_BOOKMARKS,
            tags = listOf(
                listOf("a", coordinate),
                listOf("r", "https://www.citadel21.com/the-paranoid-wallet"),
                listOf("e", "aa".repeat(32)),
            ),
            content = "encrypted",
            createdAt = 50,
        )
        val article = event(
            kind = Nip01Event.KIND_LONG_FORM,
            tags = listOf(
                listOf("d", "my-article"),
                listOf("title", "My Article"),
                listOf("image", "https://example.com/cover.jpg"),
            ),
            pubkey = pubkey,
            createdAt = 40,
        )
        val web = event(
            kind = Nip01Event.KIND_WEB_BOOKMARK,
            tags = listOf(
                listOf("d", "alice.blog/post"),
                listOf("title", "Alice"),
            ),
            createdAt = 30,
        )
        val hidden = listOf(listOf("r", "https://example.com/secret"))
        val shelves = BookmarkCatalog.build(
            listEvent = list,
            hiddenTags = hidden,
            webEvents = listOf(web),
            articles = mapOf(coordinate to article),
        )
        assertEquals(1, shelves.private.size)
        assertEquals("example.com", shelves.private[0].host)
        assertEquals("https://example.com/secret", shelves.private[0].url)
        assertEquals(BookmarkBucket.Private, shelves.private[0].bucket)
        assertEquals(3, shelves.public.size)
        assertEquals("My Article", shelves.public[0].title)
        assertTrue(shelves.public[0].url!!.startsWith("nostr:naddr1"))
        assertEquals("citadel21.com", shelves.public[1].host)
        assertEquals("Note", shelves.public[2].title)
        assertTrue(shelves.public[2].url!!.startsWith("nostr:note1"))
        assertEquals("nostr", shelves.public[2].host)
        assertEquals("Alice", shelves.web[0].title)
        assertEquals("https://alice.blog/post", shelves.web[0].url)
        assertFalse(shelves.privateLocked)
    }

    @Test
    fun publicNotesUseFetchedContentAsTitle() {
        val eventId = "aa".repeat(32)
        val list = event(
            kind = Nip01Event.KIND_BOOKMARKS,
            tags = listOf(listOf("e", eventId)),
        )
        val note = event(
            kind = Nip01Event.KIND_TEXT_NOTE,
            tags = emptyList(),
            content = "A short note about reading.\nSecond line.",
            createdAt = 9,
        ).copy(id = eventId)
        val shelves = BookmarkCatalog.build(
            listEvent = list,
            hiddenTags = emptyList(),
            webEvents = emptyList(),
            notes = mapOf(eventId to note),
        )
        assertEquals(1, shelves.public.size)
        assertEquals("A short note about reading.", shelves.public[0].title)
        assertTrue(shelves.public[0].url!!.startsWith("nostr:note1"))
    }

    @Test
    fun privateLockedWhenContentIsEncryptedAndHiddenTagsMissing() {
        val list = event(
            kind = Nip01Event.KIND_BOOKMARKS,
            tags = emptyList(),
            content = "Agcipher",
        )
        val shelves = BookmarkCatalog.build(list, hiddenTags = null, webEvents = emptyList())
        assertTrue(shelves.privateLocked)
        assertTrue(shelves.private.isEmpty())
    }

    private fun event(
        kind: Int,
        tags: List<List<String>>,
        content: String = "",
        createdAt: Long = 1,
        pubkey: String = this.pubkey,
    ): Nip01Event = Nip01Event(
        id = "11".repeat(32),
        pubkey = pubkey,
        createdAt = createdAt,
        kind = kind,
        tags = tags,
        content = content,
        sig = "22".repeat(64),
    )
}
