package org.dergigi.boris.ui.you

import org.dergigi.boris.ui.SignerEffects
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.dergigi.boris.R
import org.dergigi.boris.data.BookmarkItem
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.ui.AuthorCard
import org.dergigi.boris.ui.PullToRefresh
import org.dergigi.boris.ui.ArticleActionHandlers
import org.dergigi.boris.ui.ArticleRowWithMenu
import org.dergigi.boris.ui.bookmarkFallbackIcon
import org.dergigi.boris.ui.ContentTab
import org.dergigi.boris.ui.rememberArticleActions
import org.dergigi.boris.ui.ContentTabs
import org.dergigi.boris.ui.HighlightCard
import org.dergigi.boris.ui.HighlightCardMenu
import org.dergigi.boris.ui.HighlightMenuViewModel
import org.dergigi.boris.ui.feed.FeedLevel
import org.dergigi.boris.ui.theme.rememberDisplayLook

@Composable
fun YouHighlights(
    npub: String,
    profile: Profile?,
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit = { url, _, _ ->
        onOpenArticle(url)
    },
    modifier: Modifier = Modifier,
    viewModel: YouViewModel = viewModel(),
    menuViewModel: HighlightMenuViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val fetchedProfile by viewModel.profile.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val relation by viewModel.relation.collectAsStateWithLifecycle()
    val deletedIds by menuViewModel.deleted.collectAsStateWithLifecycle()
    val menuSignIntent by menuViewModel.signIntent.collectAsStateWithLifecycle()
    val menuMessage by menuViewModel.message.collectAsStateWithLifecycle()
    SignerEffects(
        signIntent = menuSignIntent,
        message = menuMessage,
        onConsumeSignIntent = menuViewModel::consumeSignIntent,
        onConsumeMessage = menuViewModel::consumeMessage,
        onSignerResult = menuViewModel::onSignerResult,
    )
    val authorHex = remember(npub) { runCatching { Nip19.npubDecode(npub) }.getOrNull() }
    val shown = profile ?: fetchedProfile
    val look = rememberDisplayLook(settings)
    val highlightColor = when (relation) {
        FeedLevel.Mine -> look.mine
        FeedLevel.Friends -> look.friends
        FeedLevel.Foaf -> look.foaf
        FeedLevel.Nostrverse -> look.nostrverse
    }
    val displayName = shown?.name?.takeIf { it.isNotBlank() } ?: shortNpub(npub)
    val actions = rememberArticleActions()
    LaunchedEffect(npub) {
        val hex = runCatching { Nip19.npubDecode(npub) }.getOrNull() ?: return@LaunchedEffect
        viewModel.refresh(hex)
    }
    val loadingMore by viewModel.loadingMore.collectAsStateWithLifecycle()
    val endReached by viewModel.endReached.collectAsStateWithLifecycle()
    YouHighlightsContent(
        state = state,
        refreshing = refreshing,
        displayName = displayName,
        about = shown?.about,
        pictureUrl = shown?.picture,
        highlightColor = highlightColor,
        onRefresh = { tab -> viewModel.refresh(tab = tab) },
        loadingMore = loadingMore,
        endReached = endReached,
        onLoadMore = { viewModel.loadMoreHighlights() },
        onOpenArticle = onOpenArticle,
        onOpenHighlight = onOpenHighlight,
        actions = actions,
        deletedIds = deletedIds,
        menuFor = { item ->
            HighlightCardMenu(
                highlightId = item.id,
                authorHex = authorHex,
                onGoToQuote = item.url?.let { url ->
                    { onOpenHighlight(url, item.id, item.quote) }
                },
                onDelete = if (menuViewModel.canDelete(authorHex)) {
                    { menuViewModel.delete(item.id) }
                } else {
                    null
                },
            )
        },
        modifier = modifier,
    )
}

