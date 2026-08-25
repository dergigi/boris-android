package org.dergigi.boris.ui.reader

import kotlin.math.abs

/**
 * Distinguishes the saved reading position from exploratory scrolling
 * (issue #86). Settled scroll positions near the saved position count as
 * reading and advance it. A jump beyond [jumpViewports] enters a drifted
 * state where the saved position is frozen until one of:
 *
 * - the viewport returns near the saved position,
 * - the position is set explicitly ([onPositionSet]): jump-back, the
 *   "set progress here" selection action, or a progress reset,
 * - the user demonstrably resumes reading at the new spot: at least
 *   [adoptStreak] consecutive reading-like settles spanning [adoptMinMs]
 *   and at least [adoptMinTravelViewports] of net movement.
 *
 * A quick jump plus a short dwell (or a single small scroll) never adopts
 * the new location. All inputs are pixel offsets so the logic stays pure
 * and testable.
 */
class ReadingTracker(
    private val jumpViewports: Float = 3f,
    private val readViewports: Float = 1.2f,
    private val adoptStreak: Int = 3,
    private val adoptMinMs: Long = 20_000,
    private val adoptMinTravelViewports: Float = 0.5f,
) {
    var drifting: Boolean = false
        private set

    private var lastSettle: Int? = null
    private var streak = 0
    private var streakStartOffset = 0
    private var streakStartTime = 0L

    /**
     * Classifies a settled scroll position. Returns true when the position
     * should be saved as the reading position.
     */
    fun onSettle(offset: Int, savedOffset: Int, viewportHeight: Int, nowMs: Long): Boolean {
        if (viewportHeight <= 0) return true
        val previous = lastSettle
        lastSettle = offset
        val nearSaved = abs(offset - savedOffset) <= viewportHeight * jumpViewports
        if (!drifting) {
            if (nearSaved) return true
            drifting = true
            streak = 0
            return false
        }
        if (nearSaved) {
            clearDrift()
            return true
        }
        val local = offset - (previous ?: offset)
        if (local != 0 && abs(local) <= viewportHeight * readViewports) {
            if (streak == 0) {
                streakStartOffset = previous ?: offset
                streakStartTime = nowMs
            }
            streak++
        } else {
            streak = 0
        }
        val sustained = streak >= adoptStreak &&
            nowMs - streakStartTime >= adoptMinMs &&
            abs(offset - streakStartOffset) >= viewportHeight * adoptMinTravelViewports
        if (sustained) {
            clearDrift()
            return true
        }
        return false
    }

    /** The reading position was set explicitly; stop drifting. */
    fun onPositionSet() {
        clearDrift()
        lastSettle = null
    }

    private fun clearDrift() {
        drifting = false
        streak = 0
    }
}
