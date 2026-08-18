package org.dergigi.boris.tts

import org.dergigi.boris.data.ReadableContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsTextTest {
    @Test
    fun paragraphsIncludeTitleSummaryThenBody() {
        val content = ReadableContent(
            url = "https://example.com/a",
            title = "The Title",
            summary = "A short summary.",
            markdown = "First paragraph.\n\nSecond paragraph.",
        )
        val paragraphs = TtsText.paragraphs(content)
        assertEquals(
            listOf("The Title", "A short summary.", "First paragraph.", "Second paragraph."),
            paragraphs,
        )
    }

    @Test
    fun paragraphsDropCodeImagesAndTables() {
        val markdown = """
            Intro text.

            ```kotlin
            val secret = "never spoken"
            ```

            ![alt text](https://example.com/pic.png)

            | col a | col b |
            |-------|-------|
            | 1     | 2     |

            Outro text.
        """.trimIndent()
        val content = ReadableContent(url = "https://example.com/b", markdown = markdown)
        val paragraphs = TtsText.paragraphs(content)
        assertEquals(listOf("Intro text.", "Outro text."), paragraphs)
        assertFalse(paragraphs.any { it.contains("secret") })
        assertFalse(paragraphs.any { it.contains("col a") })
    }

    @Test
    fun paragraphsFlattenLinksToLabels() {
        val content = ReadableContent(
            url = "https://example.com/c",
            markdown = "Read [the docs](https://example.com/docs) today.",
        )
        val paragraphs = TtsText.paragraphs(content)
        assertEquals(listOf("Read the docs today."), paragraphs)
    }

    @Test
    fun paragraphsDropReferenceLinkDefinitions() {
        val markdown = """
            Read [Boris][boris] and [the docs][] today.

            Last visible sentence.

            [boris]: https://readwithboris.com/
            [the docs]: https://example.com/docs
                "Reference title"
        """.trimIndent()
        val content = ReadableContent(url = "https://example.com/ref", markdown = markdown)
        val paragraphs = TtsText.paragraphs(content)
        assertEquals(
            listOf("Read Boris and the docs today.", "Last visible sentence."),
            paragraphs,
        )
        assertFalse(paragraphs.any { it.contains("https://") })
        assertFalse(paragraphs.any { it.contains("Reference title") })
    }

    @Test
    fun headingsAndListItemsAreOwnParagraphs() {
        val markdown = """
            # Heading One

            Body text here.

            - first item
            - second item
        """.trimIndent()
        val content = ReadableContent(url = "https://example.com/d", markdown = markdown)
        val paragraphs = TtsText.paragraphs(content)
        assertEquals(
            listOf("Heading One", "Body text here.", "first item", "second item"),
            paragraphs,
        )
    }

    @Test
    fun startIndexNoiseFloorStartsAtTop() {
        assertEquals(0, TtsText.startIndex(0f, 10))
        assertEquals(0, TtsText.startIndex(0.005f, 10))
    }

    @Test
    fun startIndexMapsMidFractionIntoRange() {
        val index = TtsText.startIndex(0.5f, 10)
        assertTrue(index in 0..9)
        assertEquals(5, index)
        assertEquals(9, TtsText.startIndex(1f, 10))
        assertEquals(0, TtsText.startIndex(0.5f, 0))
    }

    @Test
    fun chunksSplitLongTextOnSentencesThenSpaces() {
        val sentence = "This is a sentence. " // 20 chars
        val long = sentence.repeat(10).trim()
        val chunks = TtsText.chunks(long, 50)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 50 })
        assertEquals(long.split(" ").size, chunks.joinToString(" ").split(" ").size)
        val noSpaces = "a".repeat(120)
        assertTrue(TtsText.chunks(noSpaces, 50).all { it.length <= 50 })
        assertEquals(listOf("short"), TtsText.chunks("short", 50))
    }

    @Test
    fun previewSentenceIsLocked() {
        assertEquals(
            "Boris aims to be a calm reader app with clean typography, beautiful design, " +
                "and a focus on readability. Boris does not and will never have ads, trackers, " +
                "paywalls, subscriptions, or any other distractions.",
            TtsPreview.EXAMPLE_TEXT,
        )
    }
}