@Composable
fun YouHighlightsContent(
    state: YouUiState,
    refreshing: Boolean,
    displayName: String,
    about: String?,
    pictureUrl: String?,
    highlightColor: Color,
    onRefresh: (ContentTab?) -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    actions: ArticleActionHandlers,
    modifier: Modifier = Modifier,
    loadingMore: Boolean = false,
    endReached: Boolean = false,
    onLoadMore: () -> Unit = {},
    deletedIds: Set<String> = emptySet(),
    menuFor: (YouHighlight) -> HighlightCardMenu? = { null },
) {
    var tab by rememberSaveable { mutableStateOf(ContentTab.All) }
    var query by rememberSaveable { mutableStateOf("") }
    PullToRefresh(
        isRefreshing = refreshing,
        onRefresh = { onRefresh(tab) },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                AuthorCard(
                    displayName = displayName,
                    about = about,
                    pictureUrl = pictureUrl,
                )
            }
            item(key = "tabs") {
                ContentTabs(
                    tab = tab,
                    onSelect = { tab = it },
                    showAll = true,
                    showArticles = true,
                    showBookmarks = true,
                )
            }
            if (state is YouUiState.Ready) {
                item(key = "search") {
                    ProfileSearchField(
                        query = query,
                        onQueryChange = { query = it },
                    )
                }
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
                YouUiState.Error -> {
                    item(key = "error") {
                        StatusMessage(
                            text = stringResource(R.string.feed_error),
                            onRetry = { onRefresh(null) },
                        )
                    }
                }
                is YouUiState.Ready -> {
                    when (tab) {
                        ContentTab.All -> {
                            val merged = mergeYouItems(
                                highlights = state.highlights,
                                writings = state.writings,
                                publicBookmarks = state.publicBookmarks,
                                webBookmarks = state.webBookmarks,
                                associatedArticles = state.associatedArticles,
                                deletedIds = deletedIds,
                                query = query,
                            )
                            if (merged.isEmpty()) {
                                item(key = "empty-all") {
                                    val hasAny = state.highlights.any { it.id !in deletedIds } ||
                                        state.writings.isNotEmpty() ||
                                        state.publicBookmarks.isNotEmpty() ||
                                        state.webBookmarks.isNotEmpty() ||
                                        state.associatedArticles.isNotEmpty()
                                    if (!hasAny) {
                                        StatusMessage(
                                            text = stringResource(R.string.you_all_empty),
                                            onRetry = { onRefresh(tab) },
                                        )
                                    } else {
                                        NoSearchMatches()
                                    }
                                }
                            } else {
                                items(merged, key = { it.key }) { entry ->
                                    when (entry) {
                                        is YouMergedItem.Highlight -> {
                                            val item = entry.item
                                            HighlightCard(
                                                quote = item.quote,
                                                context = item.context,
                                                color = highlightColor,
                                                createdAt = item.createdAt,
                                                authorName = displayName,
                                                host = item.host,
                                                url = item.url,
                                                authorPicture = pictureUrl,
                                                onClick = item.url?.let { url ->
                                                    { onOpenHighlight(url, item.id, item.quote) }
                                                },
                                                menu = menuFor(item),
                                            )
                                        }
                                        is YouMergedItem.Writing -> {
                                            YouWritingCard(
                                                item = entry.item,
                                                actions = actions,
                                                onOpenArticle = onOpenArticle,
                                            )
                                        }
                                        is YouMergedItem.Bookmark -> {
                                            YouBookmarkCard(
                                                item = entry.item,
                                                actions = actions,
                                                onOpenArticle = onOpenArticle,
                                                onOpenHighlight = onOpenHighlight,
                                            )
                                        }
                                    }
                                }
                            }
                            if (!endReached && state.highlights.isNotEmpty()) {
                                item(key = "load-more") {
                                    LoadMoreRow(
                                        loading = loadingMore,
                                        onLoadMore = onLoadMore,
                                    )
                                }
                            }
                        }
                        ContentTab.Highlights -> {
                            val visible = state.highlights
                                .filter { it.id !in deletedIds && it.matchesQuery(query) }
                            if (visible.isEmpty()) {
                                item(key = "empty-highlights") {
                                    if (state.highlights.none { it.id !in deletedIds }) {
                                        StatusMessage(
                                            text = stringResource(R.string.you_highlights_empty),
                                            onRetry = { onRefresh(tab) },
                                        )
                                    } else {
                                        NoSearchMatches()
                                    }
                                }
                            } else {
                                items(visible, key = { it.id }) { item ->
                                    HighlightCard(
                                        quote = item.quote,
                                        context = item.context,
                                        color = highlightColor,
                                        createdAt = item.createdAt,
                                        authorName = displayName,
                                        host = item.host,
                                        url = item.url,
                                        authorPicture = pictureUrl,
                                        onClick = item.url?.let { url ->
                                            { onOpenHighlight(url, item.id, item.quote) }
                                        },
                                        menu = menuFor(item),
                                    )
                                }
                            }
                            if (!endReached && state.highlights.isNotEmpty()) {
                                item(key = "load-more") {
                                    LoadMoreRow(
                                        loading = loadingMore,
                                        onLoadMore = onLoadMore,
                                    )
                                }
                            }
                        }
                        ContentTab.Writings -> {
                            val visible = state.writings.filter { it.matchesQuery(query) }
                            if (visible.isEmpty()) {
                                item(key = "empty-writings") {
                                    if (state.writings.isEmpty()) {
                                        StatusMessage(
                                            text = stringResource(R.string.you_writings_empty),
                                            onRetry = { onRefresh(tab) },
                                        )
                                    } else {
                                        NoSearchMatches()
                                    }
                                }
                            } else {
                                items(visible, key = { it.id }) { item ->
                                    YouWritingCard(
                                        item = item,
                                        actions = actions,
                                        onOpenArticle = onOpenArticle,
                                    )
                                }
                            }
                        }
                        ContentTab.Articles -> {
                            profileBookmarkItems(
                                items = state.associatedArticles,
                                query = query,
                                emptyRes = R.string.you_articles_empty,
                                onRefresh = { onRefresh(tab) },
                                onOpenArticle = onOpenArticle,
                                onOpenHighlight = onOpenHighlight,
                                actions = actions,
                            )
                        }
                        ContentTab.Public -> {
                            profileBookmarkItems(
                                items = state.publicBookmarks,
                                query = query,
                                emptyRes = R.string.you_public_empty,
                                onRefresh = { onRefresh(tab) },
                                onOpenArticle = onOpenArticle,
                                onOpenHighlight = onOpenHighlight,
                                actions = actions,
                            )
                        }
                        ContentTab.Web -> {
                            profileBookmarkItems(
                                items = state.webBookmarks,
                                query = query,
                                emptyRes = R.string.you_web_empty,
                                onRefresh = { onRefresh(tab) },
                                onOpenArticle = onOpenArticle,
                                onOpenHighlight = onOpenHighlight,
                                actions = actions,
                            )
                        }
                        ContentTab.Rss -> Unit
                    }
                }
            }
        }
    }
}

