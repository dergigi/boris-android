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
import org.dergigi.boris.ui.theme.HighlightFoaf
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther
import org.dergigi.boris.ui.theme.SpokenMark

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
fun matchSpokenSpans(
    displayed: String,
    sentence: String?,
    paragraph: String?,
): List<HighlightSpan> {
    val item = sentence?.let { ArticleFind.paintedSpoken(it, paragraph) } ?: return emptyList()
    return matchHighlightSpans(displayed, listOf(item))
}

fun matchSpokenSpansForParagraph(
    displayed: String,
    sentence: String?,
    paragraph: String?,
    displayedTtsIndex: Int?,
    spokenTtsIndex: Int?,
): List<HighlightSpan> {
    if (displayedTtsIndex == null || spokenTtsIndex == null) return emptyList()
    if (displayedTtsIndex != spokenTtsIndex) return emptyList()
    return matchSpokenSpans(displayed, sentence, paragraph)
}

fun matchHighlightSpans(
    displayed: String,
    highlights: List<PaintedHighlight>,
): List<HighlightSpan> {
    if (displayed.isEmpty() || highlights.isEmpty()) return emptyList()
    return highlights.flatMap { item ->
        when {
            item.spoken && !item.context.isNullOrBlank() -> spokenContextSpans(displayed, item)
            item.find || item.outline -> quoteOccurrences(displayed, item)
            else -> anchoredQuoteSpans(displayed, item)
        }
    }
}

/** Paint every occurrence of the quote. Used for find, outlines, and highlights without context. */
private fun quoteOccurrences(displayed: String, item: PaintedHighlight): List<HighlightSpan> =
    QuoteMatch.occurrences(
        displayed,
        highlightMark(item.quote, item.context),
        ignoreCase = item.ignoreCase,
    ).map { range -> HighlightSpan(item, range.first, range.last + 1) }

/**
 * When a highlight has context, paint only the occurrence that sits in that
 * window. Missing or unmatched context falls back to every occurrence for
 * longer legacy highlights. Short quotes without a usable window paint only
 * when they appear once; multiple hits fail closed so the reader is not flooded.
 */
private fun anchoredQuoteSpans(displayed: String, item: PaintedHighlight): List<HighlightSpan> {
    val fallback = { fallbackQuoteSpans(displayed, item) }
    val context = item.context?.takeIf { it.isNotBlank() } ?: return fallback()
    val mark = highlightMark(item.quote, context)
    if (mark.isBlank()) return emptyList()
    val ignoreCase = item.ignoreCase
    val quoteInContext = preferredQuoteRange(context, mark, ignoreCase) ?: return fallback()
    val contextHits = QuoteMatch.occurrences(displayed, context, ignoreCase = ignoreCase)
    if (contextHits.isNotEmpty()) {
        val hit = contextHits.first()
        return listOf(
            HighlightSpan(
                item = item,
                start = hit.first + quoteInContext.first,
                end = hit.first + quoteInContext.last + 1,
            ),
        )
    }
    val displayedInContext = QuoteMatch.occurrences(context, displayed, ignoreCase = ignoreCase)
        .firstOrNull()
    if (displayedInContext != null) {
        val start = quoteInContext.first - displayedInContext.first
        val end = quoteInContext.last + 1 - displayedInContext.first
        if (start >= 0 && end <= displayed.length && end > start) {
            return listOf(HighlightSpan(item, start, end))
        }
        return emptyList()
    }
    return fallback()
}

private fun fallbackQuoteSpans(displayed: String, item: PaintedHighlight): List<HighlightSpan> {
    if (item.quote.isBlank()) return emptyList()
    val matches = quoteOccurrences(displayed, item)
    if (isShortAmbiguousHighlight(item.quote) && matches.size > 1) return emptyList()
    return matches
}

private fun isShortAmbiguousHighlight(quote: String): Boolean {
    val words = quote.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return words.size <= 2 && words.sumOf { it.length } <= 24
}

