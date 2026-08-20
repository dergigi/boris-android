package org.dergigi.boris.ui.reader

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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

    private val nodes = linkedMapOf<Any, SelectionNode>()
    private var startOwner: Any? = null
    private var startOffset = 0
    private var endOwner: Any? = null
    private var endOffset = 0
    private var frozenStartOwner: Any? = null
    private var frozenStartOffset = 0
    private var frozenEndOwner: Any? = null
    private var frozenEndOffset = 0
    private var loupeOwner: Any? = null

    val toolbarReady: Boolean
        get() = hasSelection && (toolbarRect.width > 1f || toolbarRect.height > 1f)
    val selectedText: String
        get() {
            if (!hasSelection) return ""
            val (from, to) = ordered() ?: return ""
            if (from.owner === to.owner) {
                val value = nodeText(from.owner)
                val a = from.offset.coerceIn(0, value.length)
                val b = to.offset.coerceIn(0, value.length)
                return if (b > a) value.substring(a, b) else ""
            }
            val list = nodes.values.toList()
            val first = indexOf(from.owner)
            val last = indexOf(to.owner)
            if (first < 0 || last < 0 || last < first) return ""
            val pieces = ArrayList<String>(last - first + 1)
            for (i in first..last) {
                val node = list[i]
                val chunk = when (i) {
                    first -> node.text.substring(from.offset.coerceIn(0, node.text.length))
                    last -> node.text.substring(0, to.offset.coerceIn(0, node.text.length))
                    else -> node.text
                }
                if (chunk.isNotEmpty()) pieces += chunk
            }
            return pieces.joinToString("\n\n")
        }

    fun attach(
        id: Any,
        value: String,
        layout: TextLayoutResult? = null,
        coordinates: LayoutCoordinates? = null,
    ) {
        val existing = nodes[id]
        nodes[id] = SelectionNode(
            id,
            value,
            layout ?: existing?.layout,
            coordinates ?: existing?.coordinates,
        )
        if (hasSelection && (id === startOwner || id === endOwner || id === owner)) {
            publishRange()
        }
    }

    fun detach(id: Any) {
        nodes.remove(id)
        if (id === startOwner || id === endOwner) clear()
    }

    fun owns(id: Any): Boolean = hasSelection && rangeIn(id) != null

    fun hasStartHandle(id: Any): Boolean = ordered()?.first?.owner === id

    fun hasEndHandle(id: Any): Boolean = ordered()?.second?.owner === id

    fun ownsLoupe(id: Any): Boolean =
        loupeOwner === id && loupeCenter != Offset.Unspecified

    fun rangeIn(id: Any): TextRange? {
        if (!hasSelection) return null
        val (from, to) = ordered() ?: return null
        val node = nodes[id]
        val first = indexOf(from.owner)
        val last = indexOf(to.owner)
        val index = indexOf(id)
        if (node != null && first >= 0 && last >= first && index in first..last) {
            val start = if (index == first) from.offset.coerceIn(0, node.text.length) else 0
            val end = if (index == last) to.offset.coerceIn(0, node.text.length) else node.text.length
            return if (end > start) TextRange(start, end) else null
        }
        if (id === owner && from.owner === to.owner && from.owner === id) {
            val value = nodeText(id)
            val start = from.offset.coerceIn(0, value.length)
            val end = to.offset.coerceIn(0, value.length)
            return if (end > start) TextRange(start, end) else null
        }
        return null
    }

    fun begin(id: Any, value: String, word: TextRange, ttsIndex: Int? = null) {
        ensureNode(id, value)
        owner = id
        text = value
        ttsStartIndex = ttsIndex
        setAnchors(id, word.min, id, word.max)
        freezeCurrent()
        toolbarRect = Rect.Zero
        hideLoupe()
        hasSelection = word.min != word.max
        publishRange()
    }

    fun extendTo(offset: Int) {
        val id = owner ?: return
        extendTo(id, offset)
    }

    fun extendTo(id: Any, offset: Int) {
        if (owner == null) return
        val clamped = offset.coerceIn(0, nodeText(id).length)
        val pos = rank(id, clamped)
        val frozenMin = rank(frozenStartOwner, frozenStartOffset)
        val frozenMax = rank(frozenEndOwner, frozenEndOffset)
        when {
            pos <= frozenMin -> setAnchors(id, clamped, frozenEndOwner, frozenEndOffset)
            pos >= frozenMax -> setAnchors(frozenStartOwner, frozenStartOffset, id, clamped)
            else -> setAnchors(frozenStartOwner, frozenStartOffset, frozenEndOwner, frozenEndOffset)
        }
        publishRange()
    }

    fun extendToWindow(windowPos: Offset) {
        val hit = hit(windowPos) ?: return
        extendTo(hit.owner, hit.offset)
    }

    fun moveBound(movingMin: Boolean, offset: Int) {
        val id = owner ?: return
        moveBound(movingMin, id, offset)
    }

    fun moveBound(movingMin: Boolean, id: Any, offset: Int) {
        if (owner == null) return
        val clamped = offset.coerceIn(0, nodeText(id).length)
        if (movingMin) {
            startOwner = id
            startOffset = clamped
        } else {
            endOwner = id
            endOffset = clamped
        }
        publishRange()
    }

    fun moveBoundToWindow(movingMin: Boolean, windowPos: Offset) {
        val hit = hit(windowPos) ?: return
        moveBound(movingMin, hit.owner, hit.offset)
    }

    fun selectAll(id: Any, value: String, ttsIndex: Int? = null) {
        ensureNode(id, value)
        owner = id
        text = value
        ttsStartIndex = ttsIndex
        setAnchors(id, 0, id, value.length)
        freezeCurrent()
        toolbarRect = Rect.Zero
        hideLoupe()
        hasSelection = value.isNotEmpty()
        publishRange()
    }

    fun hideToolbar() {
        toolbarRect = Rect.Zero
    }

    fun showLoupe(center: Offset) {
        loupeOwner = owner
        loupeCenter = center
    }

    fun showLoupe(id: Any, center: Offset) {
        loupeOwner = id
        loupeCenter = center
    }

    fun showLoupeAt(id: Any, offset: Int) {
        val layout = nodes[id]?.layout ?: return
        showLoupe(id, loupeSource(layout, offset))
    }

    fun hideLoupe() {
        loupeOwner = null
        loupeCenter = Offset.Unspecified
    }

    fun clear() {
        owner = null
        text = ""
        range = TextRange.Zero
        ttsStartIndex = null
        startOwner = null
        endOwner = null
        frozenStartOwner = null
        frozenEndOwner = null
        toolbarRect = Rect.Zero
        hideLoupe()
        hasSelection = false
    }

    fun refreshToolbar() {
        val from = ordered()?.first ?: return
        val node = nodes[from.owner] ?: return
        val layout = node.layout ?: return
        val coords = node.coordinates ?: return
        val local = rangeIn(from.owner) ?: return
        if (!coords.isAttached) return
        val boxes = HighlightMarks.highlightRects(layout, local.min, local.max)
        val box = boxes.firstOrNull() ?: return
        val topLeft = coords.localToWindow(Offset(box.left, box.top))
        val bottomRight = coords.localToWindow(Offset(box.right, box.bottom))
        toolbarRect = Rect(topLeft, bottomRight)
    }

    fun hit(windowPos: Offset): SelectionAnchor? {
        val attached = nodes.values.filter { node ->
            node.layout != null && node.coordinates?.isAttached == true
        }
        if (attached.isEmpty()) return null
        var best: SelectionNode? = null
        var bestArea = Float.MAX_VALUE
        var bestLocal = Offset.Zero
        for (node in attached) {
            val coords = node.coordinates ?: continue
            val local = coords.windowToLocal(windowPos)
            val width = coords.size.width.toFloat()
            val height = coords.size.height.toFloat()
            if (local.x in 0f..width && local.y in 0f..height) {
                val area = width * height
                if (area < bestArea) {
                    best = node
                    bestArea = area
                    bestLocal = local
                }
            }
        }
        val inside = best
        val insideLayout = inside?.layout
        if (inside != null && insideLayout != null) {
            return SelectionAnchor(inside.owner, JustifiedLayout.offsetAt(insideLayout, bestLocal))
        }
        var nearest: SelectionNode? = null
        var nearestDist = Float.MAX_VALUE
        var nearestLocal = Offset.Zero
        for (node in attached) {
            val coords = node.coordinates ?: continue
            val local = coords.windowToLocal(windowPos)
            val width = coords.size.width.toFloat().coerceAtLeast(1f)
            val height = coords.size.height.toFloat().coerceAtLeast(1f)
            val dx = when {
                local.x < 0f -> -local.x
                local.x > width -> local.x - width
                else -> 0f
            }
            val dy = when {
                local.y < 0f -> -local.y
                local.y > height -> local.y - height
                else -> 0f
            }
            val dist = dx * dx + dy * dy
            if (dist < nearestDist) {
                nearest = node
                nearestDist = dist
                nearestLocal = Offset(local.x.coerceIn(0f, width), local.y.coerceIn(0f, height))
            }
        }
        val node = nearest ?: return null
        val layout = node.layout ?: return null
        return SelectionAnchor(node.owner, JustifiedLayout.offsetAt(layout, nearestLocal))
    }

    private fun ensureNode(id: Any, value: String) {
        val existing = nodes[id]
        if (existing == null) {
            nodes[id] = SelectionNode(id, value, null, null)
        } else if (existing.text != value) {
            nodes[id] = existing.copy(text = value)
        }
    }

    private fun setAnchors(startId: Any?, start: Int, endId: Any?, end: Int) {
        startOwner = startId
        startOffset = start
        endOwner = endId
        endOffset = end
    }

    private fun freezeCurrent() {
        frozenStartOwner = startOwner
        frozenStartOffset = startOffset
        frozenEndOwner = endOwner
        frozenEndOffset = endOffset
    }

    private fun publishRange() {
        val id = owner
        val local = id?.let { rangeIn(it) }
        text = id?.let { nodeText(it) }.orEmpty()
        range = local ?: TextRange.Zero
        hasSelection = selectedText.isNotEmpty()
    }

    private fun ordered(): Pair<SelectionAnchor, SelectionAnchor>? {
        val startId = startOwner ?: return null
        val endId = endOwner ?: return null
        val a = SelectionAnchor(startId, startOffset)
        val b = SelectionAnchor(endId, endOffset)
        return if (compare(a, b) <= 0) a to b else b to a
    }

    private fun compare(a: SelectionAnchor, b: SelectionAnchor): Int {
        val byNode = indexOf(a.owner).compareTo(indexOf(b.owner))
        return if (byNode != 0) byNode else a.offset.compareTo(b.offset)
    }

    private fun rank(id: Any?, offset: Int): Long {
        if (id == null) return Long.MIN_VALUE
        return indexOf(id).toLong() * 1_000_000L + offset
    }

    private fun indexOf(id: Any?): Int {
        if (id == null) return -1
        var index = 0
        for (key in nodes.keys) {
            if (key === id) return index
            index++
        }
        return if (id === owner) 0 else -1
    }

    private fun nodeText(id: Any): String = nodes[id]?.text ?: if (id === owner) text else ""

    private data class SelectionNode(
        val owner: Any,
        val text: String,
        val layout: TextLayoutResult?,
        val coordinates: LayoutCoordinates?,
    )
}

