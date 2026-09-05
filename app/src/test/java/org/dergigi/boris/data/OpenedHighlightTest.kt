package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip01Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenedHighlightTest {
    @Test
    fun readsHttpArticleAndContext() {
        val event = highlight(
            content = "  a chosen line  ",
            tags = listOf(
                listOf("r", "https://example.com/post"),
                listOf("context", "before a chosen line after"),
            ),
        )
        val opened = openedHighlightFrom(event)!!
        assertEquals(event.id, opened.id)
        assertEquals("a chosen line", opened.quote)
        assertEquals("before a chosen line after", opened.context)
        assertEquals("https://example.com/post", opened.articleUrl)
        assertEquals("example.com", opened.host)
        assertEquals(event.pubkey, opened.authorPubkey)
        assertEquals(42L, opened.createdAt)
        assertNull(opened.comment)
    }

    @Test
    fun readsCommentTag() {
        val event = highlight(
            tags = listOf(
                listOf("r", "https://example.com/post", "source"),
                listOf("comment", "why this matters"),
            ),
        )
        assertEquals("why this matters", openedHighlightFrom(event)!!.comment)
        assertEquals("https://example.com/post", openedHighlightFrom(event)!!.articleUrl)
    }

    @Test
    fun readsLongFormArticlePointer() {
        val coordinate =
            "30023:3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d:my-article"
        val event = highlight(tags = listOf(listOf("a", coordinate)))
        val opened = openedHighlightFrom(event)!!
        assertTrue(opened.articleUrl.startsWith("nostr:"))
        assertEquals("my-article", NostrArticle.parse(opened.articleUrl)?.pointer?.identifier)
    }

    @Test
    fun skipsBlankQuote() {
        assertNull(openedHighlightFrom(highlight(content = "   ")))
    }

    @Test
    fun skipsHighlightWithoutArticle() {
        assertNull(openedHighlightFrom(highlight(tags = listOf(listOf("alt", "x")))))
    }

    @Test
    fun skipsNonHighlightKinds() {
        assertNull(openedHighlightFrom(highlight(kind = Nip01Event.KIND_TEXT_NOTE)))
    }

    private fun highlight(
        content: String = "quote",
        tags: List<List<String>> = listOf(listOf("r", "https://example.com/post")),
        kind: Int = Nip01Event.KIND_HIGHLIGHT,
    ): Nip01Event = Nip01Event(
        id = "11".repeat(32),
        pubkey = "aa".repeat(32),
        createdAt = 42L,
        kind = kind,
        tags = tags,
        content = content,
        sig = "bb".repeat(32),
    )
}
