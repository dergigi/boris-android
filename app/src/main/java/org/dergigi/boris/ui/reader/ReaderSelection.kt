package org.dergigi.boris.ui.reader

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

@Stable
class ReaderSelectionState {
    var owner by mutableStateOf<Any?>(null)
        private set
    var hasSelection by mutableStateOf(false)
        private set
    var text by mutableStateOf("")
        private set
    var range by mutableStateOf(TextRange.Zero)
        private set
    var toolbarRect by mutableStateOf(Rect.Zero)
        private set
    var loupeCenter by mutableStateOf(Offset.Unspecified)
        private set
    var ttsStartIndex by mutableStateOf<Int?>(null)
        private set

    private var selectedRanges by mutableStateOf<Map<Any, TextRange>>(emptyMap())
    private var blocks = linkedMapOf<Any, SelectableBlock>()
    private var anchorOwner: Any? = null
    private var frozenMin = 0
    private var frozenMax = 0
    private var startOwner: Any? = null
    private var endOwner: Any? = null
    private var loupeOwner: Any? = null

    val toolbarReady: Boolean
        get() = hasSelection && (toolbarRect.width > 1f || toolbarRect.height > 1f)
    val selectedText: String
        get() {
            if (!hasSelection) return ""
            val parts = selectedPieces()
            if (parts.isNotEmpty()) return parts.joinToString("\n")
            val a = range.min.coerceIn(0, text.length)
            val b = range.max.coerceIn(0, text.length)
            return if (b > a) text.substring(a, b) else ""
        }
    val anchorText: String
        get() = startOwner?.let(::blockText).orEmpty().ifBlank { text }
    val anchorOffset: Int
        get() = startOwner?.let { selectedRanges[it]?.min } ?: range.min

    fun owns(id: Any): Boolean = hasSelection && selectedRanges.containsKey(id)

    fun rangeFor(id: Any): TextRange? = selectedRanges[id]

    fun handleOffset(id: Any, start: Boolean): Int? {
        val current = selectedRanges[id] ?: return null
        return when {
            start && startOwner === id -> current.min
            !start && endOwner === id -> current.max
            else -> null
        }
    }

    fun showsLoupe(id: Any): Boolean =
        hasSelection && loupeOwner === id && loupeCenter != Offset.Unspecified

    fun updateBlock(
        id: Any,
        value: String,
        layout: TextLayoutResult?,
        coordinates: LayoutCoordinates?,
        ttsIndex: Int?,
    ) {
        if (layout == null || coordinates == null || !coordinates.isAttached) return
        blocks[id] = SelectableBlock(id, value, layout, coordinates, ttsIndex)
    }

    fun unregister(id: Any) {
        blocks.remove(id)
        if (owner === id || selectedRanges.containsKey(id)) clear()
    }

    fun begin(id: Any, value: String, word: TextRange, ttsIndex: Int? = null) {
        owner = id
        anchorOwner = id
        text = value
        range = word
        ttsStartIndex = ttsIndex
        frozenMin = word.min
        frozenMax = word.max
        startOwner = id
        endOwner = id
        selectedRanges = if (word.min != word.max) mapOf(id to word) else emptyMap()
        toolbarRect = Rect.Zero
        loupeCenter = Offset.Unspecified
        loupeOwner = null
        hasSelection = word.min != word.max
    }

    fun extendTo(offset: Int) {
        val id = anchorOwner ?: owner ?: return
        extendTo(id, offset)
    }

    fun extendTo(id: Any, offset: Int) {
        val anchor = anchorOwner ?: owner ?: return
        val clamped = offset.coerceIn(0, blockText(id).length)
        if (id === anchor) {
            val next = when {
                clamped <= frozenMin -> TextRange(clamped, frozenMax)
                clamped >= frozenMax -> TextRange(frozenMin, clamped)
                else -> TextRange(frozenMin, frozenMax)
            }
            applySingleRange(anchor, next)
            return
        }
        if (comesBefore(id, anchor)) {
            applyRangeBetween(id, clamped, anchor, frozenMax)
        } else {
            applyRangeBetween(anchor, frozenMin, id, clamped)
        }
    }

