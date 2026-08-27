package org.dergigi.boris.ui.search

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.R
import org.dergigi.boris.data.HomeFilters
import org.dergigi.boris.data.LocalSearch
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.ArticleRow
import org.dergigi.boris.ui.AuthorCard
import org.dergigi.boris.ui.ContentFilterMenu
import org.dergigi.boris.ui.ContentTabChip
import org.dergigi.boris.ui.FilterChipRow
import org.dergigi.boris.ui.HighlightCard
import org.dergigi.boris.ui.HighlightCardMenu
import org.dergigi.boris.ui.rememberArticleActions
import org.dergigi.boris.ui.settings.hexColor
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

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
    initialQuery: String? = null,
    initialQueryVersion: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val actions = rememberArticleActions()
    var resultType by rememberSaveable { mutableStateOf(SearchResultType.All) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshRelation()
    }
    LaunchedEffect(initialQueryVersion, initialQuery) {
        initialQuery
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(viewModel::onQueryChange)
    }
    val mineColor = hexColor(settings.highlightColorMine, HighlightMine)
    val friendsColor = hexColor(settings.highlightColorFriends, HighlightFriends)
    val nostrverseColor = hexColor(settings.highlightColorNostrverse, HighlightOther)
    SearchScreenContent(
        query = query,
        results = state.results,
        resultType = resultType,
        settings = settings,
        archivedKeys = actions.archivedKeys,
        onQueryChange = viewModel::onQueryChange,
        onClear = viewModel::clear,
        onSelectResultType = { resultType = it },
        colorFor = { hit ->
            when {
                hit.mine -> mineColor
                hit.friend -> friendsColor
                else -> nostrverseColor
            }
        },
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
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreenContent(
    query: String,
    results: List<LocalSearch.Hit>,
    resultType: SearchResultType,
    settings: UserSettings,
    archivedKeys: Set<String>,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSelectResultType: (SearchResultType) -> Unit,
    colorFor: (LocalSearch.Hit.Highlight) -> Color,
    onOpenHit: (LocalSearch.Hit) -> Unit,
    onOpenProfile: (pubkeyHex: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                actions = {
                    ContentFilterMenu(settings = settings)
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
            CompactSearchField(
                query = query,
                onQueryChange = onQueryChange,
                onClear = onClear,
                onSearch = { focus.clearFocus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            )
            if (query.trim().length >= 2) {
                SearchResultFilters(
                    selected = resultType,
                    onSelect = onSelectResultType,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            val visibleResults = remember(results, resultType, settings, archivedKeys) {
                results
                    .filter { resultType.includes(it) }
                    .filter { hit ->
                        searchHitVisible(
                            hit = hit,
                            settings = settings,
                            archivedKeys = archivedKeys,
                        )
                    }
            }
            when {
                query.trim().length < 2 -> Unit
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
private fun CompactSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (focused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val searchContentDescription = stringResource(R.string.search_title)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.semantics { contentDescription = searchContentDescription },
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.SansSerif,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(shape)
                    .border(1.dp, borderColor, shape)
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.SansSerif,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = stringResource(R.string.search_clear),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SearchHighlightCard(
    hit: LocalSearch.Hit.Highlight,
    color: Color,
    onOpen: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    HighlightCard(
        quote = hit.quote,
        context = hit.context,
        color = color,
        createdAt = hit.sortAt,
        authorName = hit.authorName,
        host = hit.host,
        url = hit.url,
        authorPicture = hit.authorPicture,
        maxQuoteLines = 8,
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
