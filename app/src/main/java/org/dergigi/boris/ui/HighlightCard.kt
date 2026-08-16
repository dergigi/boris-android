package org.dergigi.boris.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.dergigi.boris.R
import org.dergigi.boris.data.RelativeTime
import org.dergigi.boris.ui.theme.SourceSerif

fun highlightContextParts(quote: String, context: String?): Triple<String, String, String> {
    if (context.isNullOrBlank()) return Triple("", quote, "")
    val index = context.indexOf(quote)
    if (index < 0) return Triple(context.trim(), quote, "")
    return Triple(
        context.substring(0, index),
        quote,
        context.substring(index + quote.length),
    )
}

@Composable
fun HighlightQuoteText(
    quote: String,
    color: Color,
    modifier: Modifier = Modifier,
    context: String? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val (before, marked, after) = highlightContextParts(quote, context)
    Text(
        text = buildAnnotatedString {
            if (before.isNotBlank()) append(before)
            withStyle(
                SpanStyle(
                    background = color.copy(alpha = 0.45f),
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            ) {
                append(marked)
            }
            if (after.isNotBlank()) append(after)
        },
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
    authorPicture: String? = null,
    maxQuoteLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.55f), shape)
            .clip(shape)
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
                tint = color,
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
        HighlightAuthor(
            name = authorName,
            color = color,
            picture = authorPicture,
        )
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
