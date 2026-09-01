package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LongParagraphsTest {
    @Test
    fun leavesShortMarkdownAlone() {
        val src = "# Title\n\nA normal paragraph.\n\n- a list item"
        assertEquals(src, LongParagraphs.split(src))
    }

    @Test
    fun splitsGiantSingleLineAtSentenceBoundaries() {
        val sentence = "This sentence pads out the wall of text a little more. "
        val src = sentence.repeat(20).trim()
        val out = LongParagraphs.split(src, maxLength = 200)
        val blocks = out.split("\n\n")
        assertTrue(blocks.size > 1)
        blocks.forEach { block ->
            assertTrue("block too long: ${block.length}", block.length <= 200)
            assertTrue("split mid-sentence: $block", block.endsWith("."))
        }
        assertEquals(wordsOf(src), wordsOf(out))
    }

    @Test
    fun breaksSoftWrappedRunsWithBlankLines() {
        val line = "Soft wrapped line of the same endless paragraph, no break."
        val src = (1..30).joinToString("\n") { line }
        val out = LongParagraphs.split(src, maxLength = 200)
        val blocks = out.split("\n\n")
        assertTrue(blocks.size > 1)
        blocks.forEach { assertTrue(it.length <= 200 + line.length) }
        assertEquals(wordsOf(src), wordsOf(out))
    }

    @Test
    fun splitsWordsWhenThereAreNoSentences() {
        val src = "word ".repeat(200).trim()
        val out = LongParagraphs.split(src, maxLength = 100)
        out.split("\n\n").forEach { assertTrue(it.length <= 100) }
        assertEquals(wordsOf(src), wordsOf(out))
    }

    @Test
    fun leavesCodeFencesAlone() {
        val code = "```\n" + "x".repeat(500) + "\n```"
        val src = "Intro.\n\n$code\n\nOutro."
        assertEquals(src, LongParagraphs.split(src, maxLength = 100))
    }

    @Test
    fun keepsQuotePrefixOnSplitPieces() {
        val quoted = "> " + "A quoted sentence that carries on. ".repeat(20).trim()
        val out = LongParagraphs.split("$quoted\n\nAfter.", maxLength = 200)
        val pieces = out.split("\n\n").filter { it.startsWith(">") }
        assertTrue(pieces.size > 1)
    }

    @Test
    fun headingsAndListsDoNotJoinTheRun() {
        val src = listOf(
            "First paragraph line one.",
            "# Heading",
            "- item one",
            "Second paragraph line.",
        ).joinToString("\n")
        assertEquals(src, LongParagraphs.split(src, maxLength = 60))
    }

    private fun wordsOf(text: String): List<String> =
        text.split(Regex("\\s+")).filter { it.isNotBlank() }
}
