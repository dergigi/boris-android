package org.dergigi.boris.ui.reader

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
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
    var ttsStartIndex by mutableStateOf<Int?>(null)
        private set

    private var frozenMin = 0
    private var frozenMax = 0

    val hasSelection: Boolean get() = owner != null && range.min != range.max
    val toolbarReady: Boolean
        get() = hasSelection && (toolbarRect.width > 1f || toolbarRect.height > 1f)
    val selectedText: String
        get() {
            if (!hasSelection) return ""
            val a = range.min.coerceIn(0, text.length)
            val b = range.max.coerceIn(0, text.length)
            return if (b > a) text.substring(a, b) else ""
        }

    fun owns(id: Any): Boolean = owner === id && hasSelection

    fun begin(id: Any, value: String, word: TextRange, ttsIndex: Int? = null) {
        owner = id
        text = value
        range = word
        ttsStartIndex = ttsIndex
        frozenMin = word.min
        frozenMax = word.max
        toolbarRect = Rect.Zero
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

    fun selectAll(id: Any, value: String, ttsIndex: Int? = null) {
        owner = id
        text = value
        range = TextRange(0, value.length)
        ttsStartIndex = ttsIndex
        frozenMin = 0
        frozenMax = value.length
    }

    fun clear() {
        owner = null
        text = ""
        range = TextRange.Zero
        ttsStartIndex = null
        toolbarRect = Rect.Zero
    }

    fun updateToolbar(layout: TextLayoutResult, coords: LayoutCoordinates) {
        if (!hasSelection) {
            toolbarRect = Rect.Zero
            return
        }
        if (!coords.isAttached) return
        val boxes = HighlightMarks.highlightRects(layout, range.min, range.max)
        val box = boxes.firstOrNull() ?: return
        val topLeft = coords.localToWindow(Offset(box.left, box.top))
        val bottomRight = coords.localToWindow(Offset(box.right, box.bottom))
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
    onTap: ((Offset) -> Boolean)? = null,
    ttsStartIndex: Int? = null,
): Modifier = composed {
    val colors = LocalTextSelectionColors.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val viewConfig = LocalViewConfiguration.current
    val layoutRef = rememberUpdatedState(layout)
    val coordsRef = rememberUpdatedState(coordinates)
    val textRef = rememberUpdatedState(text)
    val onTapRef = rememberUpdatedState(onTap)
    val ttsStartIndexRef = rememberUpdatedState(ttsStartIndex)

    SideEffect {
        val laid = layout
        val coords = coordinates
        if (laid != null && coords != null && state.owns(owner)) {
            state.updateToolbar(laid, coords)
        }
    }

    onGloballyPositioned { coords ->
        onCoordinates(coords)
        val current = layoutRef.value
        if (state.owns(owner) && current != null) state.updateToolbar(current, coords)
    }
        .drawWithContent {
            val current = layoutRef.value
            if (state.owns(owner) && current != null) {
                drawSelection(current, state.range, colors.backgroundColor)
            }
            drawContent()
            if (state.owns(owner) && current != null) {
                drawHandles(current, state.range, colors.handleColor)
            }
        }
        .pointerInput(owner) {
            val touchSlop = viewConfig.touchSlop
            val longPressTimeout = viewConfig.longPressTimeoutMillis
            val handleSlop = 24.dp.toPx()
            while (true) {
                awaitPointerEventScope {
                    handleReaderGesture(
                        owner = owner,
                        text = { textRef.value },
                        state = state,
                        layout = { layoutRef.value },
                        coordinates = { coordsRef.value },
                        view = view,
                        haptic = haptic,
                        touchSlop = touchSlop,
                        longPressTimeout = longPressTimeout,
                        handleSlop = handleSlop,
                        onTap = { onTapRef.value?.invoke(it) == true },
                        ttsStartIndex = { ttsStartIndexRef.value },
                    )
                }
            }
        }
}

private suspend fun AwaitPointerEventScope.handleReaderGesture(
    owner: Any,
    text: () -> String,
    state: ReaderSelectionState,
    layout: () -> TextLayoutResult?,
    coordinates: () -> LayoutCoordinates?,
    view: View,
    haptic: HapticFeedback,
    touchSlop: Float,
    longPressTimeout: Long,
    handleSlop: Float,
    onTap: (Offset) -> Boolean,
    ttsStartIndex: () -> Int?,
) {
    val pass = PointerEventPass.Initial
    val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
    val currentLayout = layout() ?: return

    if (state.owns(owner)) {
        val startHandle = handleCenter(currentLayout, state.range.min, start = true)
        val endHandle = handleCenter(currentLayout, state.range.max, start = false)
        val movingMin = (down.position - startHandle).getDistance() <= handleSlop
        val movingMax = (down.position - endHandle).getDistance() <= handleSlop
        if (movingMin || movingMax) {
            down.consume()
            dragSelectionBound(down.id, movingMin, state, layout, coordinates, pass)
            return
        }
    }

    val reachedLongPress = withTimeoutOrNull(longPressTimeout) {
        while (true) {
            val event = awaitPointerEvent(pass)
            val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
            if (!change.pressed) return@withTimeoutOrNull false
            if ((change.position - down.position).getDistance() > touchSlop) {
                return@withTimeoutOrNull false
            }
        }
        @Suppress("UNREACHABLE_CODE")
        true
    }

    if (reachedLongPress == null) {
        val laid = layout() ?: return
        val change = currentEvent.changes.firstOrNull { it.id == down.id } ?: return
        if (!change.pressed) return
        change.consume()
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val index = JustifiedLayout.offsetAt(laid, change.position)
        state.begin(owner, text(), laid.getWordBoundary(index), ttsStartIndex())
        coordinates()?.let { state.updateToolbar(laid, it) }
        while (true) {
            val event = awaitPointerEvent(pass)
            val drag = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!drag.pressed) break
            drag.consume()
            val next = layout() ?: break
            state.extendTo(JustifiedLayout.offsetAt(next, drag.position))
            coordinates()?.let { state.updateToolbar(next, it) }
        }
        return
    }

    val change = currentEvent.changes.firstOrNull { it.id == down.id } ?: return
    if (!change.pressed && (change.position - down.position).getDistance() <= touchSlop) {
        if (state.hasSelection) {
            state.clear()
        } else if (onTap(down.position)) {
            change.consume()
        }
    }
}

private suspend fun AwaitPointerEventScope.dragSelectionBound(
    pointerId: PointerId,
    movingMin: Boolean,
    state: ReaderSelectionState,
    layout: () -> TextLayoutResult?,
    coordinates: () -> LayoutCoordinates?,
    pass: PointerEventPass,
) {
    while (true) {
        val event = awaitPointerEvent(pass)
        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
        if (!change.pressed) break
        change.consume()
        val current = layout() ?: break
        state.moveBound(movingMin, JustifiedLayout.offsetAt(current, change.position))
        coordinates()?.let { state.updateToolbar(current, it) }
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
