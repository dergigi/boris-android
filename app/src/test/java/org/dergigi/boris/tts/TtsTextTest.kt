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
    fun paragraphsSpeakAtNameForRawNpubMention() {
        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        val label = "@" + org.dergigi.boris.nostr.Profile.displayName(
            "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            null,
        )
        val content = ReadableContent(
            url = "https://example.com/mention",
            markdown = "Thanks nostr:$npub for writing.",
        )
        val paragraphs = TtsText.paragraphs(content)
        assertEquals(listOf("Thanks $label for writing."), paragraphs)
        assertFalse(paragraphs.any { it.contains(npub) })
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
    fun startIndexForSelectionFindsBodyParagraph() {
        val content = ReadableContent(
            url = "https://example.com/selection",
            title = "Title",
            summary = "Summary",
            markdown = "First paragraph.\n\nSecond paragraph with the chosen words.\n\nThird paragraph.",
        )
        assertEquals(
            3,
            TtsText.startIndexForSelection(
                content,
                ownerText = "Second paragraph with the chosen words.",
                selectedText = "chosen words",
            ),
        )
    }

    @Test
    fun fromSentenceStartsAtContainingSentence() {
        val paragraph = "First sentence. Second sentence here. Third one."
        assertEquals(
            "Second sentence here. Third one.",
            TtsText.fromSentence(paragraph, "sentence here"),
        )
        assertEquals(paragraph, TtsText.fromSentence(paragraph, "First"))
        assertEquals(paragraph, TtsText.fromSentence(paragraph, "missing"))
        assertEquals(
            "Only one sentence.",
            TtsText.fromSentence("Only one sentence.", "sentence"),
        )
    }

    @Test
    fun fromSentenceUsesOwnerOffsetWhenTheWordRepeats() {
        val paragraph = "The cat sat. The cat ran."
        assertEquals(
            "The cat ran.",
            TtsText.fromSentence(
                paragraph = paragraph,
                selectedText = "cat",
                ownerText = paragraph,
                ownerOffset = paragraph.lastIndexOf("cat"),
            ),
        )
        assertEquals(
            paragraph,
            TtsText.fromSentence(
                paragraph = paragraph,
                selectedText = "cat",
                ownerText = paragraph,
                ownerOffset = paragraph.indexOf("cat"),
            ),
        )
    }

    @Test
    fun applySentenceStartTrimsOnlyTheStartingParagraph() {
        val paragraphs = listOf("Title", "One. Two. Three.", "Next.")
        assertEquals(
            listOf("Title", "Two. Three.", "Next."),
            TtsText.applySentenceStart(paragraphs, 1, "Two"),
        )
    }

    @Test
    fun startIndexForSelectionFindsTitle() {
        val content = ReadableContent(
            url = "https://example.com/title",
            title = "A Selected Title",
            markdown = "Body paragraph.",
        )
        assertEquals(
            0,
            TtsText.startIndexForSelection(
                content,
                ownerText = "A Selected Title",
                selectedText = "Selected",
            ),
        )
    }

    @Test
    fun startIndexForMarkdownOffsetPreservesRepeatedParagraphOccurrence() {
        val markdown = """
            Repeat this paragraph.

            Something else.

            Repeat this paragraph.
        """.trimIndent()
        val content = ReadableContent(
            url = "https://example.com/repeated",
            markdown = markdown,
        )
        assertEquals(
            2,
            TtsText.startIndexForMarkdownOffset(content, markdown.lastIndexOf("Repeat")),
        )
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
