package org.dergigi.boris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.dergigi.boris.R
import org.dergigi.boris.data.MarkdownInline
import org.dergigi.boris.data.RelativeTime
import org.dergigi.boris.nostr.QuoteMatch
import org.dergigi.boris.ui.theme.ChromeColor
import org.dergigi.boris.ui.theme.SourceSerif

fun highlightContextParts(quote: String, context: String?): Triple<String, String, String> {
    val mark = quote.trim()
    if (mark.isEmpty()) return Triple(context.orEmpty(), "", "")
    if (context.isNullOrBlank()) return Triple("", mark, "")
    val range = QuoteMatch.occurrences(context, mark).firstOrNull()
    if (range != null) {
        if (coversContext(context, range)) {
            tighterCore(context)?.let { return it }
        }
        return Triple(
            context.substring(0, range.first),
            context.substring(range.first, range.last + 1),
            context.substring(range.last + 1),
        )
    }
    if (QuoteMatch.normalizeWhitespace(mark) == QuoteMatch.normalizeWhitespace(context.trim())) {
        tighterCore(context)?.let { return it }
    }
    return Triple(context, mark, "")
}

fun highlightMark(quote: String, context: String?): String =
    highlightContextParts(quote, context).second.ifBlank { quote.trim() }

private fun coversContext(context: String, range: IntRange): Boolean {
    val start = context.indexOfFirst { !it.isWhitespace() }
    val end = context.indexOfLast { !it.isWhitespace() }
    if (start < 0 || end < 0) return true
    return range.first <= start && range.last >= end
}

private fun tighterCore(context: String): Triple<String, String, String>? {
    val sentences = context.trim().split(SENTENCE_BREAK).filter { it.isNotBlank() }
    if (sentences.size < 3) return null
    val core = sentences[1]
    val index = context.indexOf(core)
    if (index < 0) return null
    return Triple(
        context.substring(0, index),
        core,
        context.substring(index + core.length),
    )
}

private val SENTENCE_BREAK = Regex("(?<=[.!?])\\s+")

@Composable
fun HighlightQuoteText(
    quote: String,
    color: Color,
    modifier: Modifier = Modifier,
    context: String? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val (before, marked, after) = highlightContextParts(quote, context)
    val (beforeText, beforeLinks) = MarkdownInline.flatten(before)
    val (markedText, markedLinks) = MarkdownInline.flatten(marked)
    val (afterText, afterLinks) = MarkdownInline.flatten(after)
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val uriHandler = LocalUriHandler.current
    val annotated = buildAnnotatedString {
        append(beforeText)
        val markStart = length
        append(markedText)
        val markEnd = length
        if (markStart < markEnd) {
            addStyle(
                SpanStyle(
                    background = color.copy(alpha = 0.45f),
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                markStart,
                markEnd,
            )
        }
        append(afterText)
        fun addLinks(links: List<MarkdownInline.Link>, offset: Int) {
            for (link in links) {
                addLink(
                    LinkAnnotation.Url(
                        url = link.url,
                        styles = linkStyle,
                        linkInteractionListener = { uriHandler.openUri(link.url) },
                    ),
                    start = offset + link.start,
                    end = offset + link.end,
                )
            }
        }
        addLinks(beforeLinks, 0)
        addLinks(markedLinks, beforeText.length)
        addLinks(afterLinks, beforeText.length + markedText.length)
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = SourceSerif,
            fontSize = 17.sp,
            lineHeight = 26.sp,
            fontStyle = FontStyle.Italic,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun HighlightCard(
    quote: String,
    color: Color,
    createdAt: Long,
    authorName: String,
    modifier: Modifier = Modifier,
    context: String? = null,
    host: String? = null,
    url: String? = null,
    authorPicture: String? = null,
    maxQuoteLines: Int = Int.MAX_VALUE,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    menu: HighlightCardMenu? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val chrome = ChromeColor.of(color, MaterialTheme.colorScheme.background)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, chrome, shape)
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.08f) else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.FormatQuote,
                contentDescription = null,
                tint = chrome,
                modifier = Modifier.size(18.dp),
            )
            if (createdAt > 0L) {
                Text(
                    text = RelativeTime.label(createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HighlightQuoteText(
            quote = quote,
            context = context,
            color = color,
            maxLines = maxQuoteLines,
        )
        if (!host.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.you_highlight_source, host),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HighlightAuthor(
                name = authorName,
                color = chrome,
                picture = authorPicture,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (menu != null || !url.isNullOrBlank()) {
                HighlightCardMenuButton(
                    menu = menu,
                    shareUrl = url,
                    shareQuote = quote,
                )
            }
        }
    }
}

@Composable
fun HighlightAuthor(
    name: String,
    color: Color,
    picture: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (picture != null) {
            val fallback = rememberVectorPainter(Icons.Outlined.AccountCircle)
            AsyncImage(
                model = picture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = fallback,
                error = fallback,
                fallback = fallback,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.SansSerif),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
