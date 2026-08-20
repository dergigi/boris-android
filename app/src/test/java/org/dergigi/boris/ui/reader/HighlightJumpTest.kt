package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightJumpTest {
    @org.junit.After
    fun clearFocus() {
        ReaderFocus.clear()
    }

    @Test
    fun stopsFollowReadingOrderInOneBlock() {
        val owner = Any()
        val highlights = listOf(
            PaintedHighlight("later", "second", mine = true),
            PaintedHighlight("first", "first", mine = false),
        )
        val stops = HighlightJump.stopsInText(owner, "first then second", highlights) { start, _ ->
            start.toFloat()
        }
        assertEquals(listOf("first", "later"), stops.map { it.highlightId })
        assertEquals(0, stops[0].start)
        assertEquals(11, stops[1].start)
    }

    @Test
    fun skipsQuotesThatAreNotInTheText() {
        val stops = HighlightJump.stopsInText(
            owner = Any(),
            text = "only this",
            highlights = listOf(PaintedHighlight("x", "missing", mine = true)),
            localTopAt = { start, _ -> start.toFloat() },
        )
        assertTrue(stops.isEmpty())
    }

    @Test
    fun nextIndexWrapsAround() {
        assertEquals(0, HighlightJump.nextIndex(-1, 3))
        assertEquals(1, HighlightJump.nextIndex(0, 3))
        assertEquals(0, HighlightJump.nextIndex(2, 3))
        assertEquals(-1, HighlightJump.nextIndex(0, 0))
    }

    @Test
    fun repeatedQuoteMakesMultipleStops() {
        val stops = HighlightJump.stopsInText(
            owner = Any(),
            text = "echo echo",
            highlights = listOf(PaintedHighlight("a", "echo", mine = true)),
            localTopAt = { start, _ -> start.toFloat() },
        )
        assertEquals(listOf(0, 5), stops.map { it.start })
    }

    @Test
    fun inDocumentOrderFollowsFirstOccurrence() {
        val late = PaintedHighlight("late", "end quote", mine = true, createdAt = 1)
        val early = PaintedHighlight("early", "start quote", mine = false, createdAt = 9)
        val missing = PaintedHighlight("ghost", "not in the article", mine = true, createdAt = 5)
        val ordered = HighlightJump.inDocumentOrder(
            listOf(late, missing, early),
            listOf("Title with start quote", "Body then end quote."),
        )
        assertEquals(listOf("early", "late", "ghost"), ordered.map { it.id })
    }

    @Test
    fun inDocumentOrderKeepsSingleOrUnmatchedLists() {
        val only = listOf(PaintedHighlight("a", "x", mine = true))
        assertEquals(only, HighlightJump.inDocumentOrder(only, listOf("hello")))
        val none = listOf(
            PaintedHighlight("a", "zzz", mine = true),
            PaintedHighlight("b", "yyy", mine = false),
        )
        assertEquals(none, HighlightJump.inDocumentOrder(none, listOf("hello")))
    }

    @Test
    fun withFocusAddsAMissingHighlightFromTheQuote() {
        val existing = listOf(PaintedHighlight("aa", "kept", mine = true))
        val next = HighlightJump.withFocus(existing, "BB", "seeded")
        assertEquals(2, next.size)
        assertEquals("bb", next.last().id)
        assertEquals("seeded", next.last().quote)
        assertEquals(existing, HighlightJump.withFocus(existing, "aa", "ignored"))
        assertEquals(existing, HighlightJump.withFocus(existing, "cc", "  "))
        assertEquals(existing, HighlightJump.withFocus(existing, "", "seeded"))
    }

    @Test
    fun scrollTargetKeepsTheQuoteNearTheTop() {
        assertEquals(152, HighlightJump.scrollTarget(scrollValue = 200, scrollMax = 2000, yInViewport = 0f, paddingPx = 48f))
        assertEquals(0, HighlightJump.scrollTarget(scrollValue = 10, scrollMax = 2000, yInViewport = 20f, paddingPx = 48f))
        assertEquals(500, HighlightJump.scrollTarget(scrollValue = 400, scrollMax = 500, yInViewport = 300f, paddingPx = 48f))
    }

    @Test
    fun chromePaddingClearsTheFloatingTopBar() {
        assertEquals(48f, HighlightJump.chromePadding(0, 48f))
        assertEquals(168f, HighlightJump.chromePadding(120, 48f))
        assertEquals(32, HighlightJump.scrollTarget(scrollValue = 200, scrollMax = 2000, yInViewport = 0f, paddingPx = 168f))
    }

    @Test
    fun chromePaddingShrinksWhenTheTopBarHasSlidAway() {
        val visible = (120 + -120f).toInt().coerceAtLeast(0)
        assertEquals(48f, HighlightJump.chromePadding(visible, 48f))
        val halfHidden = (120 + -60f).toInt().coerceAtLeast(0)
        assertEquals(108f, HighlightJump.chromePadding(halfHidden, 48f))
    }
}
