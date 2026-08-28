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
    fun fractionClampsToUnitRange() {
        assertEquals(0f, ReadingProgress.fraction(0, 0), 0f)
        assertEquals(0.5f, ReadingProgress.fraction(500, 1000), 0.0001f)
        assertEquals(1f, ReadingProgress.fraction(1200, 1000), 0f)
    }

    @Test
    fun restoreOffsetSkipsNoiseAndMissingLayout() {
        assertEquals(null, ReadingProgress.restoreOffset(0.5f, 0))
        assertEquals(null, ReadingProgress.restoreOffset(0f, 1000))
        assertEquals(null, ReadingProgress.restoreOffset(0.009f, 1000))
        assertEquals(500, ReadingProgress.restoreOffset(0.5f, 1000))
        assertEquals(1000, ReadingProgress.restoreOffset(1.5f, 1000))
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

    @Test
    fun jumpBackMatchesBlueProgressBand() {
        assertFalse(ReadingProgress.showsJumpBack(0f))
        assertFalse(ReadingProgress.showsJumpBack(0.009f))
        assertFalse(ReadingProgress.showsJumpBack(0.05f))
        assertFalse(ReadingProgress.showsJumpBack(0.10f))
        assertTrue(ReadingProgress.showsJumpBack(0.11f))
        assertTrue(ReadingProgress.showsJumpBack(0.50f))
        assertTrue(ReadingProgress.showsJumpBack(0.94f))
        assertFalse(ReadingProgress.showsJumpBack(0.95f))
        assertFalse(ReadingProgress.showsJumpBack(1f))
    }
}
