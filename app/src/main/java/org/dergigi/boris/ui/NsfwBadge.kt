package org.dergigi.boris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dergigi.boris.R
import org.dergigi.boris.data.SensitiveContent

@Composable
fun NsfwBadge(
    warning: SensitiveContent.Warning,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(
            if (warning.confirmed) R.string.nsfw_badge else R.string.nsfw_badge_maybe,
        ),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = MaterialTheme.colorScheme.onError,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