/** Prefer the quote nearest the middle of [context] when it appears more than once. */
internal fun preferredQuoteRange(context: String, mark: String, ignoreCase: Boolean): IntRange? {
    val ranges = QuoteMatch.occurrences(context, mark, ignoreCase = ignoreCase)
    if (ranges.isEmpty()) return null
    if (ranges.size == 1) return ranges[0]
    val mid = context.length / 2
    return ranges.minBy { kotlin.math.abs((it.first + it.last) / 2 - mid) }
}

private fun spokenContextSpans(displayed: String, item: PaintedHighlight): List<HighlightSpan> {
    val context = item.context?.takeIf { item.spoken && it.isNotBlank() } ?: return emptyList()
    val mark = highlightMark(item.quote, context)
    if (mark.isBlank()) return emptyList()
    val ignoreCase = item.ignoreCase
    val exact = QuoteMatch.occurrences(displayed, context, ignoreCase = ignoreCase)
        .flatMap { contextRange ->
            val contextStart = contextRange.first
            val contextText = displayed.substring(contextStart, contextRange.last + 1)
            QuoteMatch.occurrences(contextText, mark, ignoreCase = ignoreCase).map { range ->
                HighlightSpan(
                    item = item,
                    start = contextStart + range.first,
                    end = contextStart + range.last + 1,
                )
            }
        }
    if (exact.isNotEmpty()) return exact
    if (!spokenContextBelongs(displayed, context)) return emptyList()
    val range = spokenRange(displayed, mark) ?: return emptyList()
    return listOf(HighlightSpan(item, range.first, range.last + 1))
}

internal fun spokenContextBelongs(displayed: String, context: String): Boolean {
    val words = spokenWords(context)
    if (words.isEmpty()) return true
    if (words.size == 1) return displayed.contains(words[0], ignoreCase = true)
    val first = displayed.indexOf(words[0], ignoreCase = true)
    if (first < 0) return false
    return displayed.indexOf(words[1], first + words[0].length, ignoreCase = true) >= 0
}

internal fun spokenRange(displayed: String, spoken: String): IntRange? {
    val words = spokenWords(spoken)
    if (words.isEmpty()) return null
    val start = displayed.indexOf(words.first(), ignoreCase = true)
    if (start < 0) return null
    val last = words.last()
    val lastAt = displayed.indexOf(last, start, ignoreCase = true)
    if (lastAt < 0) return start until start + words.first().length
    return start until lastAt + last.length
}

private fun spokenWords(text: String): List<String> =
    text.replace(Regex("""(?i)(?:https?://|www\.)\S+"""), " ")
        .split(Regex("\\s+"))
        .map { it.trim('"', '\'', '.', ',', ';', ':', '!', '?') }
        .filter { it.length >= 2 }

fun List<PaintedHighlight>.visibleFor(settings: UserSettings): List<PaintedHighlight> {
    if (!settings.showHighlights) return emptyList()
    return filter { item ->
        if (item.outline) return@filter false
        if (item.find) return@filter true
        when {
            item.mine -> settings.defaultHighlightVisibilityMine
            item.friend -> settings.defaultHighlightVisibilityFriends
            item.foaf -> settings.defaultHighlightVisibilityFoaf
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
    spokenColor: Color = SpokenMark,
    foafColor: Color = HighlightFoaf,
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
    // Spoken sentence (D-12) is a filled teal mark, never underline.
    paint({ it.spoken }, spokenColor, asUnderline = false, alpha = HighlightMarks.FindMarkAlpha)
    // Find matches always use a filled selection-like mark, never underline.
    paint({ it.find }, findColor, asUnderline = false, alpha = HighlightMarks.FindMarkAlpha)
    paint({ !it.find && !it.spoken && !it.outline && !it.mine && !it.friend && !it.foaf }, otherColor, underline)
    paint({ !it.find && !it.spoken && !it.outline && it.foaf && !it.friend && !it.mine }, foafColor, underline)
    paint({ !it.find && !it.spoken && !it.outline && it.friend && !it.mine }, friendsColor, underline)
    paint({ !it.find && !it.spoken && !it.outline && it.mine }, mineColor, underline)
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
