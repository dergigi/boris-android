package org.dergigi.boris.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.SensitiveContent

@Composable
fun NsfwBadge(
    warning: SensitiveContent.Warning,
    modifier: Modifier = Modifier,
) {
    val color = if (warning.confirmed) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, color.copy(alpha = 0.55f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(
                if (warning.confirmed) R.string.nsfw_badge else R.string.nsfw_badge_maybe,
            ),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.SansSerif),
            color = color,
        )
    }
}
