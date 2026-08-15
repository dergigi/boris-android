package org.dergigi.boris.ui.reader

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import org.dergigi.boris.nostr.QuoteMatch
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

object HighlightMarks {
    const val HighlightMarkAlpha = 0.45f

    fun highlightRects(layout: TextLayoutResult, start: Int, end: Int): List<Rect> {
        if (start >= end) return emptyList()
        val first = layout.getLineForOffset(start)
        val last = layout.getLineForOffset(end - 1)
        return (first..last).mapNotNull { line ->
            val lineStart = maxOf(start, layout.getLineStart(line))
            val lineEnd = minOf(end, layout.getLineEnd(line, visibleEnd = true))
            if (lineEnd <= lineStart) return@mapNotNull null
            val lastChar = (lineEnd - 1).coerceAtLeast(lineStart)
            val firstBox = layout.getBoundingBox(lineStart)
            val lastBox = layout.getBoundingBox(lastChar)
            Rect(
                left = firstBox.left,
                top = minOf(firstBox.top, lastBox.top),
                right = lastBox.right,
                bottom = maxOf(firstBox.bottom, lastBox.bottom),
            )
        }
    }
}

fun Modifier.drawHighlightMarks(
    layout: TextLayoutResult?,
    displayed: String,
    highlights: List<PaintedHighlight>,
): Modifier = drawBehind {
    val result = layout ?: return@drawBehind
    if (highlights.isEmpty() || displayed.isEmpty()) return@drawBehind
    val padXPx = 5.dp.toPx()
    val padYPx = 3.dp.toPx()
    val corner = CornerRadius(3.dp.toPx())
    fun paint(items: List<PaintedHighlight>, fill: Color) {
        items.forEach { item ->
            QuoteMatch.occurrences(displayed, item.quote).forEach { range ->
                HighlightMarks.highlightRects(result, range.first, range.last + 1).forEach { box ->
                    drawRoundRect(
                        color = fill,
                        topLeft = Offset(box.left - padXPx, box.top - padYPx),
                        size = Size(box.width + padXPx * 2, box.height + padYPx * 2),
                        cornerRadius = corner,
                    )
                }
            }
        }
    }
    val others = highlights.filter { !it.mine }
    val mine = highlights.filter { it.mine }
    paint(others, HighlightOther.copy(alpha = HighlightMarks.HighlightMarkAlpha))
    paint(mine, HighlightMine.copy(alpha = HighlightMarks.HighlightMarkAlpha))
}
