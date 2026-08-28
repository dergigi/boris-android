package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MostHighlightedWindowTest {
    @Test
    fun fromIdFallsBackToWeek() {
        assertEquals(MostHighlightedWindow.Day, MostHighlightedWindow.fromId("24h"))
        assertEquals(MostHighlightedWindow.Week, MostHighlightedWindow.fromId("7d"))
        assertEquals(MostHighlightedWindow.Month, MostHighlightedWindow.fromId("30d"))
        assertEquals(MostHighlightedWindow.Week, MostHighlightedWindow.fromId("nope"))
        assertEquals(MostHighlightedWindow.Week, MostHighlightedWindow.fromId(null))
    }

    @Test
    fun sinceSubtractsTheWindow() {
        val now = 1_700_000_000L
        assertEquals(now - 24L * 60 * 60, MostHighlightedWindow.Day.since(now))
        assertEquals(now - 7L * 24 * 60 * 60, MostHighlightedWindow.Week.since(now))
        assertEquals(now - 30L * 24 * 60 * 60, MostHighlightedWindow.Month.since(now))
    }
}
