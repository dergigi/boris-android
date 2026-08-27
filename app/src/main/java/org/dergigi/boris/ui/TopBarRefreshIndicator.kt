package org.dergigi.boris.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R

/** Pull-to-refresh that reports progress only via [TopBarRefreshIndicator]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        indicator = {},
        content = content,
    )
}

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