private fun LazyListScope.profileBookmarkItems(
    items: List<BookmarkItem>,
    query: String,
    emptyRes: Int,
    onRefresh: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    actions: ArticleActionHandlers,
) {
    val visible = items.filter { it.matchesQuery(query) }
    if (visible.isEmpty()) {
        item(key = "empty-$emptyRes") {
            if (items.isEmpty()) {
                StatusMessage(
                    text = stringResource(emptyRes),
                    onRetry = onRefresh,
                )
            } else {
                NoSearchMatches()
            }
        }
    } else {
        items(visible, key = { it.id }) { item ->
            YouBookmarkCard(
                item = item,
                actions = actions,
                onOpenArticle = onOpenArticle,
                onOpenHighlight = onOpenHighlight,
            )
        }
    }
}

@Composable
private fun YouBookmarkCard(
    item: BookmarkItem,
    actions: ArticleActionHandlers,
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
) {
    val url = item.url
    ArticleRowWithMenu(
        title = item.title,
        summary = item.summary,
        imageUrl = item.imageUrl,
        imageFallbackIcon = bookmarkFallbackIcon(item),
        byline = item.host,
        url = url,
        loggedIn = actions.loggedIn,
        archived = actions.archived(url),
        onClick = { item.open(onOpenArticle, onOpenHighlight) },
        onListen = { url?.let(actions.onListen) },
        onMarkAsRead = {
            if (url != null) actions.onMarkAsRead(url, item.title, item.imageUrl)
        },
    )
}

@Composable
private fun ProfileSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focus = LocalFocusManager.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(8.dp)
    val textStyle = MaterialTheme.typography.bodySmall.copy(
        fontFamily = FontFamily.SansSerif,
        color = MaterialTheme.colorScheme.onSurface,
    )
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
            .clip(shape)
            .padding(horizontal = 10.dp),
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = muted,
                    modifier = Modifier.size(16.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.you_search_placeholder),
                            style = textStyle,
                            color = muted.copy(alpha = 0.7f),
                        )
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.search_clear),
                        tint = muted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onQueryChange("") },
                    )
                }
            }
        },
    )
}

@Composable
private fun NoSearchMatches() {
    Text(
        text = stringResource(R.string.search_empty),
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
    )
}

@Composable
private fun LoadMoreRow(
    loading: Boolean,
    onLoadMore: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            TextButton(onClick = onLoadMore) {
                Text(text = stringResource(R.string.you_load_more))
            }
        }
    }
}

@Composable
private fun YouWritingCard(
    item: YouWriting,
    actions: ArticleActionHandlers,
    onOpenArticle: (String) -> Unit,
) {
    ArticleRowWithMenu(
        title = item.title,
        summary = item.summary,
        imageUrl = item.imageUrl,
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
