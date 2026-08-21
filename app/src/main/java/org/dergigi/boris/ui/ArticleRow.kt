package org.dergigi.boris.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.dergigi.boris.data.RelativeTime
import org.dergigi.boris.ui.reader.CardReadingProgress

/** Shared list row for writings, bookmarks, RSS, and search article hits. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArticleRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    imageUrl: String? = null,
    imageFallbackIcon: ImageVector = Icons.AutoMirrored.Outlined.Article,
    byline: String? = null,
    bylinePicture: String? = null,
    bylineFallbackIcon: ImageVector? = null,
    publishedAt: Long = 0L,
    url: String? = null,
    showReadingProgress: Boolean = true,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    val clickModifier = if (onLongClick == null) {
        Modifier.clickable(enabled = enabled, onClick = onClick)
    } else {
        Modifier.combinedClickable(
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick,
            onLongClickLabel = onLongClickLabel,
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(clickModifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl.isNullOrBlank()) {
                Icon(
                    imageVector = imageFallbackIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                // bodyLarge is reader-tuned (36sp / justify); list titles need tight wrap.
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!byline.isNullOrBlank() || publishedAt > 0L) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!byline.isNullOrBlank()) {
                        if (bylineFallbackIcon != null) {
                            val fallback = rememberVectorPainter(bylineFallbackIcon)
                            AsyncImage(
                                model = bylinePicture,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                placeholder = fallback,
                                error = fallback,
                                fallback = fallback,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape),
                            )
                        }
                        Text(
                            text = byline,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.SansSerif,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (publishedAt > 0L) {
                        Text(
                            text = RelativeTime.label(publishedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (showReadingProgress) {
                CardReadingProgress(url = url)
            }
        }
    }
}