    fun moveBound(movingMin: Boolean, offset: Int) {
        val id = if (movingMin) startOwner else endOwner
        if (id != null) moveBound(movingMin, id, offset)
    }

    fun moveBound(movingMin: Boolean, id: Any, offset: Int) {
        if (owner == null) return
        val currentStartOwner = startOwner ?: return
        val currentEndOwner = endOwner ?: return
        val currentStart = selectedRanges[currentStartOwner]?.min ?: return
        val currentEnd = selectedRanges[currentEndOwner]?.max ?: return
        val clamped = offset.coerceIn(0, blockText(id).length)
        if (movingMin) {
            applyRangeBetween(id, clamped, currentEndOwner, currentEnd)
        } else {
            applyRangeBetween(currentStartOwner, currentStart, id, clamped)
        }
    }

    fun selectAll(id: Any, value: String, ttsIndex: Int? = null) {
        val actual = blockText(id).ifBlank { value }
        owner = id
        anchorOwner = id
        text = actual
        range = TextRange(0, actual.length)
        ttsStartIndex = ttsIndex
        frozenMin = 0
        frozenMax = actual.length
        startOwner = id
        endOwner = id
        selectedRanges = if (actual.isNotEmpty()) mapOf(id to TextRange(0, actual.length)) else emptyMap()
        toolbarRect = Rect.Zero
        loupeCenter = Offset.Unspecified
        loupeOwner = null
        hasSelection = actual.isNotEmpty()
    }

    fun hideToolbar() {
        toolbarRect = Rect.Zero
    }

    fun showLoupe(center: Offset) {
        loupeCenter = center
        loupeOwner = owner
    }

    fun showLoupe(id: Any, center: Offset) {
        loupeCenter = center
        loupeOwner = id
    }

    fun hideLoupe() {
        loupeCenter = Offset.Unspecified
        loupeOwner = null
    }

    fun clear() {
        owner = null
        anchorOwner = null
        text = ""
        range = TextRange.Zero
        ttsStartIndex = null
        selectedRanges = emptyMap()
        startOwner = null
        endOwner = null
        toolbarRect = Rect.Zero
        loupeCenter = Offset.Unspecified
        loupeOwner = null
        hasSelection = false
    }

    fun updateToolbar(layout: TextLayoutResult, coords: LayoutCoordinates) {
        val id = selectedRanges.entries.firstOrNull { it.value == range }?.key
        if (id != null) {
            updateToolbar(id, layout, coords)
            return
        }
        updateToolbar()
    }

    fun updateToolbar() {
        val id = endOwner ?: startOwner ?: return
        val block = blocks[id] ?: return
        updateToolbar(id, block.layout, block.coordinates)
    }

    private fun updateToolbar(id: Any, layout: TextLayoutResult, coords: LayoutCoordinates) {
        if (!hasSelection) {
            toolbarRect = Rect.Zero
            return
        }
        if (!coords.isAttached) return
        val current = selectedRanges[id] ?: return
        val boxes = HighlightMarks.highlightRects(layout, current.min, current.max)
        val box = boxes.firstOrNull() ?: return
        val topLeft = coords.localToWindow(Offset(box.left, box.top))
        val bottomRight = coords.localToWindow(Offset(box.right, box.bottom))
        toolbarRect = Rect(topLeft, bottomRight)
    }

    fun targetAtWindow(position: Offset): ReaderSelectionTarget? {
        val ordered = orderedBlocks()
        if (ordered.isEmpty()) return null
        val containing = ordered.firstOrNull { block ->
            val local = block.coordinates.windowToLocal(position)
            local.y >= 0f && local.y <= block.layout.size.height
        }
        val block = containing ?: ordered.minBy { block ->
            val local = block.coordinates.windowToLocal(position)
            when {
                local.y < 0f -> abs(local.y)
                local.y > block.layout.size.height -> abs(local.y - block.layout.size.height)
                else -> 0f
            }
        }
        val local = block.coordinates.windowToLocal(position)
        val offset = JustifiedLayout.offsetAt(block.layout, local)
        return ReaderSelectionTarget(block.owner, offset, block.layout)
    }