data class SelectionAnchor(
    val owner: Any,
    val offset: Int,
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

    DisposableEffect(owner) {
        onDispose { state.detach(owner) }
    }
    SideEffect {
        state.attach(owner, text, layout, coordinates)
    }
    onGloballyPositioned { coords ->
        onCoordinates(coords)
        state.attach(owner, textRef.value, layoutRef.value, coords)
        if (state.owns(owner) && state.toolbarReady) {
            state.refreshToolbar()
        }
    }
        .magnifier(
            sourceCenter = {
                if (state.ownsLoupe(owner)) state.loupeCenter else Offset.Unspecified
            },
            zoom = LOUPE_ZOOM,
            size = DpSize(LOUPE_WIDTH, LOUPE_HEIGHT),
            cornerRadius = LOUPE_HEIGHT / 2,
            elevation = 8.dp,
        )
        .drawWithContent {
            val current = layoutRef.value
            val local = state.rangeIn(owner)
            if (local != null && current != null) {
                drawSelection(current, local, colors.backgroundColor)
            }
            drawContent()
            if (local != null && current != null) {
                drawHandles(
                    current,
                    local,
                    colors.handleColor,
                    start = state.hasStartHandle(owner),
                    end = state.hasEndHandle(owner),
                )
            }
        }
        .pointerInput(owner) {
            val touchSlop = viewConfig.touchSlop
            val longPressTimeout = viewConfig.longPressTimeoutMillis
            val handleSlop = 40.dp.toPx()
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
        val local = state.rangeIn(owner)
        if (local != null) {
            val startHandle = handleCenter(currentLayout, local.min, start = true)
            val endHandle = handleCenter(currentLayout, local.max, start = false)
            val movingMin = state.hasStartHandle(owner) &&
                nearHandle(down.position, startHandle, handleSlop)
            val movingMax = state.hasEndHandle(owner) &&
                nearHandle(down.position, endHandle, handleSlop)
            if (movingMin || movingMax) {
                down.consume()
                state.hideToolbar()
                val bound = if (movingMin) local.min else local.max
                state.showLoupe(owner, loupeSource(currentLayout, bound))
                dragSelectionBound(down.id, movingMin, owner, state, layout, coordinates, pass)
                return
            }
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
        state.showLoupe(owner, loupeSource(laid, index))
        dragExtendSelection(down.id, owner, state, layout, coordinates, pass)
        return
    }

    val change = currentEvent.changes.firstOrNull { it.id == down.id } ?: return
    val travel = (change.position - down.position).getDistance()
    if (change.pressed && travel > touchSlop) {
        val delta = change.position - down.position
        if (state.owns(owner)) {
            val local = state.rangeIn(owner) ?: return
            change.consume()
            state.hideToolbar()
            val at = JustifiedLayout.offsetAt(currentLayout, down.position)
            val movingMin = closerToMin(at, local)
            state.moveBound(movingMin, owner, JustifiedLayout.offsetAt(currentLayout, change.position))
            state.showLoupe(owner, loupeSource(currentLayout, if (movingMin) local.min else local.max))
            dragSelectionBound(down.id, movingMin, owner, state, layout, coordinates, pass)
            return
        }
        if (kotlin.math.abs(delta.x) > kotlin.math.abs(delta.y)) {
            val laid = layout() ?: return
            change.consume()
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val start = JustifiedLayout.offsetAt(laid, down.position)
            state.begin(owner, text(), laid.getWordBoundary(start), ttsStartIndex())
            val hit = selectionAt(owner, change.position, state, laid, coordinates())
            state.extendTo(hit.owner, hit.offset)
            showHitLoupe(state, hit, owner, laid)
            dragExtendSelection(down.id, owner, state, layout, coordinates, pass)
            return
        }
        return
    }
    if (!change.pressed && travel <= touchSlop) {
        if (state.hasSelection) {
            state.clear()
        } else if (onTap(down.position)) {
            change.consume()
        }
    }
}

private suspend fun AwaitPointerEventScope.dragExtendSelection(
    pointerId: PointerId,
    owner: Any,
    state: ReaderSelectionState,
    layout: () -> TextLayoutResult?,
    coordinates: () -> LayoutCoordinates?,
    pass: PointerEventPass,
) {
    while (true) {
        val event = awaitPointerEvent(pass)
        val drag = event.changes.firstOrNull { it.id == pointerId } ?: break
        if (!drag.pressed) break
        drag.consume()
        val next = layout() ?: break
        val hit = selectionAt(owner, drag.position, state, next, coordinates())
        state.extendTo(hit.owner, hit.offset)
        showHitLoupe(state, hit, owner, next)
    }
    showToolbar(state)
}

private suspend fun AwaitPointerEventScope.dragSelectionBound(
    pointerId: PointerId,
    movingMin: Boolean,
    owner: Any,
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
        val hit = selectionAt(owner, change.position, state, current, coordinates())
        state.moveBound(movingMin, hit.owner, hit.offset)
        showHitLoupe(state, hit, owner, current)
    }
    showToolbar(state)
}

