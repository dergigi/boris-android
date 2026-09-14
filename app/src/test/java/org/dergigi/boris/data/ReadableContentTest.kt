package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadableContentTest {
    @Test
    fun hidesSummaryThatDuplicatesOpeningParagraph() {
        val content = ReadableContent(
            url = "https://example.com/post",
            summary = "Fear has a contagion of its own.",
            markdown = "Fear has a contagion of its own.\n\nThe second paragraph continues.",
        )

        assertNull(content.displaySummary)
    }

    @Test
    fun hidesSummaryThatDuplicatesFormattedOpeningParagraph() {
        val content = ReadableContent(
            url = "https://example.com/post",
            summary = "Fear has a contagion of its own.",
            markdown = "![cover](https://example.com/cover.jpg)\n\n**Fear** has a contagion of its own.",
        )

        assertNull(content.displaySummary)
    }

    @Test
    fun keepsSummaryWhenOpeningParagraphDiffers() {
        val content = ReadableContent(
            url = "https://example.com/post",
            summary = "A concise description of the article.",
            markdown = "The article opens with a different paragraph.\n\nMore text follows.",
        )

        assertEquals("A concise description of the article.", content.displaySummary)
    }
}
