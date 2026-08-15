package org.dergigi.boris.ui.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.max

/**
 * Compose's per-glyph boxes omit the extra gaps [TextAlign.Justify] inserts between
 * words. Marks and hit-testing have to put that extra back using the line's real edges.
 */
internal object JustifiedLayout {
    fun highlightRects(layout: TextLayoutResult, start: Int, end: Int): List<Rect> {
        if (start >= end) return emptyList()
        val first = layout.getLineForOffset(start)
        val last = layout.getLineForOffset(end - 1)
        return (first..last).mapNotNull { line ->
            val from = maxOf(start, layout.getLineStart(line))
            val to = minOf(end, layout.getLineEnd(line, visibleEnd = true))
            if (to <= from) return@mapNotNull null
            val left = visualCursor(layout, from, line)
            val right = visualCursor(layout, to, line)
            if (right <= left + 0.5f) return@mapNotNull null
            Rect(left, layout.getLineTop(line), right, layout.getLineBottom(line))
        }
    }

    fun offsetAt(layout: TextLayoutResult, position: Offset): Int {
        val length = layout.layoutInput.text.length
        if (length == 0 || layout.lineCount == 0) return 0
        val y = position.y.coerceIn(0f, max(layout.size.height.toFloat(), 0f))
        val line = layout.getLineForVerticalPosition(y).coerceIn(0, layout.lineCount - 1)
        val lineStart = layout.getLineStart(line)
        val lineEnd = layout.getLineEnd(line, visibleEnd = true)
        return offsetForX(position.x, lineStart, lineEnd) { offset ->
            visualCursor(layout, offset, line)
        }.coerceIn(0, length)
    }

    fun visualCursor(layout: TextLayoutResult, offset: Int, line: Int): Float {
        val lineStart = layout.getLineStart(line)
        val lineEnd = layout.getLineEnd(line, visibleEnd = true)
        val clamped = offset.coerceIn(lineStart, lineEnd)
        return visualX(
            offset = clamped,
            lineStart = lineStart,
            lineEnd = lineEnd,
            lineLeft = layout.getLineLeft(line),
            lineRight = visualLineRight(layout, line),
            naturalLeft = naturalCursor(layout, lineStart, line),
            naturalRight = naturalCursor(layout, lineEnd, line),
            naturalX = naturalCursor(layout, clamped, line),
            spacesOnLine = spaceCount(layout, lineStart, lineEnd),
            spacesBefore = spaceCount(layout, lineStart, clamped),
            atLineStart = clamped <= lineStart,
            atLineEnd = clamped >= lineEnd,
        )
    }

    fun visualX(
        offset: Int,
        lineStart: Int,
        lineEnd: Int,
        lineLeft: Float,
        lineRight: Float,
        naturalLeft: Float,
        naturalRight: Float,
        naturalX: Float,
        spacesOnLine: Int,
        spacesBefore: Int,
        atLineStart: Boolean,
        atLineEnd: Boolean,
    ): Float {
        if (atLineStart || offset <= lineStart) return lineLeft
        if (atLineEnd || offset >= lineEnd) return lineRight
        val extra = (lineRight - lineLeft) - (naturalRight - naturalLeft)
        if (extra <= 0.5f || spacesOnLine <= 0) return naturalX
        val shift = extra * (spacesBefore.toFloat() / spacesOnLine.toFloat())
        return naturalX + shift
    }

    fun offsetForX(
        x: Float,
        lineStart: Int,
        lineEnd: Int,
        visualCursor: (Int) -> Float,
    ): Int {
        if (lineEnd <= lineStart) return lineStart
        if (x <= visualCursor(lineStart)) return lineStart
        if (x >= visualCursor(lineEnd)) return lineEnd
        var lo = lineStart
        var hi = lineEnd
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (visualCursor(mid) <= x) lo = mid else hi = mid - 1
        }
        if (lo < lineEnd) {
            val left = visualCursor(lo)
            val right = visualCursor(lo + 1)
            if (x - left > right - x) return lo + 1
        }
        return lo
    }

    internal fun visualLineRight(layout: TextLayoutResult, line: Int): Float {
        val lineRight = layout.getLineRight(line)
        if (layout.layoutInput.style.textAlign != TextAlign.Justify) return lineRight
        if (line >= layout.lineCount - 1) return lineRight
        return max(lineRight, layout.size.width.toFloat())
    }

    internal fun naturalCursor(layout: TextLayoutResult, offset: Int, line: Int): Float {
        val text = layout.layoutInput.text
        if (text.isEmpty()) return layout.getLineLeft(line)
        val lineStart = layout.getLineStart(line)
        val lineEnd = layout.getLineEnd(line, visibleEnd = true)
        val lineLeft = layout.getLineLeft(line)
        if (offset >= lineEnd && lineEnd > lineStart) {
            val last = (lineEnd - 1).coerceIn(0, text.length - 1)
            val box = layout.getBoundingBox(last)
            val right = maxOf(box.left, box.right)
            if (right > lineLeft + 0.5f) return right
        }
        val o = offset.coerceIn(0, text.length)
        val x = layout.getHorizontalPosition(o, true)
        return if (offset >= lineEnd && x < lineLeft + 0.5f) {
            lineLeft
        } else {
            x
        }
    }

    private fun spaceCount(layout: TextLayoutResult, start: Int, end: Int): Int {
        val text = layout.layoutInput.text
        val from = start.coerceAtLeast(0)
        val to = end.coerceAtMost(text.length)
        var n = 0
        for (i in from until to) {
            if (text[i] == ' ') n++
        }
        return n
    }
}
