package org.dergigi.boris.ui.reader

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Stable
class ReaderSelectionState {
    var owner by mutableStateOf<Any?>(null)
        private set
    var text by mutableStateOf("")
        private set
    var range by mutableStateOf(TextRange.Zero)
        private set
    var toolbarRect by mutableStateOf(Rect.Zero)
        private set

    private var frozenMin = 0
    private var frozenMax = 0

    val hasSelection: Boolean get() = owner != null && range.min != range.max
    val selectedText: String
        get() {
            if (!hasSelection) return ""
            val a = range.min.coerceIn(0, text.length)
            val b = range.max.coerceIn(0, text.length)
            return if (b > a) text.substring(a, b) else ""
        }

    fun owns(id: Any): Boolean = owner === id && hasSelection

    fun begin(id: Any, value: String, word: TextRange) {
        owner = id
        text = value
        range = word
        frozenMin = word.min
        frozenMax = word.max
    }

    fun extendTo(offset: Int) {
        if (owner == null) return
        val clamped = offset.coerceIn(0, text.length)
        range = when {
            clamped <= frozenMin -> TextRange(clamped, frozenMax)
            clamped >= frozenMax -> TextRange(frozenMin, clamped)
            else -> TextRange(frozenMin, frozenMax)
        }
    }

    fun moveBound(movingMin: Boolean, offset: Int) {
        if (owner == null) return
        val clamped = offset.coerceIn(0, text.length)
        range = if (movingMin) TextRange(clamped, range.max) else TextRange(range.min, clamped)
    }

    fun selectAll(id: Any, value: String) {
        owner = id
        text = value
        range = TextRange(0, value.length)
    }

    fun clear() {
        owner = null
        text = ""
        range = TextRange.Zero
        toolbarRect = Rect.Zero
    }

    fun updateToolbar(layout: TextLayoutResult, coords: LayoutCoordinates) {
        if (!hasSelection) {
            toolbarRect = Rect.Zero
            return
        }
        val boxes = HighlightMarks.highlightRects(layout, range.min, range.max)
        val box = boxes.firstOrNull() ?: return
        val topLeft = coords.localToRoot(Offset(box.left, box.top))
        val bottomRight = coords.localToRoot(Offset(box.right, box.bottom))
        toolbarRect = Rect(topLeft, bottomRight)
    }
}

fun Modifier.readerSelectable(
    owner: Any,
    text: String,
    layout: TextLayoutResult?,
    coordinates: LayoutCoordinates?,
    state: ReaderSelectionState,
    onCoordinates: (LayoutCoordinates) -> Unit,
): Modifier = composed {
    val colors = LocalTextSelectionColors.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    onGloballyPositioned { coords ->
        onCoordinates(coords)
        if (state.owns(owner) && layout != null) state.updateToolbar(layout, coords)
    }
        .drawWithContent {
            if (state.owns(owner) && layout != null) {
                drawSelection(layout, state.range, colors.backgroundColor)
            }
            drawContent()
            if (state.owns(owner) && layout != null) {
                drawHandles(layout, state.range, colors.handleColor)
            }
        }
        .pointerInput(owner, text, layout, state.hasSelection && state.owner === owner) {
            if (layout == null) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!state.owns(owner)) return@awaitEachGesture
                val slop = 24.dp.toPx()
                val startHandle = handleCenter(layout, state.range.min, start = true)
                val endHandle = handleCenter(layout, state.range.max, start = false)
                val movingMin = (down.position - startHandle).getDistance() <= slop
                val movingMax = (down.position - endHandle).getDistance() <= slop
                if (!movingMin && !movingMax) return@awaitEachGesture
                down.consume()
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
                    change.consume()
                    state.moveBound(movingMin, JustifiedLayout.offsetAt(layout, change.position))
                    coordinates?.let { state.updateToolbar(layout, it) }
                }
            }
        }
        .pointerInput(owner, text, layout) {
            if (layout == null) return@pointerInput
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val index = JustifiedLayout.offsetAt(layout, offset)
                    state.begin(owner, text, layout.getWordBoundary(index))
                    coordinates?.let { state.updateToolbar(layout, it) }
                },
                onDrag = { change, _ ->
                    change.consume()
                    state.extendTo(JustifiedLayout.offsetAt(layout, change.position))
                    coordinates?.let { state.updateToolbar(layout, it) }
                },
            )
        }
        .pointerInput(owner, state.hasSelection && state.owner === owner) {
            if (!state.owns(owner)) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val slop = 24.dp.toPx()
                if (layout != null) {
                    val startHandle = handleCenter(layout, state.range.min, start = true)
                    val endHandle = handleCenter(layout, state.range.max, start = false)
                    if ((down.position - startHandle).getDistance() <= slop) return@awaitEachGesture
                    if ((down.position - endHandle).getDistance() <= slop) return@awaitEachGesture
                }
                val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                if ((up.position - down.position).getDistance() < slop) {
                    state.clear()
                }
            }
        }
}

private fun DrawScope.drawSelection(
    layout: TextLayoutResult,
    range: TextRange,
    fill: Color,
) {
    if (range.min == range.max) return
    HighlightMarks.highlightRects(layout, range.min, range.max).forEach { box ->
        drawRect(
            color = fill,
            topLeft = Offset(box.left, box.top),
            size = Size(max(box.width, 1f), box.height),
        )
    }
}

private fun DrawScope.drawHandles(
    layout: TextLayoutResult,
    range: TextRange,
    color: Color,
) {
    if (range.min == range.max) return
    val radius = 6.dp.toPx()
    drawCircle(color, radius, handleCenter(layout, range.min, start = true))
    drawCircle(color, radius, handleCenter(layout, range.max, start = false))
}

private fun handleCenter(layout: TextLayoutResult, offset: Int, start: Boolean): Offset {
    val last = (layout.layoutInput.text.length - 1).coerceAtLeast(0)
    val line = layout.getLineForOffset(
        if (start) offset.coerceIn(0, last) else (offset - 1).coerceIn(0, last),
    )
    val x = JustifiedLayout.visualCursor(layout, offset, line)
    val y = if (start) layout.getLineTop(line) else layout.getLineBottom(line)
    return Offset(x, y)
}
