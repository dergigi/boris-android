package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightSpanTest {
    @Test
    fun shortAmbiguousHighlightWithoutContextMatchesNothing() {
        val mine = PaintedHighlight(id = "a", quote = "the", mine = true)
        val spans = matchHighlightSpans("the cat the hat", listOf(mine))
        assertTrue(spans.isEmpty())
    }

    @Test
    fun uniqueShortHighlightWithoutContextStillPaints() {
        val mine = PaintedHighlight(id = "a", quote = "cat", mine = true)
        val spans = matchHighlightSpans("the cat sat", listOf(mine))
        assertEquals(listOf(4 to 7), spans.map { it.start to it.end })
    }

    @Test
    fun uniqueShortHighlightWithUnmatchedContextStillPaints() {
        val mine = PaintedHighlight(
            id = "a",
            quote = "cat",
            mine = true,
            context = "The selected context lives in a different article.",
        )
        val spans = matchHighlightSpans("the cat sat", listOf(mine))
        assertEquals(listOf(4 to 7), spans.map { it.start to it.end })
    }

    @Test
    fun longerLegacyHighlightWithoutContextCanStillMatchEveryOccurrence() {
        val mine = PaintedHighlight(id = "a", quote = "quiet reading flow", mine = true)
        val spans = matchHighlightSpans(
            "quiet reading flow starts here, and quiet reading flow returns later",
            listOf(mine),
        )
        assertEquals(listOf(0 to 18, 36 to 54), spans.map { it.start to it.end })
        assertTrue(spans.all { it.item === mine })
    }

    @Test
    fun contextPinsARepeatedQuoteToOneOccurrence() {
        val displayed = "The cat sat. The cat ran. The cat slept."
        val mine = PaintedHighlight(
            id = "a",
            quote = "cat",
            mine = true,
            context = "The cat sat. The cat ran. The cat slept.",
        )
        val spans = matchHighlightSpans(displayed, listOf(mine))
        assertEquals(1, spans.size)
        val marked = displayed.substring(spans[0].start, spans[0].end)
        assertEquals("cat", marked)
        assertEquals(displayed.indexOf("cat", displayed.indexOf("ran") - 8), spans[0].start)
    }

    @Test
    fun contextSkipsABlockThatIsNotTheSelectedWindow() {
        val mine = PaintedHighlight(
            id = "a",
            quote = "cat",
            mine = true,
            context = "The cat sat. The cat ran.",
        )
        assertTrue(matchHighlightSpans("Other paragraph. The dog ran.", listOf(mine)).isEmpty())
    }

    @Test
    fun unmatchedContextDoesNotPaintEveryShortOccurrence() {
        val mine = PaintedHighlight(
            id = "a",
            quote = "sovereignty",
            mine = true,
            context = "The selected context lives in a different article.",
        )
        val spans = matchHighlightSpans(
            "sovereignty appears here. sovereignty appears there.",
            listOf(mine),
        )
        assertTrue(spans.isEmpty())
    }

    @Test
    fun emptyTextOrHighlightsMatchNothing() {
        val mine = PaintedHighlight(id = "a", quote = "word", mine = true)
        assertTrue(matchHighlightSpans("", listOf(mine)).isEmpty())
        assertTrue(matchHighlightSpans("some text", emptyList()).isEmpty())
    }

    @Test
    fun quoteMissingFromBlockMatchesNothing() {
        val mine = PaintedHighlight(id = "a", quote = "absent", mine = true)
        assertTrue(matchHighlightSpans("this paragraph has other words", listOf(mine)).isEmpty())
    }

    @Test
    fun spokenSpansIgnoreBlankSentence() {
        assertTrue(matchSpokenSpans("hello there", null, null).isEmpty())
        assertTrue(matchSpokenSpans("hello there", "   ", null).isEmpty())
    }

    @Test
    fun spokenSpansMatchWhenDisplayedKeepsAUrl() {
        val displayed = "Read this https://example.com/docs and keep going."
        val spoken = "Read this and keep going."
        val spans = matchSpokenSpans(displayed, spoken, spoken)
        assertEquals(1, spans.size)
        val marked = displayed.substring(spans[0].start, spans[0].end)
        assertTrue(marked.startsWith("Read this"))
        assertTrue(marked.contains("https://example.com/docs"))
        assertTrue(marked.endsWith("going"))
    }

    @Test
    fun spokenHighlightUsesParagraphContext() {
        val spoken = ArticleFind.paintedSpoken(
            sentence = "Repeated sentence.",
            paragraph = "Target paragraph. Repeated sentence.",
        )!!
        assertTrue(
            matchHighlightSpans("Other paragraph. Repeated sentence.", listOf(spoken)).isEmpty(),
        )
        val spans = matchHighlightSpans("Target paragraph. Repeated sentence.", listOf(spoken))
        assertEquals(listOf(18 to 36), spans.map { it.start to it.end })
    }

    @Test
    fun spokenFollowAlongRequiresTheActiveTtsParagraph() {
        val spans = matchSpokenSpansForParagraph(
            displayed = "The wrong section starts here.",
            sentence = "The",
            paragraph = "The right section starts here.",
            displayedTtsIndex = 4,
            spokenTtsIndex = 8,
        )

        assertTrue(spans.isEmpty())
    }

    @Test
    fun spokenFollowAlongMatchesTheActiveTtsParagraph() {
        val displayed = "The right section starts here."
        val spans = matchSpokenSpansForParagraph(
            displayed = displayed,
            sentence = "The",
            paragraph = displayed,
            displayedTtsIndex = 8,
            spokenTtsIndex = 8,
        )

        assertEquals(listOf(0 to 3), spans.map { it.start to it.end })
    }

    @Test
    fun stopsFromSpansKeepsOrderAndSkipsUnlaidRanges() {
        val item = PaintedHighlight(id = "h", quote = "x", mine = false)
        val spans = listOf(
            HighlightSpan(item, start = 10, end = 12),
            HighlightSpan(item, start = 2, end = 4),
        )
        val stops = HighlightJump.stopsFromSpans("owner", spans) { start, _ ->
            if (start == 2) 5f else null
        }
        assertEquals(1, stops.size)
        assertEquals(2, stops.single().start)
        assertEquals(5f, stops.single().localTop)
    }
}
