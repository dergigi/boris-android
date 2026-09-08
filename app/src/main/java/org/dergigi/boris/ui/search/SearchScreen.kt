package org.dergigi.boris.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.R
import org.dergigi.boris.data.HomeFilters
import org.dergigi.boris.data.HighlightedArticle
import org.dergigi.boris.data.LocalSearch
import org.dergigi.boris.data.MostHighlightedWindow
import org.dergigi.boris.data.ReadingPositionStore
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.ArticleActionHandlers
import org.dergigi.boris.ui.ArticleRow
import org.dergigi.boris.ui.AuthorCard
import org.dergigi.boris.ui.SearchBarField
import org.dergigi.boris.ui.ContentFilterMenu
import org.dergigi.boris.ui.ContentTabChip
import org.dergigi.boris.ui.FilterChipRow
import org.dergigi.boris.ui.HighlightCard
import org.dergigi.boris.ui.HighlightCardMenu
import org.dergigi.boris.ui.TopBarMenuItem
import org.dergigi.boris.ui.TopBarMoreMenu
import org.dergigi.boris.ui.TopBarRefreshIndicator
import org.dergigi.boris.ui.home.ExploreRows
import org.dergigi.boris.ui.home.HighlightedRow
import org.dergigi.boris.ui.home.HomeHighlightsState
import org.dergigi.boris.ui.home.HomeSections
import org.dergigi.boris.ui.home.HomeViewModel
import org.dergigi.boris.ui.home.MostHighlightedWindowMenu
import org.dergigi.boris.ui.settings.SettingsViewModel
import org.dergigi.boris.ui.rememberArticleActions
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.rememberDisplayLook

enum class SearchResultType {
    All,
    Highlights,
    Writings,
    Bookmarks,
    Profiles,
}

