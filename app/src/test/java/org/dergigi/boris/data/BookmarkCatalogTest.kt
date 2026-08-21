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
    fun mergedLibraryDedupesSameUrlAcrossShelves() {
        val list = event(
            kind = Nip01Event.KIND_BOOKMARKS,
            tags = listOf(listOf("r", "https://www.example.com/read/")),
            createdAt = 10,
        )
        val web = event(
            kind = Nip01Event.KIND_WEB_BOOKMARK,
            tags = listOf(
                listOf("d", "example.com/read"),
                listOf("title", "Web title"),
                listOf("published_at", "20"),
            ),
            createdAt = 20,
        )

        val shelves = BookmarkCatalog.build(
            listEvent = list,
            hiddenTags = emptyList(),
            webEvents = listOf(web),
        )

        assertEquals(1, shelves.public.size)
        assertEquals(1, shelves.web.size)
        assertEquals(1, shelves.merged().size)
        assertEquals("Web title", shelves.merged().single().title)
    }

    @Test
    fun publicHighlightBookmarksShowQuoteAndSource() {
        val eventId = "aa".repeat(32)
        val list = event(
            kind = Nip01Event.KIND_BOOKMARKS,
            tags = listOf(listOf("e", eventId)),
        )
        val highlight = event(
            kind = Nip01Event.KIND_HIGHLIGHT,
            tags = listOf(
                listOf("r", "https://example.com/essay"),
                listOf("context", "Before the quote. The selected line. After."),
            ),
            content = "The selected line",
            createdAt = 9,
        ).copy(id = eventId)
        val shelves = BookmarkCatalog.build(
            listEvent = list,
            hiddenTags = emptyList(),
            webEvents = emptyList(),
            notes = mapOf(eventId to highlight),
        )
        assertEquals(1, shelves.public.size)
        val item = shelves.public[0]
        assertEquals("The selected line", item.title)
        assertEquals("https://example.com/essay", item.url)
        assertEquals("example.com", item.host)
        assertEquals("Before the quote. The selected line. After.", item.summary)
        assertEquals(eventId, item.highlightId)
        assertTrue(item.isHighlight)
    }

    @Test
    fun highlightBookmarkWithoutSourceFallsBackToNoteUrl() {
        val eventId = "bb".repeat(32)
        val list = event(
            kind = Nip01Event.KIND_BOOKMARKS,
            tags = listOf(listOf("e", eventId)),
        )
        val highlight = event(
            kind = Nip01Event.KIND_HIGHLIGHT,
            tags = emptyList(),
            content = "A lonely quote",
            createdAt = 8,
        ).copy(id = eventId)
        val shelves = BookmarkCatalog.build(
            listEvent = list,
            hiddenTags = emptyList(),
            webEvents = emptyList(),
            notes = mapOf(eventId to highlight),
        )
        val item = shelves.public.single()
        assertEquals("A lonely quote", item.title)
        assertTrue(item.url!!.startsWith("nostr:note1"))
        assertEquals("highlight", item.host)
        assertEquals(eventId, item.highlightId)
        assertTrue(item.isHighlight)
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

    @Test
    fun lookShelfUsesKind7EyesReactions() {
        val eventId = "aa".repeat(32)
        val note = event(
            kind = Nip01Event.KIND_TEXT_NOTE,
            tags = emptyList(),
            content = "Worth a look",
            createdAt = 9,
        ).copy(id = eventId)
        val look = event(
            kind = Nip01Event.KIND_REACTION,
            tags = listOf(listOf("e", eventId)),
            content = "👀",
            createdAt = 20,
        )
        val plus = event(
            kind = Nip01Event.KIND_REACTION,
            tags = listOf(listOf("e", "bb".repeat(32))),
            content = "+",
            createdAt = 21,
        )
        val shelves = BookmarkCatalog.build(
            listEvent = null,
            hiddenTags = emptyList(),
            webEvents = emptyList(),
            lookEvents = listOf(look, plus),
            notes = mapOf(eventId to note),
        )
        assertEquals(1, shelves.look.size)
        assertEquals("Worth a look", shelves.look[0].title)
        assertEquals(BookmarkBucket.Look, shelves.look[0].bucket)
        assertTrue(shelves.look[0].url!!.startsWith("nostr:note1"))
    }

    @Test
    fun archiveShelfUsesBooksReactions() {
        val eventId = "aa".repeat(32)
        val note = event(
            kind = Nip01Event.KIND_TEXT_NOTE,
            tags = emptyList(),
            content = "Finished this one",
            createdAt = 9,
        ).copy(id = eventId)
        val archive = event(
            kind = Nip01Event.KIND_REACTION,
            tags = listOf(listOf("e", eventId)),
            content = "📚",
            createdAt = 20,
        )
        val web = event(
            kind = Nip01Event.KIND_URL_REACTION,
            tags = listOf(listOf("r", "https://example.com/read")),
            content = "📚",
            createdAt = 19,
        )
        val look = event(
            kind = Nip01Event.KIND_REACTION,
            tags = listOf(listOf("e", "bb".repeat(32))),
            content = "👀",
            createdAt = 21,
        )
        val shelves = BookmarkCatalog.build(
            listEvent = null,
            hiddenTags = emptyList(),
            webEvents = emptyList(),
            archiveEvents = listOf(archive, web, look),
            notes = mapOf(eventId to note),
        )
        assertEquals(2, shelves.archive.size)
        assertEquals("Finished this one", shelves.archive[0].title)
        assertEquals(BookmarkBucket.Archive, shelves.archive[0].bucket)
        assertEquals("example.com", shelves.archive[1].host)
        assertEquals("https://example.com/read", shelves.archive[1].url)
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
