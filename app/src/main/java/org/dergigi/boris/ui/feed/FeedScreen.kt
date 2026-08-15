package org.dergigi.boris.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.dergigi.boris.R
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.ui.settings.hexColor
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther
import org.dergigi.boris.ui.theme.SourceSerif

@Composable
fun FeedScreen(
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    FeedScreenContent(
        state = state,
        refreshing = refreshing,
        scope = scope,
        loggedIn = loggedIn,
        nostrverseColor = hexColor(settings.highlightColorNostrverse, HighlightOther),
        friendsColor = hexColor(settings.highlightColorFriends, HighlightFriends),
        mineColor = hexColor(settings.highlightColorMine, HighlightMine),
        onRefresh = viewModel::refresh,
        onToggle = viewModel::toggle,
        onOpenArticle = onOpenArticle,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreenContent(
    state: FeedUiState,
    refreshing: Boolean,
    scope: FeedScope,
    loggedIn: Boolean,
    nostrverseColor: Color,
    friendsColor: Color,
    mineColor: Color,
    onRefresh: () -> Unit,
    onToggle: (FeedLevel) -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_feed)) },
                actions = {
                    ScopeToggle(
                        icon = Icons.Outlined.Hub,
                        on = scope.nostrverse,
                        enabled = true,
                        tint = nostrverseColor,
                        contentDescription = stringResource(R.string.settings_visibility_nostrverse),
                        onClick = { onToggle(FeedLevel.Nostrverse) },
                    )
                    ScopeToggle(
                        icon = Icons.Outlined.Group,
                        on = scope.friends,
                        enabled = loggedIn,
                        tint = friendsColor,
                        contentDescription = stringResource(
                            if (loggedIn) R.string.settings_visibility_friends else R.string.feed_scope_friends_login,
                        ),
                        onClick = { onToggle(FeedLevel.Friends) },
                    )
                    ScopeToggle(
                        icon = Icons.Outlined.Person,
                        on = scope.mine,
                        enabled = loggedIn,
                        tint = mineColor,
                        contentDescription = stringResource(
                            if (loggedIn) R.string.settings_visibility_mine else R.string.feed_scope_mine_login,
                        ),
                        onClick = { onToggle(FeedLevel.Mine) },
                    )
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when (state) {
                FeedUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                FeedUiState.Empty -> {
                    StatusMessage(
                        text = stringResource(R.string.feed_empty),
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                FeedUiState.Error -> {
                    StatusMessage(
                        text = stringResource(R.string.feed_error),
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                is FeedUiState.Ready -> {
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (state.items.isEmpty()) {
                            StatusMessage(
                                text = stringResource(R.string.feed_empty_filtered),
                                onRetry = onRefresh,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .widthIn(max = 720.dp)
                                        .fillMaxSize()
                                        .align(Alignment.TopCenter),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                ) {
                                    items(state.items, key = { it.id }) { item ->
                                        FeedHighlightRow(
                                            item = item,
                                            onOpenArticle = onOpenArticle,
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeToggle(
    icon: ImageVector,
    on: Boolean,
    enabled: Boolean,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val alpha = when {
        !enabled -> 0.28f
        on -> 1f
        else -> 0.4f
    }
    IconButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = alpha),
        )
    }
}

@Composable
private fun FeedHighlightRow(
    item: FeedItem,
    onOpenArticle: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.url != null) {
                item.url?.let(onOpenArticle)
            }
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = item.quote,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = SourceSerif,
                fontStyle = FontStyle.Italic,
                fontSize = 18.sp,
                lineHeight = 28.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val fallback = rememberVectorPainter(Icons.Outlined.AccountCircle)
            AsyncImage(
                model = item.authorPicture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = fallback,
                error = fallback,
                fallback = fallback,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            )
            Column {
                Text(
                    text = item.authorName,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.SansSerif),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!item.host.isNullOrBlank()) {
                    Text(
                        text = item.host,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = onRetry,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(stringResource(R.string.feed_retry))
        }
    }
}
