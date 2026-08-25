package org.dergigi.boris.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingTrackerTest {
    private val viewport = 1000
    private val saved = 5000

    @Test
    fun steadyReadingNearSavedPositionSaves() {
        val tracker = ReadingTracker()
        assertTrue(tracker.onSettle(5800, saved, viewport, 0))
        assertTrue(tracker.onSettle(6500, 5800, viewport, 10_000))
        assertFalse(tracker.drifting)
    }

    @Test
    fun jumpFreezesSavedPosition() {
        val tracker = ReadingTracker()
        assertFalse(tracker.onSettle(20_000, saved, viewport, 0))
        assertTrue(tracker.drifting)
    }

    @Test
    fun jumpPlusShortDwellAndSmallScrollDoesNotAdopt() {
        val tracker = ReadingTracker()
        assertFalse(tracker.onSettle(20_000, saved, viewport, 0))
        // Brief pause, then one small scroll: still drifted.
        assertFalse(tracker.onSettle(20_400, saved, viewport, 3_000))
        assertTrue(tracker.drifting)
    }

    @Test
    fun quickStreakWithoutDwellDoesNotAdopt() {
        val tracker = ReadingTracker()
        assertFalse(tracker.onSettle(20_000, saved, viewport, 0))
        assertFalse(tracker.onSettle(20_400, saved, viewport, 1_000))
        assertFalse(tracker.onSettle(20_800, saved, viewport, 2_000))
        assertFalse(tracker.onSettle(21_200, saved, viewport, 3_000))
        assertTrue(tracker.drifting)
    }

    @Test
    fun sustainedReadingAtNewSpotAdopts() {
        val tracker = ReadingTracker()
        assertFalse(tracker.onSettle(20_000, saved, viewport, 0))
        assertFalse(tracker.onSettle(20_500, saved, viewport, 5_000))
        assertFalse(tracker.onSettle(21_000, saved, viewport, 15_000))
        // Third reading-like settle, 25s into the streak, 1.6 viewports of travel.
        assertTrue(tracker.onSettle(21_600, saved, viewport, 30_000))
        assertFalse(tracker.drifting)
    }

    @Test
    fun fastScanningWhileDriftedResetsStreak() {
        val tracker = ReadingTracker()
        assertFalse(tracker.onSettle(20_000, saved, viewport, 0))
        assertFalse(tracker.onSettle(20_500, saved, viewport, 10_000))
        // Another big flick breaks the streak.
        assertFalse(tracker.onSettle(26_000, saved, viewport, 12_000))
        assertFalse(tracker.onSettle(26_500, saved, viewport, 40_000))
        assertFalse(tracker.onSettle(27_000, saved, viewport, 50_000))
        assertTrue(tracker.drifting)
    }

    @Test
    fun returningNearSavedPositionResumesSaving() {
        val tracker = ReadingTracker()
        assertFalse(tracker.onSettle(20_000, saved, viewport, 0))
        assertTrue(tracker.onSettle(6_000, saved, viewport, 5_000))
        assertFalse(tracker.drifting)
    }

    @Test
    fun explicitPositionSetClearsDrift() {
        val tracker = ReadingTracker()
        assertFalse(tracker.onSettle(20_000, saved, viewport, 0))
        assertTrue(tracker.drifting)
        tracker.onPositionSet()
        assertFalse(tracker.drifting)
        // Next settle near the newly set position saves normally.
        assertTrue(tracker.onSettle(20_200, 20_000, viewport, 1_000))
    }

    @Test
    fun degenerateViewportFallsBackToAlwaysSaving() {
        val tracker = ReadingTracker()
        assertTrue(tracker.onSettle(20_000, 0, 0, 0))
        assertFalse(tracker.drifting)
    }
}
