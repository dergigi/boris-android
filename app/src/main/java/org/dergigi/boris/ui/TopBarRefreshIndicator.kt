package org.dergigi.boris.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R

@Composable
fun TopBarRefreshIndicator(
    refreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!refreshing) return
    val refreshingLabel = stringResource(R.string.refreshing)
    CircularProgressIndicator(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .size(18.dp)
            .semantics { contentDescription = refreshingLabel },
        strokeWidth = 2.dp,
    )
}
