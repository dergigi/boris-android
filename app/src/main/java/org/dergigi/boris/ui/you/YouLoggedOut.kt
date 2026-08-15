package org.dergigi.boris.ui.you

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dergigi.boris.R
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.ui.reader.HighlightMarks
import org.dergigi.boris.ui.settings.hexColor
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.SourceSerif

@Composable
fun YouLoggedOut(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.you_logged_out_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        HighlightSample(stringResource(R.string.you_logged_out_sample))
        Text(
            text = stringResource(R.string.you_logged_out_body),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HighlightSample(sample: String) {
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val dark = settings.isDark(isSystemInDarkTheme())
    val fill = hexColor(settings.highlightColorMine, HighlightMine)
        .copy(alpha = if (dark) 0.32f else 0.42f)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val padX = 6.dp
    val padY = 2.dp
    val radius = 2.dp
    Text(
        text = sample,
        onTextLayout = { layout = it },
        modifier = Modifier.drawBehind {
            val result = layout ?: return@drawBehind
            if (sample.isEmpty()) return@drawBehind
            val padXPx = padX.toPx()
            val padYPx = padY.toPx()
            val corner = CornerRadius(radius.toPx())
            HighlightMarks.highlightRects(result, 0, sample.length).forEach { box ->
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(box.left - padXPx, box.top - padYPx),
                    size = Size(box.width + padXPx * 2, box.height + padYPx * 2),
                    cornerRadius = corner,
                )
            }
        },
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = SourceSerif,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            textAlign = TextAlign.Center,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
