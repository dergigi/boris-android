package org.dergigi.boris.ui.reader

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.nostr.QuoteMatch
import org.dergigi.boris.ui.highlightMark
import org.dergigi.boris.ui.theme.FindMark
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

object HighlightMarks {
    const val HighlightMarkAlpha = 0.45f
    const val FindMarkAlpha = 0.38f

    fun highlightRects(layout: TextLayoutResult, start: Int, end: Int): List<Rect> =
        JustifiedLayout.highlightRects(layout, start, end)
}

/** One matched occurrence of a highlight's painted mark within a text block. */
data class HighlightSpan(
    val item: PaintedHighlight,
    val start: Int,
    val end: Int,
)

/**
 * Quote-to-position matching is O(text length × highlights) with string
 * allocations, so it must run once per (text, highlights) — never per frame
 * in the draw phase. Callers cache the result with remember().
 */
fun matchHighlightSpans(
    displayed: String,
    highlights: List<PaintedHighlight>,
): List<HighlightSpan> {
    if (displayed.isEmpty() || highlights.isEmpty()) return emptyList()
    return highlights.flatMap { item ->
        QuoteMatch.occurrences(
            displayed,
            highlightMark(item.quote, item.context),
            ignoreCase = item.ignoreCase,
        ).map { range -> HighlightSpan(item, range.first, range.last + 1) }
    }
}

fun List<PaintedHighlight>.visibleFor(settings: UserSettings): List<PaintedHighlight> {
    if (!settings.showHighlights) return emptyList()
    return filter { item ->
        if (item.find) return@filter true
        when {
            item.mine -> settings.defaultHighlightVisibilityMine
            item.friend -> settings.defaultHighlightVisibilityFriends
            else -> settings.defaultHighlightVisibilityNostrverse
        }
    }
}

fun Modifier.drawHighlightMarks(
    layout: TextLayoutResult?,
    spans: List<HighlightSpan>,
    mineColor: Color = HighlightMine,
    friendsColor: Color = HighlightFriends,
    otherColor: Color = HighlightOther,
    underline: Boolean = false,
    findColor: Color = FindMark,
): Modifier = drawBehind {
    val result = layout ?: return@drawBehind
    if (spans.isEmpty()) return@drawBehind
    fun paint(matches: (PaintedHighlight) -> Boolean, fill: Color, asUnderline: Boolean, alpha: Float = HighlightMarks.HighlightMarkAlpha) {
        spans.forEach { span ->
            if (matches(span.item)) {
                paintHighlight(result, span.start, span.end, fill, asUnderline, alpha)
            }
        }
    }
    // Find matches always use a filled selection-like mark, never underline.
    paint({ it.find }, findColor, asUnderline = false, alpha = HighlightMarks.FindMarkAlpha)
    paint({ !it.find && !it.mine && !it.friend }, otherColor, underline)
    paint({ !it.find && it.friend && !it.mine }, friendsColor, underline)
    paint({ !it.find && it.mine }, mineColor, underline)
}

fun DrawScope.paintHighlight(
    layout: TextLayoutResult,
    start: Int,
    end: Int,
    fill: Color,
    underline: Boolean,
    alpha: Float = HighlightMarks.HighlightMarkAlpha,
) {
    val padXPx = 5.dp.toPx()
    val padYPx = 3.dp.toPx()
    val corner = CornerRadius(3.dp.toPx())
    val stroke = 2.dp.toPx()
    HighlightMarks.highlightRects(layout, start, end).forEach { box ->
        if (underline) {
            drawLine(
                color = fill.copy(alpha = 0.85f),
                start = Offset(box.left, box.bottom - stroke / 2),
                end = Offset(box.right, box.bottom - stroke / 2),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        } else {
            drawRoundRect(
                color = fill.copy(alpha = alpha),
                topLeft = Offset(box.left - padXPx, box.top - padYPx),
                size = Size(box.width + padXPx * 2, box.height + padYPx * 2),
                cornerRadius = corner,
            )
        }
    }
}
