package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightSpanTest {
    @Test
    fun matchesEveryOccurrencePerHighlight() {
        val mine = PaintedHighlight(id = "a", quote = "the", mine = true)
        val spans = matchHighlightSpans("the cat the hat", listOf(mine))
        assertEquals(listOf(0 to 3, 8 to 11), spans.map { it.start to it.end })
        assertTrue(spans.all { it.item === mine })
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