private fun selectionAt(
    owner: Any,
    localPos: Offset,
    state: ReaderSelectionState,
    layout: TextLayoutResult,
    coordinates: LayoutCoordinates?,
): SelectionAnchor {
    val coords = coordinates
    if (coords != null && coords.isAttached) {
        val width = coords.size.width.toFloat()
        val height = coords.size.height.toFloat()
        val inside = localPos.x in 0f..width && localPos.y in 0f..height
        if (!inside) {
            state.hit(coords.localToWindow(localPos))?.let { return it }
        }
    }
    return SelectionAnchor(owner, JustifiedLayout.offsetAt(layout, localPos))
}

private fun showHitLoupe(
    state: ReaderSelectionState,
    hit: SelectionAnchor,
    fallbackOwner: Any,
    fallbackLayout: TextLayoutResult,
) {
    if (hit.owner === fallbackOwner) {
        state.showLoupe(fallbackOwner, loupeSource(fallbackLayout, hit.offset))
    } else {
        state.showLoupeAt(hit.owner, hit.offset)
    }
}

internal fun closerToMin(offset: Int, range: TextRange): Boolean {
    return kotlin.math.abs(offset - range.min) <= kotlin.math.abs(offset - range.max)
}

private fun nearHandle(down: Offset, handle: Offset, slop: Float): Boolean {
    return kotlin.math.abs(down.x - handle.x) <= slop &&
        kotlin.math.abs(down.y - handle.y) <= slop
}

private fun showToolbar(state: ReaderSelectionState) {
    state.hideLoupe()
    state.refreshToolbar()
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
    start: Boolean = true,
    end: Boolean = true,
) {
    if (range.min == range.max) return
    val radius = 6.dp.toPx()
    if (start) drawCircle(color, radius, handleCenter(layout, range.min, start = true))
    if (end) drawCircle(color, radius, handleCenter(layout, range.max, start = false))
}

private fun handleCenter(layout: TextLayoutResult, offset: Int, start: Boolean): Offset {
    val line = caretLine(layout, offset, start)
    val x = JustifiedLayout.visualCursor(layout, offset, line)
    val y = if (start) layout.getLineTop(line) else layout.getLineBottom(line)
    return Offset(x, y)
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
private const val LOUPE_ZOOM = 1.75f