    private fun applySingleRange(id: Any, next: TextRange) {
        val block = blocks[id]
        owner = owner ?: id
        startOwner = id
        endOwner = id
        selectedRanges = if (next.min != next.max) mapOf(id to next) else emptyMap()
        range = next
        text = block?.text ?: text
        if (block != null) ttsStartIndex = block.ttsStartIndex
        hasSelection = next.min != next.max
        if (!hasSelection) toolbarRect = Rect.Zero
    }

    private fun applyRangeBetween(firstOwner: Any, firstOffset: Int, lastOwner: Any, lastOffset: Int) {
        val ordered = orderedBlocks()
        val firstIndex = ordered.indexOfFirst { it.owner === firstOwner }
        val lastIndex = ordered.indexOfFirst { it.owner === lastOwner }
        if (firstIndex < 0 || lastIndex < 0) {
            if (firstOwner === lastOwner) applySingleRange(firstOwner, TextRange(firstOffset, lastOffset))
            return
        }
        val startIndex = minOf(firstIndex, lastIndex)
        val endIndex = maxOf(firstIndex, lastIndex)
        val sameBlock = firstIndex == lastIndex
        val startOffset = when {
            sameBlock -> minOf(firstOffset, lastOffset)
            firstIndex <= lastIndex -> firstOffset
            else -> lastOffset
        }
        val endOffset = when {
            sameBlock -> maxOf(firstOffset, lastOffset)
            firstIndex <= lastIndex -> lastOffset
            else -> firstOffset
        }
        val next = linkedMapOf<Any, TextRange>()
        for (index in startIndex..endIndex) {
            val block = ordered[index]
            val from = if (index == startIndex) startOffset.coerceIn(0, block.text.length) else 0
            val to = if (index == endIndex) endOffset.coerceIn(0, block.text.length) else block.text.length
            if (to > from) next[block.owner] = TextRange(from, to)
        }
        startOwner = ordered[startIndex].owner
        endOwner = ordered[endIndex].owner
        selectedRanges = next
        val pieces = selectedPieces()
        text = if (next.size == 1) {
            val only = next.keys.first()
            blockText(only)
        } else {
            pieces.joinToString("\n")
        }
        range = if (next.size == 1) next.values.first() else TextRange(0, text.length)
        ttsStartIndex = ordered[startIndex].ttsStartIndex
        hasSelection = next.isNotEmpty()
        if (!hasSelection) toolbarRect = Rect.Zero
    }

    private fun selectedPieces(): List<String> =
        orderedBlocks().mapNotNull { block ->
            val current = selectedRanges[block.owner] ?: return@mapNotNull null
            val start = current.min.coerceIn(0, block.text.length)
            val end = current.max.coerceIn(0, block.text.length)
            block.text.substring(start, end).takeIf { it.isNotBlank() }
        }

    private fun orderedBlocks(): List<SelectableBlock> =
        blocks.values
            .filter { it.coordinates.isAttached }
            .sortedWith(
                compareBy<SelectableBlock> { it.coordinates.localToWindow(Offset.Zero).y }
                    .thenBy { it.coordinates.localToWindow(Offset.Zero).x },
            )

    private fun comesBefore(left: Any, right: Any): Boolean {
        val ordered = orderedBlocks()
        val a = ordered.indexOfFirst { it.owner === left }
        val b = ordered.indexOfFirst { it.owner === right }
        return a >= 0 && b >= 0 && a < b
    }

    private fun blockText(id: Any): String = blocks[id]?.text ?: if (owner === id) text else ""
}

data class ReaderSelectionTarget(
    val owner: Any,
    val offset: Int,
    val layout: TextLayoutResult,
)

