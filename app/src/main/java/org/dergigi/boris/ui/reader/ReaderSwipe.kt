package org.dergigi.boris.ui.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

internal enum class ReaderSwipeTarget {
    Contents,
    Highlights,
}

internal object ReaderSwipe {
    private const val AXIS_LOCK_RATIO = 1.4f

    fun target(totalX: Float, totalY: Float, thresholdPx: Float): ReaderSwipeTarget? {
        val horizontal = abs(totalX)
        val vertical = abs(totalY)
        if (horizontal < thresholdPx) return null
        if (horizontal < vertical * AXIS_LOCK_RATIO) return null
        return if (totalX < 0f) ReaderSwipeTarget.Highlights else ReaderSwipeTarget.Contents
    }
}

internal fun Modifier.readerSwipeGestures(
    enabled: Boolean,
    thresholdPx: Float,
    onSwipe: (ReaderSwipeTarget) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(thresholdPx, onSwipe) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var total = Offset.Zero
            var cancelled = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val active = event.changes.firstOrNull { it.id == down.id }
                if (event.changes.size > 1 || event.changes.any { it.isConsumed }) {
                    cancelled = true
                }
                if (!cancelled && active != null) {
                    total += active.position - active.previousPosition
                }
            } while (event.changes.any { it.pressed })

            if (!cancelled) {
                ReaderSwipe.target(total.x, total.y, thresholdPx)?.let(onSwipe)
            }
        }
    }
}
