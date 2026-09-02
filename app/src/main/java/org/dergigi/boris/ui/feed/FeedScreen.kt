package org.dergigi.boris.ui.feed

import org.dergigi.boris.ui.SignerEffects
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UnfoldMore
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
import androidx.compose.ui.res.pluralStringResource
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
import org.dergigi.boris.data.CollapsedHighlights
import org.dergigi.boris.data.HomeFilters
import org.dergigi.boris.data.RelativeTime
import org.dergigi.boris.data.RssItem
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.ArticleActionHandlers
import org.dergigi.boris.ui.PullToRefresh
import org.dergigi.boris.ui.ArticleRowWithMenu
import org.dergigi.boris.ui.ContentFilterMenu
import org.dergigi.boris.ui.ContentTab
import org.dergigi.boris.ui.rememberArticleActions
import org.dergigi.boris.ui.ContentTabs
import org.dergigi.boris.ui.HighlightCard
import org.dergigi.boris.ui.HighlightCardMenu
import org.dergigi.boris.ui.HighlightMenuViewModel
import org.dergigi.boris.ui.TopBarMenuItem
import org.dergigi.boris.ui.TopBarMoreMenu
import org.dergigi.boris.ui.TopBarRefreshIndicator
import org.dergigi.boris.ui.theme.rememberDisplayLook

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
    SignerEffects(
        signIntent = menuSignIntent,
        message = menuMessage,
        onConsumeSignIntent = menuViewModel::consumeSignIntent,
        onConsumeMessage = menuViewModel::consumeMessage,
        onSignerResult = menuViewModel::onSignerResult,
    )
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    val actions = rememberArticleActions()
    var tab by rememberSaveable {
        mutableStateOf(ContentTab.fromSettings(settings.defaultFeedView))
    }
    val look = rememberDisplayLook(settings)
    FeedScreenContent(
        state = state,
        refreshing = refreshing,
        scope = scope,
        tab = tab,
        loggedIn = loggedIn,
        rssItems = rssItems,
        rssLoading = rssLoading,
        hasRssFeeds = settings.rssFeeds.isNotEmpty(),
        settings = settings,
        nostrverseColor = look.nostrverse,
        foafColor = look.foaf,
        friendsColor = look.friends,
        mineColor = look.mine,
        onRefresh = viewModel::refresh,
        onToggle = viewModel::toggle,
        onSelectTab = { tab = it },
        onOpenArticle = onOpenArticle,
        onOpenHighlight = onOpenHighlight,
        actions = actions,
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
                onIgnoreArticle = FeedHighlightCollapse.articleKey(item)?.let { key ->
                    { CollapsedHighlights.ignoreArticle(context, key) }
                },
                onIgnoreAuthor = {
                    CollapsedHighlights.ignoreAuthor(context, item.authorHex)
                },
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
    settings: UserSettings,
    nostrverseColor: Color,
    foafColor: Color,
    friendsColor: Color,
    mineColor: Color,
    onRefresh: () -> Unit,
    onToggle: (FeedLevel) -> Unit,
    onSelectTab: (ContentTab) -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    actions: ArticleActionHandlers,
    onOpenRssSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    deletedIds: Set<String> = emptySet(),
    menuFor: (FeedItem) -> HighlightCardMenu? = { null },
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { CollapsedHighlights.ensure(context) }
    val collapsedArticles by CollapsedHighlights.articles.collectAsStateWithLifecycle()
    val collapsedAuthors by CollapsedHighlights.authors.collectAsStateWithLifecycle()
    fun expandCollapsed(row: FeedHighlightRow.Collapsed) {
        when (row.reason) {
            HighlightCollapseReason.Article -> CollapsedHighlights.showArticle(context, row.targetKey)
            HighlightCollapseReason.Author -> CollapsedHighlights.showAuthor(context, row.targetKey)
        }
    }
    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) {
        FeedInfoDialog(
            nostrverseColor = nostrverseColor,
            foafColor = foafColor,
            friendsColor = friendsColor,
            mineColor = mineColor,
            onDismiss = { showInfo = false },
        )
    }
    val filteredRssItems = remember(rssItems, actions.archivedKeys, settings) {
        rssItems.filter { item ->
            HomeFilters.visible(
                url = item.link,
                title = item.title,
                summary = item.summary,
                archivedKeys = actions.archivedKeys,
                hideArchived = settings.hideArchivedOnHome,
                hideCompleted = settings.hideCompletedOnHome,
                hideNsfw = settings.hideNsfwOnHome,
            )
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feed_title)) },
                actions = {
                    TopBarRefreshIndicator(refreshing = refreshing)
                    ScopeToggle(
                        icon = Icons.Outlined.Hub,
                        on = scope.nostrverse,
                        enabled = true,
                        tint = nostrverseColor,
                        contentDescription = stringResource(R.string.feed_scope_nostrverse),
                        onClick = { onToggle(FeedLevel.Nostrverse) },
                    )
                    ScopeToggle(
                        icon = Icons.Outlined.Groups,
                        on = scope.foaf,
                        enabled = loggedIn,
                        tint = foafColor,
                        contentDescription = stringResource(
                            if (loggedIn) R.string.feed_scope_foaf else R.string.feed_scope_foaf_login,
                        ),
                        onClick = { onToggle(FeedLevel.Foaf) },
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
                    ContentFilterMenu(settings = settings)
                    TopBarMoreMenu(
                        items = listOf(
                            TopBarMenuItem(
                                label = stringResource(R.string.feed_info),
                                icon = Icons.Outlined.Info,
                                onClick = { showInfo = true },
                            ),
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
                            PullToRefresh(
                                isRefreshing = refreshing,
                                onRefresh = onRefresh,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                val emptyText = when {
                                    rssItems.isNotEmpty() && filteredRssItems.isEmpty() ->
                                        R.string.feed_empty_filtered
                                    hasRssFeeds -> R.string.feed_rss_empty
                                    else -> R.string.feed_rss_none_configured
                                }
                                FeedRssList(
                                    items = filteredRssItems,
                                    emptyText = stringResource(emptyText),
                                    emptyActionLabel = stringResource(
                                        if (hasRssFeeds) R.string.feed_retry else R.string.feed_rss_open_settings,
                                    ),
                                    onEmptyAction = if (hasRssFeeds) onRefresh else onOpenRssSettings,
                                    onOpenArticle = onOpenArticle,
                                    actions = actions,
                                )
                            }
                        }
                    }
                    ContentTab.All -> {
                        FeedAllPane(
                            state = state,
                            refreshing = refreshing,
                            rssItems = filteredRssItems,
                            rssLoading = rssLoading,
                            deletedIds = deletedIds,
                            nostrverseColor = nostrverseColor,
                            foafColor = foafColor,
                            friendsColor = friendsColor,
                            mineColor = mineColor,
                            onRefresh = onRefresh,
                            onOpenArticle = onOpenArticle,
                            onOpenHighlight = onOpenHighlight,
                            actions = actions,
                            menuFor = menuFor,
                            settings = settings,
                            collapsedArticles = collapsedArticles,
                            collapsedAuthors = collapsedAuthors,
                            onExpand = ::expandCollapsed,
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
                                PullToRefresh(
                                    isRefreshing = refreshing,
                                    onRefresh = onRefresh,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    when (tab) {
                                        ContentTab.Highlights -> FeedHighlightList(
                                            items = state.highlights
                                                .filter { it.id !in deletedIds }
                                                .filter { item ->
                                                    HomeFilters.visible(
                                                        url = item.url,
                                                        title = item.quote,
                                                        summary = item.context,
                                                        archivedKeys = actions.archivedKeys,
                                                        hideArchived = settings.hideArchivedOnHome,
                                                        hideCompleted = settings.hideCompletedOnHome,
                                                        hideNsfw = settings.hideNsfwOnHome,
                                                    )
                                                },
                                            collapsedArticles = collapsedArticles,
                                            collapsedAuthors = collapsedAuthors,
                                            emptyText = emptyMessage(
                                                tab,
                                                filtered = state.hasHighlights,
                                            ),
                                            levelColor = { level ->
                                                when (level) {
                                                    FeedLevel.Mine -> mineColor
                                                    FeedLevel.Friends -> friendsColor
                                                    FeedLevel.Foaf -> foafColor
                                                    FeedLevel.Nostrverse -> nostrverseColor
                                                }
                                            },
                                            onRefresh = onRefresh,
                                            onOpenHighlight = onOpenHighlight,
                                            onExpand = ::expandCollapsed,
                                            menuFor = menuFor,
                                        )
                                        ContentTab.Writings -> FeedWritingList(
                                            items = state.writings.filter { item ->
                                                HomeFilters.visible(
                                                    url = item.url,
                                                    title = item.title,
                                                    summary = item.summary,
                                                    archivedKeys = actions.archivedKeys,
                                                    hideArchived = settings.hideArchivedOnHome,
                                                    hideCompleted = settings.hideCompletedOnHome,
                                                    hideNsfw = settings.hideNsfwOnHome,
                                                )
                                            },
                                            emptyText = emptyMessage(
                                                tab,
                                                filtered = state.hasWritings,
                                            ),
                                            onRefresh = onRefresh,
                                            onOpenArticle = onOpenArticle,
                                            actions = actions,
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

    data class CollapsedHighlight(val row: FeedHighlightRow.Collapsed) : FeedMergedItem() {
        override val sortAt: Long get() = row.sortAt
        override val key: String get() = row.key
    }
}

@Composable
private fun FeedAllPane(
    state: FeedUiState,
    refreshing: Boolean,
    rssItems: List<RssItem>,
    rssLoading: Boolean,
    deletedIds: Set<String>,
    nostrverseColor: Color,
    foafColor: Color,
    friendsColor: Color,
    mineColor: Color,
    onRefresh: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    actions: ArticleActionHandlers,
    menuFor: (FeedItem) -> HighlightCardMenu?,
    settings: UserSettings,
    collapsedArticles: Set<String>,
    collapsedAuthors: Set<String>,
    onExpand: (FeedHighlightRow.Collapsed) -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlights = (state as? FeedUiState.Ready)?.highlights.orEmpty()
        .filter { it.id !in deletedIds }
        .filter { item ->
            HomeFilters.visible(
                url = item.url,
                title = item.quote,
                summary = item.context,
                archivedKeys = actions.archivedKeys,
                hideArchived = settings.hideArchivedOnHome,
                hideCompleted = settings.hideCompletedOnHome,
                hideNsfw = settings.hideNsfwOnHome,
            )
        }
    val writings = (state as? FeedUiState.Ready)?.writings.orEmpty()
        .filter { item ->
            HomeFilters.visible(
                url = item.url,
                title = item.title,
                summary = item.summary,
                archivedKeys = actions.archivedKeys,
                hideArchived = settings.hideArchivedOnHome,
                hideCompleted = settings.hideCompletedOnHome,
                hideNsfw = settings.hideNsfwOnHome,
            )
        }
    val filteredRss = rssItems.filter { item ->
        HomeFilters.visible(
            url = item.link,
            title = item.title,
            summary = item.summary,
            archivedKeys = actions.archivedKeys,
            hideArchived = settings.hideArchivedOnHome,
            hideCompleted = settings.hideCompletedOnHome,
            hideNsfw = settings.hideNsfwOnHome,
        )
    }
    val highlightRows = remember(highlights, collapsedArticles, collapsedAuthors) {
        FeedHighlightCollapse.rows(highlights, collapsedArticles, collapsedAuthors)
    }
    val merged = remember(highlightRows, writings, filteredRss) {
        buildList {
            highlightRows.forEach { row ->
                when (row) {
                    is FeedHighlightRow.Open -> add(FeedMergedItem.Highlight(row.item))
                    is FeedHighlightRow.Collapsed -> add(FeedMergedItem.CollapsedHighlight(row))
                }
            }
            writings.forEach { add(FeedMergedItem.Writing(it)) }
            filteredRss.forEach { add(FeedMergedItem.Rss(it)) }
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
                PullToRefresh(
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
                                FeedLevel.Foaf -> foafColor
                                FeedLevel.Nostrverse -> nostrverseColor
                            }
                        },
                        onRefresh = onRefresh,
                        onOpenArticle = onOpenArticle,
                        onOpenHighlight = onOpenHighlight,
                        actions = actions,
                        menuFor = menuFor,
                        onExpand = onExpand,
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
    actions: ArticleActionHandlers,
    menuFor: (FeedItem) -> HighlightCardMenu?,
    onExpand: (FeedHighlightRow.Collapsed) -> Unit,
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
                            actions = actions,
                            onOpenArticle = onOpenArticle,
                        )
                    }
                    is FeedMergedItem.CollapsedHighlight -> {
                        CollapsedHighlightRow(
                            row = entry.row,
                            onExpand = { onExpand(entry.row) },
                        )
                    }
                    is FeedMergedItem.Rss -> {
                        FeedRssRow(
                            item = entry.item,
                            actions = actions,
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
    collapsedArticles: Set<String>,
    collapsedAuthors: Set<String>,
    emptyText: String,
    levelColor: (FeedLevel) -> Color,
    onRefresh: () -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    onExpand: (FeedHighlightRow.Collapsed) -> Unit,
    menuFor: (FeedItem) -> HighlightCardMenu? = { null },
) {
    val rows = remember(items, collapsedArticles, collapsedAuthors) {
        FeedHighlightCollapse.rows(items, collapsedArticles, collapsedAuthors)
    }
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
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is FeedHighlightRow.Open -> {
                        val item = row.item
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
                    is FeedHighlightRow.Collapsed -> {
                        CollapsedHighlightRow(
                            row = row,
                            onExpand = { onExpand(row) },
                        )
                    }
                }
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
    actions: ArticleActionHandlers,
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
                    actions = actions,
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
    actions: ArticleActionHandlers,
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
                    actions = actions,
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
private fun CollapsedHighlightRow(
    row: FeedHighlightRow.Collapsed,
    onExpand: () -> Unit,
) {
    val label = when (row.reason) {
        HighlightCollapseReason.Article -> {
            val source = row.source ?: stringResource(R.string.feed_highlights_this_article)
            pluralStringResource(
                R.plurals.feed_highlights_collapsed_article,
                row.count,
                row.count,
                source,
            )
        }
        HighlightCollapseReason.Author -> pluralStringResource(
            R.plurals.feed_highlights_collapsed_author,
            row.count,
            row.count,
            row.authorName.orEmpty(),
        )
    }
    val shape = RoundedCornerShape(8.dp)
    val muted = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, muted.copy(alpha = 0.7f), shape)
            .background(muted.copy(alpha = 0.18f))
            .clickable(onClick = onExpand)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.feed_highlights_expand),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Outlined.UnfoldMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun FeedWritingRow(
    item: FeedWriting,
    actions: ArticleActionHandlers,
    onOpenArticle: (String) -> Unit,
) {
    ArticleRowWithMenu(
        title = item.title,
        summary = item.summary,
        imageUrl = item.imageUrl,
        byline = item.authorName,
        bylinePicture = item.authorPicture,
        bylineFallbackIcon = Icons.Outlined.AccountCircle,
        publishedAt = item.publishedAt,
        url = item.url,
        loggedIn = actions.loggedIn,
        archived = actions.archived(item.url),
        onClick = { onOpenArticle(item.url) },
        onListen = { actions.onListen(item.url) },
        onMarkAsRead = { actions.onMarkAsRead(item.url, item.title, item.imageUrl) },
    )
}

@Composable
private fun FeedRssRow(
    item: RssItem,
    actions: ArticleActionHandlers,
    onOpenArticle: (String) -> Unit,
) {
    ArticleRowWithMenu(
        title = item.title,
        summary = item.summary,
        imageUrl = item.imageUrl,
        byline = item.sourceTitle,
        bylinePicture = null,
        bylineFallbackIcon = Icons.Outlined.RssFeed,
        publishedAt = item.publishedAt,
        url = item.link,
        loggedIn = actions.loggedIn,
        archived = actions.archived(item.link),
        onClick = { onOpenArticle(item.link) },
        onListen = { actions.onListen(item.link) },
        onMarkAsRead = { actions.onMarkAsRead(item.link, item.title, item.imageUrl) },
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
    foafColor: Color,
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
                    icon = Icons.Outlined.Groups,
                    tint = foafColor,
                    title = stringResource(R.string.feed_scope_foaf),
                    body = stringResource(R.string.feed_info_foaf),
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