private data class SelectableBlock(
    val owner: Any,
    val text: String,
    val layout: TextLayoutResult,
    val coordinates: LayoutCoordinates,
    val ttsStartIndex: Int?,
)

@Composable
fun SelectionBackHandler(state: ReaderSelectionState) {
    BackHandler(enabled = state.hasSelection) { state.clear() }
}

fun Modifier.readerSelectable(
    owner: Any,
    text: String,
    layout: TextLayoutResult?,
    coordinates: LayoutCoordinates?,
    state: ReaderSelectionState,
    onCoordinates: (LayoutCoordinates) -> Unit,
    onTap: ((Offset) -> Boolean)? = null,
    onLongPress: ((Offset, LayoutCoordinates) -> Boolean)? = null,
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
    val onLongPressRef = rememberUpdatedState(onLongPress)
    val ttsStartIndexRef = rememberUpdatedState(ttsStartIndex)
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.getTop(density)
    val minWindowY = statusTop + with(density) { (TOP_BAR_CLEARANCE + LOUPE_HEIGHT / 2).toPx() }
    val liftPx = with(density) { LOUPE_LIFT.toPx() }

    SideEffect {
        state.updateBlock(owner, textRef.value, layoutRef.value, coordsRef.value, ttsStartIndexRef.value)
    }
    DisposableEffect(owner) {
        onDispose { state.unregister(owner) }
    }

    onGloballyPositioned { coords ->
        onCoordinates(coords)
        state.updateBlock(owner, textRef.value, layoutRef.value, coords, ttsStartIndexRef.value)
        val current = layoutRef.value
        if (state.owns(owner) && state.toolbarReady && current != null) {
            state.updateToolbar()
        }
    }
        .then(
            if (state.showsLoupe(owner)) {
                Modifier.magnifier(
                    sourceCenter = { state.loupeCenter },
                    magnifierCenter = {
                        val source = state.loupeCenter
                        if (source == Offset.Unspecified) {
                            Offset.Unspecified
                        } else {
                            val coords = coordsRef.value
                            val window = if (coords != null && coords.isAttached) {
                                coords.localToWindow(source)
                            } else {
                                source
                            }
                            loupeDisplayCenter(source, window, liftPx, minWindowY)
                        }
                    },
                    zoom = LOUPE_ZOOM,
                    size = DpSize(LOUPE_WIDTH, LOUPE_HEIGHT),
                    cornerRadius = LOUPE_HEIGHT / 2,
                    elevation = 8.dp,
                )
            } else {
                Modifier
            },
        )
        .drawWithContent {
            val current = layoutRef.value
            val selectedRange = state.rangeFor(owner)
            if (selectedRange != null && current != null) {
                drawSelection(current, selectedRange, colors.backgroundColor)
            }
            drawContent()
            if (state.owns(owner) && current != null) {
                drawHandles(current, owner, state, colors.handleColor)
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
                        onLongPress = { position, coords ->
                            onLongPressRef.value?.invoke(position, coords) == true
                        },
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
    onLongPress: (Offset, LayoutCoordinates) -> Boolean,
    ttsStartIndex: () -> Int?,
) {
    val pass = PointerEventPass.Initial
    val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
    val currentLayout = layout() ?: return

    if (state.owns(owner)) {
        val startHandle = state.handleOffset(owner, start = true)
            ?.let { handleCenter(currentLayout, it, start = true) }
        val endHandle = state.handleOffset(owner, start = false)
            ?.let { handleCenter(currentLayout, it, start = false) }
        val movingMin = startHandle != null && (down.position - startHandle).getDistance() <= handleSlop
        val movingMax = endHandle != null && (down.position - endHandle).getDistance() <= handleSlop
        if (movingMin || movingMax) {
            down.consume()
            state.hideToolbar()
            val bound = if (movingMin) {
                state.handleOffset(owner, start = true) ?: 0
            } else {
                state.handleOffset(owner, start = false) ?: 0
            }
            state.showLoupe(owner, loupeSource(currentLayout, bound))
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
        val coords = coordinates()
        if (coords != null && onLongPress(change.position, coords)) return
        state.begin(owner, text(), laid.getWordBoundary(index), ttsStartIndex())
        state.showLoupe(owner, loupeSource(laid, index))
        while (true) {
            val event = awaitPointerEvent(pass)
            val drag = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!drag.pressed) break
            drag.consume()
            val next = layout() ?: break
            val target = coordinates()
                ?.localToWindow(drag.position)
                ?.let(state::targetAtWindow)
            if (target != null) {
                state.extendTo(target.owner, target.offset)
                state.showLoupe(target.owner, loupeSource(target.layout, target.offset))
            } else {
                val nextOffset = JustifiedLayout.offsetAt(next, drag.position)
                state.extendTo(nextOffset)
                state.showLoupe(owner, loupeSource(next, nextOffset))
            }
        }
        showToolbar(state, layout, coordinates)
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
        val target = coordinates()
            ?.localToWindow(change.position)
            ?.let(state::targetAtWindow)
        if (target != null) {
            state.moveBound(movingMin, target.owner, target.offset)
            val offset = if (movingMin) {
                state.handleOffset(target.owner, start = true) ?: target.offset
            } else {
                state.handleOffset(target.owner, start = false) ?: target.offset
            }
            state.showLoupe(target.owner, loupeSource(target.layout, offset))
        } else {
            val offset = JustifiedLayout.offsetAt(current, change.position)
            state.moveBound(movingMin, offset)
            state.showLoupe(loupeSource(current, if (movingMin) state.range.min else state.range.max))
        }
    }
    showToolbar(state, layout, coordinates)
}

private fun showToolbar(
    state: ReaderSelectionState,
    layout: () -> TextLayoutResult?,
    coordinates: () -> LayoutCoordinates?,
) {
    state.hideLoupe()
    state.updateToolbar()
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
    owner: Any,
    state: ReaderSelectionState,
    color: Color,
) {
    val start = state.handleOffset(owner, start = true)
    val end = state.handleOffset(owner, start = false)
    if (start == null && end == null) return
    val radius = 6.dp.toPx()
    if (start != null) drawCircle(color, radius, handleCenter(layout, start, start = true))
    if (end != null) drawCircle(color, radius, handleCenter(layout, end, start = false))
}

private fun handleCenter(layout: TextLayoutResult, offset: Int, start: Boolean): Offset {
    val line = caretLine(layout, offset, start)
    val x = JustifiedLayout.visualCursor(layout, offset, line)
    val y = if (start) layout.getLineTop(line) else layout.getLineBottom(line)
    return Offset(x, y)
}

internal fun loupeDisplayCenter(
    source: Offset,
    sourceWindow: Offset,
    liftPx: Float,
    minWindowY: Float,
): Offset {
    if (source == Offset.Unspecified) return Offset.Unspecified
    val desiredWindowY = (sourceWindow.y - liftPx).coerceAtLeast(minWindowY)
    return Offset(source.x, source.y - (sourceWindow.y - desiredWindowY))
}

internal fun loupeSource(layout: TextLayoutResult, offset: Int): Offset {
    val line = caretLine(layout, offset, start = offset == 0)
    val x = JustifiedLayout.visualCursor(layout, offset, line)
    val y = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f
    return Offset(x, y)
}

private fun caretLine(layout: TextLayoutResult, offset: Int, start: Boolean): Int {
    val last = (layout.layoutInput.text.length - 1).coerceAtLeast(0)
    return layout.getLineForOffset(
        if (start) offset.coerceIn(0, last) else (offset - 1).coerceIn(0, last),
    )
}

private val LOUPE_WIDTH = 140.dp
private val LOUPE_HEIGHT = 48.dp
private val LOUPE_LIFT = 72.dp
private val TOP_BAR_CLEARANCE = 56.dp
private const val LOUPE_ZOOM = 1.75f
