package org.dergigi.boris.ui.reader

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult
import org.dergigi.boris.nostr.QuoteMatch
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
        return highlights.flatMap { item ->
            QuoteMatch.occurrences(text, item.quote).mapNotNull { range ->
                val end = range.last + 1
                val top = localTopAt(range.first, end) ?: return@mapNotNull null
                HighlightStop(
                    highlightId = item.id,
                    owner = owner,
                    start = range.first,
                    end = end,
                    localTop = top,
                )
            }
        }.sortedBy { it.start }
    }

    fun nextIndex(current: Int, size: Int): Int {
        if (size <= 0) return -1
        return (current + 1) % size
    }

    fun scrollTarget(scrollValue: Int, scrollMax: Int, yInViewport: Float, paddingPx: Float): Int {
        return (scrollValue + yInViewport - paddingPx).roundToInt().coerceIn(0, scrollMax.coerceAtLeast(0))
    }
}

@Stable
class HighlightNavigator {
    private val nodes = linkedMapOf<Any, Node>()
    var stops: List<HighlightStop> = emptyList()
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

    fun hit(owner: Any, layout: TextLayoutResult, position: Offset): HighlightStop? {
        val node = nodes[owner] ?: return null
        return node.stops.firstOrNull { stop ->
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
    text: String,
    layout: TextLayoutResult?,
    coordinates: LayoutCoordinates?,
    highlights: List<PaintedHighlight>,
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
            HighlightJump.stopsInText(owner, text, highlights) { start, end ->
                HighlightMarks.highlightRects(laid, start, end).firstOrNull()?.top
            }
        }
        navigator.put(owner, stops, coords)
    }
    this
}
