package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingProgressTest {
    @Test
    fun percentIsZeroBeforeLayout() {
        assertEquals(0, ReadingProgress.percent(0, 0))
        assertEquals(0, ReadingProgress.percent(0, 1000))
    }

    @Test
    fun percentRoundsScrollFraction() {
        assertEquals(50, ReadingProgress.percent(500, 1000))
        assertEquals(50, ReadingProgress.percent(499, 1000))
        assertEquals(1, ReadingProgress.percent(10, 1000))
        assertEquals(100, ReadingProgress.percent(1000, 1000))
        assertEquals(100, ReadingProgress.percent(1200, 1000))
    }

    @Test
    fun completeMatchesWebappThreshold() {
        assertFalse(ReadingProgress.isComplete(94))
        assertTrue(ReadingProgress.isComplete(95))
        assertTrue(ReadingProgress.isComplete(100))
        assertTrue(ReadingProgress.isStarted(1))
        assertTrue(ReadingProgress.isStarted(10))
        assertFalse(ReadingProgress.isStarted(0))
        assertFalse(ReadingProgress.isStarted(11))
    }
}
