package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip01Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedArticlesTest {
    private val naddr =
        "naddr1qvzqqqr4gupzqwlsccluhy6xxsr6l9a9uhhxf75g85g8a709tprjcn4e42h053vaqq9x67fdv9e8g6trd3jsrnn0q2"

    @Test
    fun extractsArticleLinksFromNotes() {
        val note = note(
            """
            Read this: https://example.com/blog/one.
            And this [essay](https://example.com/essays/two).
            Also nostr:$naddr
            Skip code `https://example.com/blog/code`.
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                "https://example.com/essays/two",
                "https://example.com/blog/one",
                "nostr:$naddr",
            ),
            LinkedArticles.fromNotes(listOf(note)).map { it.url },
        )
    }

    @Test
    fun ignoresProfilesNotesMediaAndObviousShorteners() {
        assertFalse(
            LinkedArticles.isArticleLike(
                "https://example.com/photo.jpg",
                OgPreview("Photo", null, "Example", "Image"),
            ),
        )
        assertFalse(
            LinkedArticles.isArticleLike(
                "https://example.com/report.pdf",
                OgPreview("Report", null, "Example", "File"),
            ),
        )
        assertFalse(
            LinkedArticles.isArticleLike(
                "https://t.co/abc",
                OgPreview("Redirect", null, "t.co", "Short"),
            ),
        )
        assertFalse(
            LinkedArticles.isArticleLike(
                "nostr:npub1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq4e2cax",
                null,
            ),
        )
    }

    @Test
    fun acceptsReadableMetadataAndNostrLongForm() {
        assertTrue(
            LinkedArticles.isArticleLike(
                "https://example.com/blog/read-this",
                OgPreview("Read this", null, "Example", "A thoughtful essay."),
            ),
        )
        assertTrue(
            LinkedArticles.isArticleLike(
                "https://example.com/posts/read-this",
                OgPreview("Read this", null, "Example"),
            ),
        )
        assertTrue(LinkedArticles.isArticleLike("nostr:$naddr", null))
        assertFalse(
            LinkedArticles.isArticleLike(
                "https://example.com",
                OgPreview("Example", null, "Example"),
            ),
        )
    }

    private fun note(content: String): Nip01Event = Nip01Event(
        id = "11".repeat(32),
        pubkey = "22".repeat(32),
        createdAt = 42,
        kind = Nip01Event.KIND_TEXT_NOTE,
        tags = emptyList(),
        content = content,
        sig = "33".repeat(64),
    )
}
