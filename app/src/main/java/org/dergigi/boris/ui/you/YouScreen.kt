package org.dergigi.boris.ui.you

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.dergigi.boris.R
import org.dergigi.boris.data.RelativeTime
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.ui.settings.hexColor
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.SourceSerif

@Composable
fun YouHighlights(
    npub: String,
    profile: Profile?,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: YouViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val mineColor = hexColor(settings.highlightColorMine, HighlightMine)
    val displayName = profile?.name?.takeIf { it.isNotBlank() } ?: shortNpub(npub)
    LaunchedEffect(npub) {
        viewModel.refresh()
    }
    YouHighlightsContent(
        state = state,
        refreshing = refreshing,
        displayName = displayName,
        about = profile?.about,
        pictureUrl = profile?.picture,
        mineColor = mineColor,
        onRefresh = viewModel::refresh,
        onOpenArticle = onOpenArticle,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouHighlightsContent(
    state: YouUiState,
    refreshing: Boolean,
    displayName: String,
    about: String?,
    pictureUrl: String?,
    mineColor: Color,
    onRefresh: () -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                YouHeader(
                    displayName = displayName,
                    about = about,
                    pictureUrl = pictureUrl,
                )
            }
            when (state) {
                YouUiState.Loading -> {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                YouUiState.Empty -> {
                    item(key = "empty") {
                        StatusMessage(
                            text = stringResource(R.string.you_highlights_empty),
                            onRetry = onRefresh,
                        )
                    }
                }
                YouUiState.Error -> {
                    item(key = "error") {
                        StatusMessage(
                            text = stringResource(R.string.feed_error),
                            onRetry = onRefresh,
                        )
                    }
                }
                is YouUiState.Ready -> {
                    items(state.items, key = { it.id }) { item ->
                        YouHighlightCard(
                            item = item,
                            displayName = displayName,
                            mineColor = mineColor,
                            onOpenArticle = onOpenArticle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YouHeader(
    displayName: String,
    about: String?,
    pictureUrl: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fallback = rememberVectorPainter(Icons.Outlined.AccountCircle)
        AsyncImage(
            model = pictureUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = fallback,
            error = fallback,
            fallback = fallback,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!about.isNullOrBlank()) {
                Text(
                    text = about,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun YouHighlightCard(
    item: YouHighlight,
    displayName: String,
    mineColor: Color,
    onOpenArticle: (String) -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val border = mineColor.copy(alpha = 0.55f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border, shape)
            .clip(shape)
            .clickable(enabled = item.url != null) {
                item.url?.let(onOpenArticle)
            }
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
                tint = mineColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = RelativeTime.label(item.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = item.quote,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = SourceSerif,
                fontSize = 18.sp,
                lineHeight = 28.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .background(mineColor.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        if (!item.host.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.you_highlight_source, item.host),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = mineColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusMessage(
    text: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
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

private fun shortNpub(npub: String): String =
    if (npub.length > 16) npub.take(12) + "…" else npub
