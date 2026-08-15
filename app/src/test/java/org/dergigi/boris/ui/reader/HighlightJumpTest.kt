package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightJumpTest {
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
    fun scrollTargetKeepsTheQuoteNearTheTop() {
        assertEquals(152, HighlightJump.scrollTarget(scrollValue = 200, scrollMax = 2000, yInViewport = 0f, paddingPx = 48f))
        assertEquals(0, HighlightJump.scrollTarget(scrollValue = 10, scrollMax = 2000, yInViewport = 20f, paddingPx = 48f))
        assertEquals(500, HighlightJump.scrollTarget(scrollValue = 400, scrollMax = 500, yInViewport = 300f, paddingPx = 48f))
    }
}
