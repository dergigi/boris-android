package org.dergigi.boris.ui.reader

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

data class HighlightStop(
    val highlightId: String,
    val owner: Any,
    val start: Int,
    val end: Int,
    val localTop: Float,
)

object HighlightJump {
    fun stopsInText(
        owner: Any,
        text: String,
        highlights: List<PaintedHighlight>,
        localTopAt: (start: Int, end: Int) -> Float?,
    ): List<HighlightStop> {
        if (text.isEmpty() || highlights.isEmpty()) return emptyList()
        return stopsFromSpans(owner, matchHighlightSpans(text, highlights), localTopAt)
    }

    fun stopsFromSpans(
        owner: Any,
        spans: List<HighlightSpan>,
        localTopAt: (start: Int, end: Int) -> Float?,
    ): List<HighlightStop> {
        return spans.mapNotNull { span ->
            val top = localTopAt(span.start, span.end) ?: return@mapNotNull null
            HighlightStop(
                highlightId = span.item.id,
                owner = owner,
                start = span.start,
                end = span.end,
                localTop = top,
            )
        }.sortedBy { it.start }
    }

    fun nextIndex(current: Int, size: Int): Int {
        if (size <= 0) return -1
        return (current + 1) % size
    }

    fun scrollTarget(scrollValue: Int, scrollMax: Int, yInViewport: Float, paddingPx: Float): Int {
        return (scrollValue + yInViewport - paddingPx).roundToInt().coerceIn(0, scrollMax.coerceAtLeast(0))
    }

    /**
     * Reading order: first occurrence in [texts] (title, then body) on top.
     * Quotes that never match the article stay at the bottom.
     */
    fun inDocumentOrder(
        highlights: List<PaintedHighlight>,
        texts: List<String>,
    ): List<PaintedHighlight> {
        if (highlights.size <= 1) return highlights
        val rank = HashMap<String, Int>(highlights.size)
        var offset = 0
        for (text in texts) {
            if (text.isNotEmpty()) {
                for (span in matchHighlightSpans(text, highlights)) {
                    rank.putIfAbsent(span.item.id, offset + span.start)
                }
                offset += text.length + 1
            }
        }
        if (rank.isEmpty()) return highlights
        return highlights.sortedWith(
            compareBy<PaintedHighlight> { rank[it.id] ?: Int.MAX_VALUE }
                .thenBy { it.createdAt },
        )
    }

    fun withFocus(
        highlights: List<PaintedHighlight>,
        highlightId: String?,
        quote: String?,
    ): List<PaintedHighlight> {
        val id = highlightId?.trim()?.lowercase().orEmpty()
        if (id.isEmpty()) return highlights
        if (highlights.any { it.id.equals(id, ignoreCase = true) }) return highlights
        val text = quote?.trim().orEmpty()
        if (text.isEmpty()) return highlights
        return highlights + PaintedHighlight(id = id, quote = text, mine = false)
    }

    suspend fun awaitStop(
        navigator: HighlightNavigator,
        highlightId: String,
        timeoutMs: Long = 10_000L,
        viewportReady: () -> Boolean,
    ): HighlightStop? {
        val id = highlightId.trim()
        if (id.isEmpty()) return null
        return withTimeoutOrNull(timeoutMs) {
            snapshotFlow {
                if (!viewportReady()) return@snapshotFlow null
                val stop = navigator.firstStop(id) ?: return@snapshotFlow null
                val coords = navigator.coordinates(stop.owner)
                if (coords == null || !coords.isAttached) return@snapshotFlow null
                stop
            }.filterNotNull().first()
        }
    }
}

@Stable
class HighlightNavigator {
    private val nodes = linkedMapOf<Any, Node>()
    var stops: List<HighlightStop> by mutableStateOf(emptyList())
        private set
    var index: Int = -1
        private set

    fun put(owner: Any, next: List<HighlightStop>, coords: LayoutCoordinates?) {
        if (next.isEmpty() || coords == null || !coords.isAttached) {
            if (nodes.remove(owner) != null) rebuild()
            return
        }
        val existing = nodes[owner]
        if (existing != null && existing.stops == next) {
            nodes[owner] = Node(next, coords)
            return
        }
        nodes[owner] = Node(next, coords)
        rebuild()
    }

    fun remove(owner: Any) {
        if (nodes.remove(owner) != null) rebuild()
    }

    fun next(): HighlightStop? {
        if (stops.isEmpty()) return null
        index = HighlightJump.nextIndex(index, stops.size)
        return stops[index]
    }

    fun select(stop: HighlightStop): HighlightStop {
        val i = stops.indexOfFirst { it.sameAs(stop) }
        if (i >= 0) index = i
        return stop
    }

    fun firstStop(highlightId: String): HighlightStop? =
        stops.firstOrNull { it.highlightId.equals(highlightId, ignoreCase = true) }

    fun nthStop(highlightId: String, occurrence: Int): HighlightStop? {
        if (occurrence < 0) return null
        return stops
            .asSequence()
            .filter { it.highlightId.equals(highlightId, ignoreCase = true) }
            .drop(occurrence)
            .firstOrNull()
    }

    fun stopCount(highlightId: String): Int =
        stops.count { it.highlightId.equals(highlightId, ignoreCase = true) }

    fun hit(owner: Any, layout: TextLayoutResult, position: Offset): HighlightStop? {
        val node = nodes[owner] ?: return null
        return node.stops.firstOrNull { stop ->
            if (ArticleOutline.isId(stop.highlightId)) return@firstOrNull false
            HighlightMarks.highlightRects(layout, stop.start, stop.end).any { it.inflate(8f).contains(position) }
        }
    }

    fun coordinates(owner: Any): LayoutCoordinates? = nodes[owner]?.coords

    private fun rebuild() {
        val previous = stops.getOrNull(index)
        stops = nodes.values.flatMap { it.stops }
        index = previous?.let { old -> stops.indexOfFirst { it.sameAs(old) } } ?: -1
    }

    private fun HighlightStop.sameAs(other: HighlightStop): Boolean =
        highlightId == other.highlightId && owner === other.owner && start == other.start

    private data class Node(
        val stops: List<HighlightStop>,
        val coords: LayoutCoordinates,
    )
}

fun Modifier.highlightAnchors(
    owner: Any,
    spans: List<HighlightSpan>,
    layout: TextLayoutResult?,
    coordinates: LayoutCoordinates?,
    navigator: HighlightNavigator,
): Modifier = composed {
    DisposableEffect(owner) {
        onDispose { navigator.remove(owner) }
    }
    SideEffect {
        val laid = layout
        val coords = coordinates
        val stops = if (laid == null || coords == null || !coords.isAttached) {
            emptyList()
        } else {
            HighlightJump.stopsFromSpans(owner, spans) { start, end ->
                HighlightMarks.highlightRects(laid, start, end).firstOrNull()?.top
            }
        }
        navigator.put(owner, stops, coords)
    }
    this
}
