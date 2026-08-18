package org.dergigi.boris.ui.feed

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import org.dergigi.boris.data.RelativeTime
import org.dergigi.boris.data.RssItem
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.ui.ArticleRow
import org.dergigi.boris.ui.ContentTab
import org.dergigi.boris.ui.ContentTabs
import org.dergigi.boris.ui.HighlightCard
import org.dergigi.boris.ui.HighlightCardMenu
import org.dergigi.boris.ui.HighlightMenuViewModel
import org.dergigi.boris.ui.TopBarMenuItem
import org.dergigi.boris.ui.TopBarMoreMenu
import org.dergigi.boris.ui.settings.hexColor
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

@Composable
fun FeedScreen(
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit = { url, _, _ ->
        onOpenArticle(url)
    },
    onOpenProfile: (String) -> Unit = {},
    onOpenFeedSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = viewModel(),
    menuViewModel: HighlightMenuViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val rssItems by viewModel.rss.collectAsStateWithLifecycle()
    val rssLoading by viewModel.rssLoading.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val deletedIds by menuViewModel.deleted.collectAsStateWithLifecycle()
    val menuSignIntent by menuViewModel.signIntent.collectAsStateWithLifecycle()
    val menuMessage by menuViewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val signLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        menuViewModel.onSignerResult(result.resultCode, result.data)
    }
    LaunchedEffect(menuSignIntent) {
        val intent = menuSignIntent ?: return@LaunchedEffect
        menuViewModel.consumeSignIntent()
        signLauncher.launch(intent)
    }
    LaunchedEffect(menuMessage) {
        val message = menuMessage ?: return@LaunchedEffect
        menuViewModel.consumeMessage()
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    var tab by rememberSaveable {
        mutableStateOf(ContentTab.fromSettings(settings.defaultFeedView))
    }
    FeedScreenContent(
        state = state,
        refreshing = refreshing,
        scope = scope,
        tab = tab,
        loggedIn = loggedIn,
        rssItems = rssItems,
        rssLoading = rssLoading,
        hasRssFeeds = settings.rssFeeds.isNotEmpty(),
        nostrverseColor = hexColor(settings.highlightColorNostrverse, HighlightOther),
        friendsColor = hexColor(settings.highlightColorFriends, HighlightFriends),
        mineColor = hexColor(settings.highlightColorMine, HighlightMine),
        onRefresh = viewModel::refresh,
        onToggle = viewModel::toggle,
        onSelectTab = { tab = it },
        onOpenArticle = onOpenArticle,
        onOpenHighlight = onOpenHighlight,
        onOpenRssSettings = onOpenFeedSettings,
        deletedIds = deletedIds,
        menuFor = { item ->
            HighlightCardMenu(
                highlightId = item.id,
                authorHex = item.authorHex,
                onGoToQuote = item.url?.let { url ->
                    { onOpenHighlight(url, item.id, item.quote) }
                },
                onViewProfile = { onOpenProfile(item.authorHex) },
                onDelete = if (menuViewModel.canDelete(item.authorHex)) {
                    { menuViewModel.delete(item.id) }
                } else {
                    null
                },
            )
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreenContent(
    state: FeedUiState,
    refreshing: Boolean,
    scope: FeedScope,
    tab: ContentTab,
    loggedIn: Boolean,
    rssItems: List<RssItem>,
    rssLoading: Boolean,
    hasRssFeeds: Boolean,
    nostrverseColor: Color,
    friendsColor: Color,
    mineColor: Color,
    onRefresh: () -> Unit,
    onToggle: (FeedLevel) -> Unit,
    onSelectTab: (ContentTab) -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    onOpenRssSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    deletedIds: Set<String> = emptySet(),
    menuFor: (FeedItem) -> HighlightCardMenu? = { null },
) {
    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) {
        FeedInfoDialog(
            nostrverseColor = nostrverseColor,
            friendsColor = friendsColor,
            mineColor = mineColor,
            onDismiss = { showInfo = false },
        )
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feed_title)) },
                actions = {
                    ScopeToggle(
                        icon = Icons.Outlined.Hub,
                        on = scope.nostrverse,
                        enabled = true,
                        tint = nostrverseColor,
                        contentDescription = stringResource(R.string.feed_scope_nostrverse),
                        onClick = { onToggle(FeedLevel.Nostrverse) },
                    )
                    ScopeToggle(
                        icon = Icons.Outlined.Group,
                        on = scope.friends,
                        enabled = loggedIn,
                        tint = friendsColor,
                        contentDescription = stringResource(
                            if (loggedIn) R.string.feed_scope_friends else R.string.feed_scope_friends_login,
                        ),
                        onClick = { onToggle(FeedLevel.Friends) },
                    )
                    ScopeToggle(
                        icon = Icons.Outlined.Person,
                        on = scope.mine,
                        enabled = loggedIn,
                        tint = mineColor,
                        contentDescription = stringResource(
                            if (loggedIn) R.string.feed_scope_mine else R.string.feed_scope_mine_login,
                        ),
                        onClick = { onToggle(FeedLevel.Mine) },
                    )
                    IconButton(onClick = { showInfo = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.feed_info),
                        )
                    }
                    TopBarMoreMenu(
                        items = listOf(
                            TopBarMenuItem(
                                label = stringResource(R.string.feed_settings),
                                icon = Icons.Outlined.Settings,
                                onClick = onOpenRssSettings,
                            ),
                        ),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            ContentTabs(
                tab = tab,
                onSelect = onSelectTab,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                showAll = true,
                showRss = true,
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    ContentTab.Rss -> {
                        if (rssLoading && rssItems.isEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            PullToRefreshBox(
                                isRefreshing = refreshing,
                                onRefresh = onRefresh,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                FeedRssList(
                                    items = rssItems,
                                    emptyText = stringResource(
                                        if (hasRssFeeds) R.string.feed_rss_empty else R.string.feed_rss_none_configured,
                                    ),
                                    emptyActionLabel = stringResource(
                                        if (hasRssFeeds) R.string.feed_retry else R.string.feed_rss_open_settings,
                                    ),
                                    onEmptyAction = if (hasRssFeeds) onRefresh else onOpenRssSettings,
                                    onOpenArticle = onOpenArticle,
                                )
                            }
                        }
                    }
                    ContentTab.All -> {
                        FeedAllPane(
                            state = state,
                            refreshing = refreshing,
                            rssItems = rssItems,
                            rssLoading = rssLoading,
                            deletedIds = deletedIds,
                            nostrverseColor = nostrverseColor,
                            friendsColor = friendsColor,
                            mineColor = mineColor,
                            onRefresh = onRefresh,
                            onOpenArticle = onOpenArticle,
                            onOpenHighlight = onOpenHighlight,
                            menuFor = menuFor,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    ContentTab.Public, ContentTab.Web -> Unit
                    ContentTab.Highlights, ContentTab.Writings -> {
                        when (state) {
                            FeedUiState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                            FeedUiState.Empty -> {
                                StatusMessage(
                                    text = emptyMessage(tab, filtered = false),
                                    onAction = onRefresh,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                            FeedUiState.Error -> {
                                StatusMessage(
                                    text = stringResource(R.string.feed_error),
                                    onAction = onRefresh,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                            is FeedUiState.Ready -> {
                                PullToRefreshBox(
                                    isRefreshing = refreshing,
                                    onRefresh = onRefresh,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    when (tab) {
                                        ContentTab.Highlights -> FeedHighlightList(
                                            items = state.highlights.filter { it.id !in deletedIds },
                                            emptyText = emptyMessage(
                                                tab,
                                                filtered = state.hasHighlights,
                                            ),
                                            levelColor = { level ->
                                                when (level) {
                                                    FeedLevel.Mine -> mineColor
                                                    FeedLevel.Friends -> friendsColor
                                                    FeedLevel.Nostrverse -> nostrverseColor
                                                }
                                            },
                                            onRefresh = onRefresh,
                                            onOpenHighlight = onOpenHighlight,
                                            menuFor = menuFor,
                                        )
                                        ContentTab.Writings -> FeedWritingList(
                                            items = state.writings,
                                            emptyText = emptyMessage(
                                                tab,
                                                filtered = state.hasWritings,
                                            ),
                                            onRefresh = onRefresh,
                                            onOpenArticle = onOpenArticle,
                                        )
                                        else -> Unit
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
private fun emptyMessage(tab: ContentTab, filtered: Boolean): String {
    if (filtered) return stringResource(R.string.feed_empty_filtered)
    return stringResource(
        if (tab == ContentTab.Writings) R.string.feed_writings_empty else R.string.feed_empty,
    )
}

private sealed class FeedMergedItem {
    abstract val sortAt: Long
    abstract val key: String

    data class Highlight(val item: FeedItem) : FeedMergedItem() {
        override val sortAt: Long get() = item.createdAt
        override val key: String get() = "h:${item.id}"
    }

    data class Writing(val item: FeedWriting) : FeedMergedItem() {
        override val sortAt: Long get() = item.publishedAt
        override val key: String get() = "w:${item.id}"
    }

    data class Rss(val item: RssItem) : FeedMergedItem() {
        override val sortAt: Long get() = item.publishedAt
        override val key: String get() = "r:${item.link}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedAllPane(
    state: FeedUiState,
    refreshing: Boolean,
    rssItems: List<RssItem>,
    rssLoading: Boolean,
    deletedIds: Set<String>,
    nostrverseColor: Color,
    friendsColor: Color,
    mineColor: Color,
    onRefresh: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    menuFor: (FeedItem) -> HighlightCardMenu?,
    modifier: Modifier = Modifier,
) {
    val highlights = (state as? FeedUiState.Ready)?.highlights.orEmpty()
        .filter { it.id !in deletedIds }
    val writings = (state as? FeedUiState.Ready)?.writings.orEmpty()
    val merged = remember(highlights, writings, rssItems) {
        buildList {
            highlights.forEach { add(FeedMergedItem.Highlight(it)) }
            writings.forEach { add(FeedMergedItem.Writing(it)) }
            rssItems.forEach { add(FeedMergedItem.Rss(it)) }
        }.sortedByDescending { it.sortAt }
    }
    val waiting = state is FeedUiState.Loading && rssLoading && merged.isEmpty()
    Box(modifier = modifier) {
        when {
            waiting -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state is FeedUiState.Error && merged.isEmpty() -> {
                StatusMessage(
                    text = stringResource(R.string.feed_error),
                    onAction = onRefresh,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = refreshing || (rssLoading && rssItems.isEmpty()),
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    FeedAllList(
                        items = merged,
                        emptyText = stringResource(R.string.feed_all_empty),
                        levelColor = { level ->
                            when (level) {
                                FeedLevel.Mine -> mineColor
                                FeedLevel.Friends -> friendsColor
                                FeedLevel.Nostrverse -> nostrverseColor
                            }
                        },
                        onRefresh = onRefresh,
                        onOpenArticle = onOpenArticle,
                        onOpenHighlight = onOpenHighlight,
                        menuFor = menuFor,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedAllList(
    items: List<FeedMergedItem>,
    emptyText: String,
    levelColor: (FeedLevel) -> Color,
    onRefresh: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    menuFor: (FeedItem) -> HighlightCardMenu?,
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            StatusMessage(
                text = emptyText,
                onAction = onRefresh,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.key }) { entry ->
                when (entry) {
                    is FeedMergedItem.Highlight -> {
                        val item = entry.item
                        HighlightCard(
                            quote = item.quote,
                            context = item.context,
                            color = levelColor(item.level),
                            createdAt = item.createdAt,
                            authorName = item.authorName,
                            host = item.host,
                            url = item.url,
                            authorPicture = item.authorPicture,
                            maxQuoteLines = 8,
                            onClick = item.url?.let { url ->
                                { onOpenHighlight(url, item.id, item.quote) }
                            },
                            menu = menuFor(item),
                        )
                    }
                    is FeedMergedItem.Writing -> {
                        FeedWritingRow(
                            item = entry.item,
                            onOpenArticle = onOpenArticle,
                        )
                    }
                    is FeedMergedItem.Rss -> {
                        FeedRssRow(
                            item = entry.item,
                            onOpenArticle = onOpenArticle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedHighlightList(
    items: List<FeedItem>,
    emptyText: String,
    levelColor: (FeedLevel) -> Color,
    onRefresh: () -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    menuFor: (FeedItem) -> HighlightCardMenu? = { null },
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            StatusMessage(
                text = emptyText,
                onAction = onRefresh,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                HighlightCard(
                    quote = item.quote,
                    context = item.context,
                    color = levelColor(item.level),
                    createdAt = item.createdAt,
                    authorName = item.authorName,
                    host = item.host,
                    url = item.url,
                    authorPicture = item.authorPicture,
                    maxQuoteLines = 8,
                    onClick = item.url?.let { url ->
                        { onOpenHighlight(url, item.id, item.quote) }
                    },
                    menu = menuFor(item),
                )
            }
        }
    }
}

@Composable
private fun FeedWritingList(
    items: List<FeedWriting>,
    emptyText: String,
    onRefresh: () -> Unit,
    onOpenArticle: (String) -> Unit,
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            StatusMessage(
                text = emptyText,
                onAction = onRefresh,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                FeedWritingRow(
                    item = item,
                    onOpenArticle = onOpenArticle,
                )
            }
        }
    }
}

@Composable
private fun FeedRssList(
    items: List<RssItem>,
    emptyText: String,
    emptyActionLabel: String,
    onEmptyAction: () -> Unit,
    onOpenArticle: (String) -> Unit,
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            StatusMessage(
                text = emptyText,
                actionLabel = emptyActionLabel,
                onAction = onEmptyAction,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.link }) { item ->
                FeedRssRow(
                    item = item,
                    onOpenArticle = onOpenArticle,
                )
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
private fun FeedWritingRow(
    item: FeedWriting,
    onOpenArticle: (String) -> Unit,
) {
    ArticleRow(
        title = item.title,
        summary = item.summary,
        imageUrl = item.imageUrl,
        byline = item.authorName,
        bylinePicture = item.authorPicture,
        bylineFallbackIcon = Icons.Outlined.AccountCircle,
        publishedAt = item.publishedAt,
        url = item.url,
        onClick = { onOpenArticle(item.url) },
    )
}

@Composable
private fun FeedRssRow(
    item: RssItem,
    onOpenArticle: (String) -> Unit,
) {
    ArticleRow(
        title = item.title,
        summary = item.summary,
        imageUrl = item.imageUrl,
        byline = item.sourceTitle,
        bylinePicture = null,
        bylineFallbackIcon = Icons.Outlined.RssFeed,
        publishedAt = item.publishedAt,
        url = item.link,
        onClick = { onOpenArticle(item.link) },
    )
}

@Composable
private fun StatusMessage(
    text: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = stringResource(R.string.feed_retry),
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
            onClick = onAction,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun FeedInfoDialog(
    nostrverseColor: Color,
    friendsColor: Color,
    mineColor: Color,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feed_info_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FeedInfoRow(
                    icon = Icons.Outlined.Hub,
                    tint = nostrverseColor,
                    title = stringResource(R.string.feed_scope_nostrverse),
                    body = stringResource(R.string.feed_info_nostrverse),
                )
                FeedInfoRow(
                    icon = Icons.Outlined.Group,
                    tint = friendsColor,
                    title = stringResource(R.string.feed_scope_friends),
                    body = stringResource(R.string.feed_info_friends),
                )
                FeedInfoRow(
                    icon = Icons.Outlined.Person,
                    tint = mineColor,
                    title = stringResource(R.string.feed_scope_mine),
                    body = stringResource(R.string.feed_info_mine),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_info_close))
            }
        },
    )
}

@Composable
private fun FeedInfoRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