@Composable
fun SearchScreen(
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    onOpenProfile: (pubkeyHex: String) -> Unit,
    onOpenExploreSettings: () -> Unit = {},
    initialQuery: String? = null,
    initialQueryVersion: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val submittedQuery by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val discovery by homeViewModel.highlights.collectAsStateWithLifecycle()
    val discoveryRefreshing by homeViewModel.refreshing.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val actions = rememberArticleActions()
    var draftQuery by rememberSaveable { mutableStateOf(submittedQuery) }
    var resultType by rememberSaveable { mutableStateOf(SearchResultType.All) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshRelation()
        if (submittedQuery.trim().length < 2) homeViewModel.refresh()
    }
    LaunchedEffect(initialQueryVersion, initialQuery) {
        val incoming = initialQuery?.trim()?.takeIf { it.isNotEmpty() } ?: return@LaunchedEffect
        draftQuery = incoming
        viewModel.onQueryChange(incoming)
    }
    val look = rememberDisplayLook(settings)
    val mineColor = look.mine
    val friendsColor = look.friends
    val foafColor = look.foaf
    val nostrverseColor = look.nostrverse
    SearchScreenContent(
        query = draftQuery,
        submittedQuery = submittedQuery,
        results = state.results,
        searchRefreshing = state.isLoading ||
            (submittedQuery.trim().length >= 2 && state.query != submittedQuery.trim()),
        discovery = discovery,
        discoveryRefreshing = discoveryRefreshing,
        discoverySectionOrder = HomeSections.visible(
            HomeSections.exploreOrder(
                settings.exploreSectionOrder,
                settings.homeSectionOrder,
            ),
            settings.exploreHiddenSections,
        ),
        resultType = resultType,
        settings = settings,
        actions = actions,
        onQueryChange = { value ->
            draftQuery = value
            if (value.trim().isEmpty()) viewModel.clear()
        },
        onSubmitSearch = { viewModel.onQueryChange(draftQuery) },
        onSelectResultType = { resultType = it },
        colorFor = { hit ->
            when {
                hit.mine -> mineColor
                hit.friend -> friendsColor
                hit.foaf -> foafColor
                else -> nostrverseColor
            }
        },
        friendsColor = friendsColor,
        foafColor = foafColor,
        nostrverseColor = nostrverseColor,
        eink = look.eink,
        onOpenHit = { hit ->
            when (hit) {
                is LocalSearch.Hit.Highlight -> {
                    val url = hit.url ?: return@SearchScreenContent
                    onOpenHighlight(url, hit.eventId, hit.quote)
                }
                is LocalSearch.Hit.Article -> onOpenArticle(hit.url)
                is LocalSearch.Hit.Bookmark -> onOpenArticle(hit.url)
                is LocalSearch.Hit.Person -> onOpenProfile(hit.pubkeyHex)
            }
        },
        onOpenProfile = onOpenProfile,
        onOpenArticle = onOpenArticle,
        onOpenExploreSettings = onOpenExploreSettings,
        mostWindow = settings.mostHighlightedWindow,
        onSelectMostWindow = { window ->
            settingsViewModel.update {
                it.withString("mostHighlightedWindow", window.id)
            }
            homeViewModel.refreshMostHighlighted()
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreenContent(
    query: String,
    submittedQuery: String = query,
    results: List<LocalSearch.Hit>,
    searchRefreshing: Boolean = false,
    discovery: HomeHighlightsState,
    discoveryRefreshing: Boolean,
    discoverySectionOrder: List<String>,
    resultType: SearchResultType,
    settings: UserSettings,
    actions: ArticleActionHandlers,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit = {},
    onSelectResultType: (SearchResultType) -> Unit,
    colorFor: (LocalSearch.Hit.Highlight) -> Color,
    friendsColor: Color,
    foafColor: Color,
    nostrverseColor: Color,
    eink: Boolean,
    onOpenHit: (LocalSearch.Hit) -> Unit,
    onOpenProfile: (pubkeyHex: String) -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenExploreSettings: () -> Unit = {},
    mostWindow: MostHighlightedWindow,
    onSelectMostWindow: (MostHighlightedWindow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searching = submittedQuery.trim().length >= 2
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                actions = {
                    TopBarRefreshIndicator(
                        refreshing = if (searching) searchRefreshing else discoveryRefreshing,
                    )
                    ContentFilterMenu(settings = settings)
                    TopBarMoreMenu(
                        items = listOf(
                            TopBarMenuItem(
                                label = stringResource(R.string.explore_settings),
                                icon = Icons.Outlined.Settings,
                                onClick = onOpenExploreSettings,
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchBarField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSubmitSearch,
                modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
            )
            if (searching) {
                SearchResultFilters(
                    selected = resultType,
                    onSelect = onSelectResultType,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            val visibleResults = remember(results, submittedQuery, resultType, settings, actions.archivedKeys) {
                results
                    .filter { resultType.includes(it) }
                    .filter { LocalSearch.hitMatches(it, submittedQuery) }
                    .filter { hit ->
                        searchHitVisible(
                            hit = hit,
                            settings = settings,
                            archivedKeys = actions.archivedKeys,
                        )
                    }
                    .take(LocalSearch.DEFAULT_LIMIT)
            }
            when {
                !searching -> {
                    ExploreDiscoveryContent(
                        highlights = discovery,
                        sectionOrder = discoverySectionOrder,
                        settings = settings,
                        actions = actions,
                        friendsColor = friendsColor,
                        foafColor = foafColor,
                        nostrverseColor = nostrverseColor,
                        onOpenArticle = onOpenArticle,
                        mostWindow = mostWindow,
                        onSelectMostWindow = onSelectMostWindow,
                    )
                }
                visibleResults.isEmpty() && searchRefreshing -> {
                    SearchLoadingHint()
                }
                visibleResults.isEmpty() -> {
                    SearchHint(stringResource(R.string.search_empty))
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(visibleResults, key = { it.id }) { hit ->
                            when (hit) {
                                is LocalSearch.Hit.Highlight -> {
                                    SearchHighlightCard(
                                        hit = hit,
                                        color = colorFor(hit),
                                        eink = eink,
                                        onOpen = { onOpenHit(hit) },
                                        onOpenProfile = { onOpenProfile(hit.authorHex) },
                                    )
                                }
                                is LocalSearch.Hit.Person -> {
                                    AuthorCard(
                                        displayName = hit.title,
                                        about = hit.subtitle,
                                        pictureUrl = hit.pictureUrl,
                                        onClick = { onOpenHit(hit) },
                                    )
                                }
                                is LocalSearch.Hit.Article -> {
                                    ArticleRow(
                                        title = hit.title,
                                        summary = hit.subtitle,
                                        imageUrl = hit.imageUrl,
                                        byline = hit.authorName,
                                        bylinePicture = hit.authorPicture,
                                        bylineFallbackIcon = Icons.Outlined.AccountCircle,
                                        publishedAt = hit.sortAt,
                                        url = hit.url,
                                        onClick = { onOpenHit(hit) },
                                    )
                                }
                                is LocalSearch.Hit.Bookmark -> {
                                    ArticleRow(
                                        title = hit.title,
                                        imageUrl = hit.imageUrl,
                                        imageFallbackIcon = Icons.Outlined.Bookmark,
                                        byline = hit.subtitle,
                                        publishedAt = hit.sortAt,
                                        url = hit.url,
                                        onClick = { onOpenHit(hit) },
                                    )
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
private fun ExploreDiscoveryContent(
    highlights: HomeHighlightsState,
    sectionOrder: List<String>,
    settings: UserSettings,
    actions: ArticleActionHandlers,
    friendsColor: Color,
    foafColor: Color,
    nostrverseColor: Color,
    onOpenArticle: (String) -> Unit,
    mostWindow: MostHighlightedWindow,
    onSelectMostWindow: (MostHighlightedWindow) -> Unit,
) {
    when (highlights) {
        HomeHighlightsState.Loading -> ExploreDiscoveryStatus { SearchLoadingHint() }
        HomeHighlightsState.Error -> ExploreDiscoveryStatus {
            SearchHint(stringResource(R.string.feed_error))
        }
        HomeHighlightsState.Empty -> ExploreDiscoveryStatus {
            SearchHint(stringResource(R.string.feed_empty))
        }
        is HomeHighlightsState.Ready -> {
            val progressVersion by ReadingPositionStore.version.collectAsStateWithLifecycle()
            val archivedKeys = highlights.archivedKeys + actions.archivedKeys
            val discoveryRows = rememberDiscoveryRows(
                highlights = highlights,
                sectionOrder = sectionOrder,
                archivedKeys = archivedKeys,
                settings = settings,
                progressVersion = progressVersion,
            )
            val friends = discoveryRows[HomeSections.FRIENDS].orEmpty()
            val foaf = discoveryRows[HomeSections.FOAF].orEmpty()
            val others = discoveryRows[HomeSections.OTHERS].orEmpty()
            val likedFriends = discoveryRows[HomeSections.LIKED_FRIENDS].orEmpty()
            val likedFoaf = discoveryRows[HomeSections.LIKED_FOAF].orEmpty()
            val likedOthers = discoveryRows[HomeSections.LIKED_OTHERS].orEmpty()
            val readFriends = discoveryRows[HomeSections.READ_FRIENDS].orEmpty()
            val readFoaf = discoveryRows[HomeSections.READ_FOAF].orEmpty()
            val readOthers = discoveryRows[HomeSections.READ_OTHERS].orEmpty()
            val mostHighlighted = discoveryRows[HomeSections.MOST].orEmpty()
            val empty = discoveryRows.values.all { it.isEmpty() } && !highlights.hasMostPool
            if (empty) {
                ExploreDiscoveryStatus {
                    SearchHint(
                        stringResource(
                            if (settings.hideCompletedOnHome || settings.hideNsfwOnHome ||
                                (settings.hideArchivedOnHome && archivedKeys.isNotEmpty())
                            ) {
                                R.string.home_empty_filters
                            } else {
                                R.string.feed_empty
                            },
                        ),
                    )
                }
                return
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                sectionOrder.forEach { section ->
                    when (section) {
                        HomeSections.FRIENDS -> if (friends.isNotEmpty()) {
                            HighlightedRow(
                                title = stringResource(R.string.home_recently_highlighted_by_friends),
                                items = friends,
                                rowKey = "explore-friends",
                                tint = friendsColor,
                                loggedIn = actions.loggedIn,
                                archivedKeys = archivedKeys,
                                onRead = onOpenArticle,
                                onListen = { actions.onListen(it.url) },
                                onMarkAsRead = { actions.onMarkAsRead(it.url, it.title, it.imageUrl) },
                            )
                        }
                        HomeSections.FOAF -> if (foaf.isNotEmpty()) {
                            HighlightedRow(
                                title = stringResource(R.string.home_recently_highlighted_by_foaf),
                                items = foaf,
                                rowKey = "explore-foaf",
                                tint = foafColor,
                                loggedIn = actions.loggedIn,
                                archivedKeys = archivedKeys,
                                onRead = onOpenArticle,
                                onListen = { actions.onListen(it.url) },
                                onMarkAsRead = { actions.onMarkAsRead(it.url, it.title, it.imageUrl) },
                            )
                        }
                        HomeSections.LIKED_FRIENDS -> if (likedFriends.isNotEmpty()) {
                            HighlightedRow(
                                title = stringResource(R.string.home_liked_by_friends),
                                items = likedFriends,
                                rowKey = "explore-liked-friends",
                                icon = Icons.Outlined.FavoriteBorder,
                                tint = friendsColor,
                                loggedIn = actions.loggedIn,
                                archivedKeys = archivedKeys,
                                onRead = onOpenArticle,
                                onListen = { actions.onListen(it.url) },
                                onMarkAsRead = { actions.onMarkAsRead(it.url, it.title, it.imageUrl) },
                            )
                        }
                        HomeSections.READ_FRIENDS -> if (readFriends.isNotEmpty()) {
                            HighlightedRow(
                                title = stringResource(R.string.home_recently_read_by_friends),
                                items = readFriends,
                                rowKey = "explore-read-friends",
                                tint = friendsColor,
                                loggedIn = actions.loggedIn,
                                archivedKeys = archivedKeys,
                                onRead = onOpenArticle,
                                onListen = { actions.onListen(it.url) },
                                onMarkAsRead = { actions.onMarkAsRead(it.url, it.title, it.imageUrl) },
                            )
                        }
                        HomeSections.LIKED_FOAF -> if (likedFoaf.isNotEmpty()) {
                            HighlightedRow(
                                title = stringResource(R.string.home_liked_by_foaf),
                                items = likedFoaf,
                                rowKey = "explore-liked-foaf",
                                icon = Icons.Outlined.FavoriteBorder,
                                tint = foafColor,
                                loggedIn = actions.loggedIn,
                                archivedKeys = archivedKeys,
                                onRead = onOpenArticle,
                                onListen = { actions.onListen(it.url) },
                                onMarkAsRead = { actions.onMarkAsRead(it.url, it.title, it.imageUrl) },
                            )
                        }
                        HomeSections.READ_FOAF -> if (readFoaf.isNotEmpty()) {
                            HighlightedRow(
                                title = stringResource(R.string.home_recently_read_by_foaf),
                                items = readFoaf,
                                rowKey = "explore-read-foaf",
                                tint = foafColor,
                                loggedIn = actions.loggedIn,
                                archivedKeys = archivedKeys,
                                onRead = onOpenArticle,
                                onListen = { actions.onListen(it.url) },
                                onMarkAsRead = { actions.onMarkAsRead(it.url, it.title, it.imageUrl) },
                            )
                        }
                        HomeSections.OTHERS -> if (others.isNotEmpty()) {
                            HighlightedRow(
                                title = stringResource(
                                    if (highlights.loggedIn || friends.isNotEmpty() || foaf.isNotEmpty()) {
                                        R.string.home_recently_highlighted_by_others
                                    } else {
                                        R.string.home_recently_highlighted
                                    },
                                ),
                                items = others,
                                rowKey = "explore-others",
                                tint = nostrverseColor,
                                loggedIn = actions.loggedIn,
                                archivedKeys = archivedKeys,
                                onRead = onOpenArticle,
                                onListen = { actions.onListen(it.url) },
                                onMarkAsRead = { actions.onMarkAsRead(it.url, it.title, it.imageUrl) },
                            )
                        }
                        HomeSections.LIKED_OTHERS -> if (likedOthers.isNotEmpty()) {
                            HighlightedRow(
                                title = stringResource(R.string.home_liked_by_others),
                                items = likedOthers,
                                rowKey = "explore-liked-others",
                                icon = Icons.Outlined.FavoriteBorder,
                                tint = nostrverseColor,
                                loggedIn = actions.loggedIn,
                                archivedKeys = archivedKeys,
                                onRead = onOpenArticle,
                                onListen = { actions.onListen(it.url) },
                                onMarkAsRead = { actions.onMarkAsRead(it.url, it.title, it.imageUrl) },
                            )
                        }
                        HomeSections.READ_OTHERS -> if (readOthers.isNotEmpty()) {
                            HighlightedRow(
                                title = stringResource(R.string.home_recently_read_by_others),
                                items = readOthers,
                                rowKey = "explore-read-others",
                                tint = nostrverseColor,
                                loggedIn = actions.loggedIn,
                                archivedKeys = archivedKeys,
                                onRead = onOpenArticle,
                                onListen = { actions.onListen(it.url) },
                                onMarkAsRead = { actions.onMarkAsRead(it.url, it.title, it.imageUrl) },
                            )
                        }
                        HomeSections.MOST -> if (mostHighlighted.isNotEmpty() || highlights.hasMostPool) {
                            HighlightedRow(
                                title = stringResource(R.string.home_most_highlighted),
                                items = mostHighlighted,
                                rowKey = "explore-most",
                                tint = nostrverseColor,
                                loggedIn = actions.loggedIn,
                                archivedKeys = archivedKeys,
                                onRead = onOpenArticle,
                                onListen = { actions.onListen(it.url) },
                                onMarkAsRead = { actions.onMarkAsRead(it.url, it.title, it.imageUrl) },
                                emptyText = stringResource(R.string.home_most_highlighted_empty),
                                headerTrailing = {
                                    MostHighlightedWindowMenu(
                                        selected = mostWindow,
                                        onSelect = onSelectMostWindow,
                                    )
                                },
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
private fun ExploreDiscoveryStatus(
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        content()
    }
}

@Composable
private fun rememberDiscoveryRows(
    highlights: HomeHighlightsState.Ready,
    sectionOrder: List<String>,
    archivedKeys: Set<String>,
    settings: UserSettings,
    progressVersion: Int,
): Map<String, List<HighlightedArticle>> = remember(
    highlights.friends,
    highlights.likedFriends,
    highlights.readFriends,
    highlights.foaf,
    highlights.likedFoaf,
    highlights.readFoaf,
    highlights.others,
    highlights.likedOthers,
    highlights.readOthers,
    highlights.mostHighlighted,
    sectionOrder,
    archivedKeys,
    settings.hideArchivedOnHome,
    settings.hideCompletedOnHome,
    settings.hideNsfwOnHome,
    progressVersion,
) {
    fun visible(items: List<HighlightedArticle>) = HomeFilters.visible(
        items,
        archivedKeys,
        hideArchived = settings.hideArchivedOnHome,
        hideCompleted = settings.hideCompletedOnHome,
        hideNsfw = settings.hideNsfwOnHome,
    )
    ExploreRows.fill(
        order = sectionOrder,
        rows = mapOf(
            HomeSections.FRIENDS to visible(highlights.friends),
            HomeSections.LIKED_FRIENDS to visible(highlights.likedFriends),
            HomeSections.READ_FRIENDS to visible(highlights.readFriends),
            HomeSections.FOAF to visible(highlights.foaf),
            HomeSections.LIKED_FOAF to visible(highlights.likedFoaf),
            HomeSections.READ_FOAF to visible(highlights.readFoaf),
            HomeSections.OTHERS to visible(highlights.others),
            HomeSections.LIKED_OTHERS to visible(highlights.likedOthers),
            HomeSections.READ_OTHERS to visible(highlights.readOthers),
            HomeSections.MOST to visible(highlights.mostHighlighted),
        ),
    )
}

@Composable
private fun SearchResultFilters(
    selected: SearchResultType,
    onSelect: (SearchResultType) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChipRow(modifier = modifier) {
        ContentTabChip(
            selected = selected == SearchResultType.All,
            label = stringResource(R.string.library_all),
            icon = Icons.Outlined.Apps,
            onClick = { onSelect(SearchResultType.All) },
        )
        ContentTabChip(
            selected = selected == SearchResultType.Highlights,
            label = stringResource(R.string.search_filter_highlights),
            icon = BorisIcons.Highlighter,
            onClick = { onSelect(SearchResultType.Highlights) },
        )
        ContentTabChip(
            selected = selected == SearchResultType.Writings,
            label = stringResource(R.string.search_filter_writings),
            icon = Icons.AutoMirrored.Outlined.Article,
            onClick = { onSelect(SearchResultType.Writings) },
        )
        ContentTabChip(
            selected = selected == SearchResultType.Bookmarks,
            label = stringResource(R.string.search_filter_bookmarks),
            icon = Icons.Outlined.Bookmark,
            onClick = { onSelect(SearchResultType.Bookmarks) },
        )
        ContentTabChip(
            selected = selected == SearchResultType.Profiles,
            label = stringResource(R.string.search_filter_profiles),
            icon = Icons.Outlined.AccountCircle,
            onClick = { onSelect(SearchResultType.Profiles) },
        )
    }
}

private fun SearchResultType.includes(hit: LocalSearch.Hit): Boolean = when (this) {
    SearchResultType.All -> true
    SearchResultType.Highlights -> hit is LocalSearch.Hit.Highlight
    SearchResultType.Writings -> hit is LocalSearch.Hit.Article
    SearchResultType.Bookmarks -> hit is LocalSearch.Hit.Bookmark
    SearchResultType.Profiles -> hit is LocalSearch.Hit.Person
}

internal fun searchHitVisible(
    hit: LocalSearch.Hit,
    settings: UserSettings,
    archivedKeys: Set<String>,
): Boolean {
    val content = when (hit) {
        is LocalSearch.Hit.Highlight -> Triple(hit.url, hit.quote, hit.context)
        is LocalSearch.Hit.Article -> Triple(hit.url, hit.title, hit.subtitle)
        is LocalSearch.Hit.Bookmark -> Triple(hit.url, hit.title, hit.subtitle)
        is LocalSearch.Hit.Person -> return true
    }
    return HomeFilters.visible(
        url = content.first,
        title = content.second,
        summary = content.third,
        archivedKeys = archivedKeys,
        hideArchived = settings.hideArchivedOnHome,
        hideCompleted = settings.hideCompletedOnHome,
        hideNsfw = settings.hideNsfwOnHome,
    )
}

@Composable
private fun SearchHighlightCard(
    hit: LocalSearch.Hit.Highlight,
    color: Color,
    eink: Boolean,
    onOpen: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    HighlightCard(
        quote = hit.quote,
        context = hit.context,
        comment = hit.comment,
        color = color,
        createdAt = hit.sortAt,
        authorName = hit.authorName,
        host = hit.host,
        url = hit.url,
        authorPicture = hit.authorPicture,
        maxQuoteLines = 8,
        eink = eink,
        onClick = hit.url?.let { onOpen },
        menu = HighlightCardMenu(
            highlightId = hit.eventId,
            authorHex = hit.authorHex,
            onGoToQuote = hit.url?.let { onOpen },
            onViewProfile = onOpenProfile,
        ),
    )
}

@Composable
private fun SearchLoadingHint() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun SearchHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
