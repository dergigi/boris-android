package org.dergigi.boris.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dergigi.boris.R
import org.dergigi.boris.data.ReadingPositionStore
import kotlin.math.roundToInt

object ReadingProgress {
    const val COMPLETE_PERCENT = 95

    /** Positions below this fraction are noise; not worth restoring. */
    private const val MIN_RESTORE_FRACTION = 0.01f

    fun percent(scrollValue: Int, scrollMax: Int): Int {
        if (scrollMax <= 0) return 0
        return ((scrollValue.toFloat() / scrollMax) * 100f).roundToInt().coerceIn(0, 100)
    }

    fun isComplete(percent: Int): Boolean = percent >= COMPLETE_PERCENT

    fun isStarted(percent: Int): Boolean = percent in 1..10

    fun fraction(scrollValue: Int, scrollMax: Int): Float {
        if (scrollMax <= 0) return 0f
        return (scrollValue.toFloat() / scrollMax).coerceIn(0f, 1f)
    }

    /** Scroll offset to restore for a saved fraction, or null if not worth restoring. */
    fun restoreOffset(fraction: Float, scrollMax: Int): Int? {
        if (scrollMax <= 0 || fraction < MIN_RESTORE_FRACTION) return null
        return (fraction.coerceIn(0f, 1f) * scrollMax).roundToInt()
    }
}


private val CompleteGreen = Color(0xFF22C55E)

/**
 * Thin, subtle progress strip for article cards. Renders nothing when the
 * article has not been opened yet, mirroring the webapp card indicator.
 */
@Composable
fun CardReadingProgress(
    url: String?,
    modifier: Modifier = Modifier,
) {
    if (url.isNullOrBlank()) return
    val version by ReadingPositionStore.version.collectAsStateWithLifecycle()
    val percent = remember(url, version) {
        (ReadingPositionStore.fraction(url) * 100f).roundToInt()
    }
    if (percent <= 0) return
    val clamped = percent.coerceIn(1, 100)
    val complete = ReadingProgress.isComplete(clamped)
    val started = ReadingProgress.isStarted(clamped)
    val barColor = when {
        complete -> CompleteGreen
        started -> MaterialTheme.colorScheme.onBackground
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped / 100f)
                .background(barColor),
        )
    }
}

/**
 * Bottom-of-reader progress bar. [percent] fills the bar; [scrollPercent], when
 * set, marks the current (exploratory) scroll position as a small dot while the
 * fill keeps showing the saved reading position (issue #86).
 */
@Composable
fun ReadingProgressBar(
    percent: Int,
    modifier: Modifier = Modifier,
    scrollPercent: Int? = null,
) {
    val clamped = percent.coerceIn(0, 100)
    val complete = ReadingProgress.isComplete(clamped)
    val started = ReadingProgress.isStarted(clamped)
    val barColor = when {
        complete -> CompleteGreen
        started -> MaterialTheme.colorScheme.onBackground
        else -> MaterialTheme.colorScheme.primary
    }
    val labelColor = when {
        complete -> CompleteGreen
        started -> MaterialTheme.colorScheme.onBackground
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(clamped / 100f)
                        .background(barColor),
                )
            }
            if (scrollPercent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(scrollPercent.coerceIn(0, 100) / 100f)
                        .height(8.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        Text(
            text = if (complete) {
                stringResource(R.string.reader_progress_done)
            } else {
                stringResource(R.string.reader_progress, clamped)
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontFeatureSettings = "tnum",
            ),
            color = labelColor,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 32.dp),
        )
    }
}
