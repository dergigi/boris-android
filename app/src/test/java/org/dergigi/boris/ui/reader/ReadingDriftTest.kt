package org.dergigi.boris.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingDriftTest {
    private val viewport = 1000

    @Test
    fun slowReadingNearSavedPositionSaves() {
        // Steady progression: each settle within a viewport of the saved spot.
        assertTrue(ReadingDrift.shouldSave(5800, 5000, 5000, viewport))
        assertTrue(ReadingDrift.shouldSave(6500, 5800, 5800, viewport))
    }

    @Test
    fun movementWithinThreeViewportsStillCountsAsReading() {
        assertTrue(ReadingDrift.shouldSave(8000, 5000, 5000, viewport))
        assertTrue(ReadingDrift.shouldSave(2000, 5000, 5000, viewport))
    }

    @Test
    fun fastJumpAwayDoesNotOverwriteSavedPosition() {
        assertFalse(ReadingDrift.shouldSave(20000, 5000, 5000, viewport))
        assertFalse(ReadingDrift.shouldSave(0, 20000, 20000, viewport))
    }

    @Test
    fun continuedFastScanningWhileDriftedKeepsSavedPosition() {
        // Drifted to 20000, next flick lands another 5 viewports away.
        assertFalse(ReadingDrift.shouldSave(25000, 5000, 20000, viewport))
    }

    @Test
    fun slowReadingAtDriftedSpotAdoptsNewPosition() {
        // Drifted to 20000, then a small local scroll means reading resumed there.
        assertTrue(ReadingDrift.shouldSave(20600, 5000, 20000, viewport))
    }

    @Test
    fun firstSettleAfterJumpWithoutHistoryDoesNotAdopt() {
        assertFalse(ReadingDrift.shouldSave(20000, 5000, null, viewport))
    }

    @Test
    fun scrollingBackToSavedPositionResumesSaving() {
        assertTrue(ReadingDrift.shouldSave(5200, 5000, 20000, viewport))
    }

    @Test
    fun degenerateViewportFallsBackToAlwaysSaving() {
        assertTrue(ReadingDrift.shouldSave(20000, 0, null, 0))
    }
}
