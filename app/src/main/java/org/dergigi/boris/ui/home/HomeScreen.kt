package org.dergigi.boris.ui.home

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.dergigi.boris.R
import org.dergigi.boris.data.ArchivedArticles
import org.dergigi.boris.data.ClipboardLink
import org.dergigi.boris.data.HighlightedArticle
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.ui.auth.AuthUiState
import org.dergigi.boris.ui.auth.AuthViewModel
import org.dergigi.boris.ui.reader.CardReadingProgress
import org.dergigi.boris.ui.settings.hexColor
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRead: (String) -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
) {
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val loggedIn = authState is AuthUiState.LoggedIn
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    val context = LocalContext.current
    val windowInfo = LocalWindowInfo.current
    var clipboardUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { focused ->
            if (focused) clipboardUrl = ClipboardLink.read(context)
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    if (loggedIn) {
                        IconButton(
                            onClick = {
                                SettingsSync.apply(
                                    settings.withBoolean(
                                        "hideArchivedOnHome",
                                        !settings.hideArchivedOnHome,
                                    ),
                                )
                            },
                        ) {
                            Icon(
                                imageVector = BorisIcons.Books,
                                contentDescription = stringResource(
                                    if (settings.hideArchivedOnHome) {
                                        R.string.home_show_archived
                                    } else {
                                        R.string.home_hide_archived
                                    },
                                ),
                                tint = if (settings.hideArchivedOnHome) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = stringResource(R.string.home_about),
                        )
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            clipboardUrl?.let { url ->
                ClipboardBanner(
                    url = url,
                    onOpen = {
                        ClipboardLink.markHandled(url)
                        clipboardUrl = null
                        onRead(url)
                    },
                    onDismiss = {
                        ClipboardLink.markHandled(url)
                        clipboardUrl = null
                    },
                )
            }
            HomeScreenContent(
                highlights = highlights,
                refreshing = refreshing,
                loggedIn = loggedIn,
                hideArchived = settings.hideArchivedOnHome,
                mineColor = hexColor(settings.highlightColorMine, HighlightMine),
                friendsColor = hexColor(settings.highlightColorFriends, HighlightFriends),
                nostrverseColor = hexColor(settings.highlightColorNostrverse, HighlightOther),
                onRefresh = viewModel::refresh,
                onRead = onRead,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ClipboardBanner(
    url: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.ContentPaste,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_clipboard_open),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = url.removePrefix("https://").removePrefix("http://"),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.home_clipboard_dismiss),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    highlights: HomeHighlightsState,
    refreshing: Boolean,
    loggedIn: Boolean,
    hideArchived: Boolean,
    mineColor: Color,
    friendsColor: Color,
    nostrverseColor: Color,
    onRefresh: () -> Unit,
    onRead: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (highlights) {
            HomeHighlightsState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            HomeHighlightsState.Empty -> {
                StatusMessage(
                    text = stringResource(R.string.feed_empty),
                    onRetry = onRefresh,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            HomeHighlightsState.Error -> {
                StatusMessage(
                    text = stringResource(R.string.feed_error),
                    onRetry = onRefresh,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is HomeHighlightsState.Ready -> {
                val yours = ArchivedArticles.visible(
                    highlights.yours,
                    highlights.archivedKeys,
                    hideArchived,
                )
                val friends = ArchivedArticles.visible(
                    highlights.friends,
                    highlights.archivedKeys,
                    hideArchived,
                )
                val others = ArchivedArticles.visible(
                    highlights.others,
                    highlights.archivedKeys,
                    hideArchived,
                )
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (yours.isEmpty() && friends.isEmpty() && others.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            StatusMessage(
                                text = stringResource(
                                    if (hideArchived && highlights.archivedKeys.isNotEmpty()) {
                                        R.string.home_empty_archived
                                    } else {
                                        R.string.feed_empty
                                    },
                                ),
                                onRetry = onRefresh,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(top = 20.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(28.dp),
                        ) {
                            if (yours.isNotEmpty()) {
                                HighlightedRow(
                                    title = stringResource(R.string.home_recently_highlighted_by_you),
                                    items = yours,
                                    rowKey = "you",
                                    tint = mineColor,
                                    onRead = onRead,
                                )
                            }
                            if (friends.isNotEmpty()) {
                                HighlightedRow(
                                    title = stringResource(R.string.home_recently_highlighted_by_friends),
                                    items = friends,
                                    rowKey = "friends",
                                    tint = friendsColor,
                                    onRead = onRead,
                                )
                            }
                            if (others.isNotEmpty()) {
                                HighlightedRow(
                                    title = stringResource(
                                        if (loggedIn || yours.isNotEmpty() || friends.isNotEmpty()) {
                                            R.string.home_recently_highlighted_by_others
                                        } else {
                                            R.string.home_recently_highlighted
                                        },
                                    ),
                                    items = others,
                                    rowKey = "others",
                                    tint = nostrverseColor,
                                    onRead = onRead,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightedRow(
    title: String,
    items: List<HighlightedArticle>,
    rowKey: String,
    tint: Color,
    onRead: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = BorisIcons.Highlighter,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { "$rowKey:${it.url}" }) { article ->
                HighlightedArticleCard(
                    article = article,
                    fallbackTint = tint,
                    onOpen = { onRead(article.url) },
                )
            }
        }
    }
}

@Composable
private fun HighlightedArticleCard(
    article: HighlightedArticle,
    fallbackTint: Color,
    onOpen: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .width(CardWidth)
            .clickable(onClick = onOpen),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(CardImageSize)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (article.imageUrl.isNullOrBlank()) {
                Icon(
                    imageVector = BorisIcons.Highlighter,
                    contentDescription = null,
                    tint = fallbackTint,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = article.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = article.host,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CardReadingProgress(url = article.url)
    }
}

@Composable
private fun StatusMessage(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRetry, shape = RoundedCornerShape(8.dp)) {
            Text(stringResource(R.string.feed_retry))
        }
    }
}

private val CardWidth = 140.dp
private val CardImageSize = 140.dp
