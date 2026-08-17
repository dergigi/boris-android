package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NoteCoverTest {
    private val pubkey = "aa".repeat(32)
    private val eventId = "bb".repeat(32)

    @Before
    fun setUp() {
        EventCache.clear()
    }

    @After
    fun tearDown() {
        EventCache.clear()
    }

    @Test
    fun prefersImageUrlFromNoteContent() {
        val note = note("Check this https://cdn.example.com/pic.png and more text")
        assertEquals("https://cdn.example.com/pic.png", NoteCover.image(note))
    }

    @Test
    fun fallsBackToAuthorPicture() {
        EventCache.put(
            Nip01Event(
                id = "cc".repeat(32),
                pubkey = pubkey,
                createdAt = 1,
                kind = Nip01Event.KIND_METADATA,
                tags = emptyList(),
                content = """{"name":"Alice","picture":"https://cdn.example.com/alice.png"}""",
                sig = "dd".repeat(64),
            ),
        )
        assertEquals("https://cdn.example.com/alice.png", NoteCover.image(note("Just words")))
    }

    @Test
    fun returnsNullWithoutImageOrPicture() {
        assertNull(NoteCover.image(note("Just words")))
    }

    @Test
    fun titleSkipsBareImageUrls() {
        assertEquals(
            "Hello friends",
            NoteCover.title(note("https://cdn.example.com/pic.png\nHello friends")),
        )
    }

    @Test
    fun titleKeepsLinesThatMentionAnImage() {
        assertEquals(
            "Hello https://cdn.example.com/shot.jpg",
            NoteCover.title(note("Hello https://cdn.example.com/shot.jpg")),
        )
    }

    @Test
    fun decorateUsesNoteCoverForHighlightedCards() {
        EventCache.put(note("Hello https://cdn.example.com/shot.jpg"))
        EventCache.put(
            Nip01Event(
                id = "cc".repeat(32),
                pubkey = pubkey,
                createdAt = 1,
                kind = Nip01Event.KIND_METADATA,
                tags = emptyList(),
                content = """{"name":"Alice","picture":"https://cdn.example.com/alice.png"}""",
                sig = "dd".repeat(64),
            ),
        )
        val uri = "nostr:${Nip19.noteEncode(eventId)}"
        val decorated = HighlightedArticles.decorate(
            HighlightedArticle(uri, "nostr", "nostr", null, 1L),
        )
        assertEquals("Hello https://cdn.example.com/shot.jpg", decorated.title)
        assertEquals("https://cdn.example.com/shot.jpg", decorated.imageUrl)
        assertEquals("Alice", decorated.host)
    }

    private fun note(content: String) = Nip01Event(
        id = eventId,
        pubkey = pubkey,
        createdAt = 10,
        kind = Nip01Event.KIND_TEXT_NOTE,
        tags = emptyList(),
        content = content,
        sig = "ee".repeat(64),
    )
}
