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
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

object HighlightMarks {
    const val HighlightMarkAlpha = 0.45f

    fun highlightRects(layout: TextLayoutResult, start: Int, end: Int): List<Rect> =
        JustifiedLayout.highlightRects(layout, start, end)
}

fun List<PaintedHighlight>.visibleFor(settings: UserSettings): List<PaintedHighlight> {
    if (!settings.showHighlights) return emptyList()
    return filter { item ->
        when {
            item.mine -> settings.defaultHighlightVisibilityMine
            item.friend -> settings.defaultHighlightVisibilityFriends
            else -> settings.defaultHighlightVisibilityNostrverse
        }
    }
}

fun Modifier.drawHighlightMarks(
    layout: TextLayoutResult?,
    displayed: String,
    highlights: List<PaintedHighlight>,
    mineColor: Color = HighlightMine,
    friendsColor: Color = HighlightFriends,
    otherColor: Color = HighlightOther,
    underline: Boolean = false,
): Modifier = drawBehind {
    val result = layout ?: return@drawBehind
    if (highlights.isEmpty() || displayed.isEmpty()) return@drawBehind
    fun paint(items: List<PaintedHighlight>, fill: Color) {
        items.forEach { item ->
            QuoteMatch.occurrences(displayed, highlightMark(item.quote, item.context)).forEach { range ->
                paintHighlight(result, range.first, range.last + 1, fill, underline)
            }
        }
    }
    paint(highlights.filter { !it.mine && !it.friend }, otherColor)
    paint(highlights.filter { it.friend && !it.mine }, friendsColor)
    paint(highlights.filter { it.mine }, mineColor)
}

fun DrawScope.paintHighlight(
    layout: TextLayoutResult,
    start: Int,
    end: Int,
    fill: Color,
    underline: Boolean,
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
                color = fill.copy(alpha = HighlightMarks.HighlightMarkAlpha),
                topLeft = Offset(box.left - padXPx, box.top - padYPx),
                size = Size(box.width + padXPx * 2, box.height + padYPx * 2),
                cornerRadius = corner,
            )
        }
    }
}
