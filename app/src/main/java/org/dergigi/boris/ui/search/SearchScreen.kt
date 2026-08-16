package org.dergigi.boris.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.dergigi.boris.R
import org.dergigi.boris.data.LocalSearch
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.ui.HighlightCard
import org.dergigi.boris.ui.HighlightCardMenu
import org.dergigi.boris.ui.feed.FeedLevel
import org.dergigi.boris.ui.feed.classifyFeedLevel
import org.dergigi.boris.ui.settings.hexColor
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

@Composable
fun SearchScreen(
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    onOpenProfile: (pubkeyHex: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sessionHex = SessionStore.load(context)?.pubkeyHex?.lowercase()
    val friends = remember(sessionHex) {
        sessionHex?.let { RelayQuery.cachedContactPubkeys(it) } ?: emptySet()
    }
    val mineColor = hexColor(settings.highlightColorMine, HighlightMine)
    val friendsColor = hexColor(settings.highlightColorFriends, HighlightFriends)
    val nostrverseColor = hexColor(settings.highlightColorNostrverse, HighlightOther)
    SearchScreenContent(
        query = query,
        results = state.results,
        onQueryChange = viewModel::onQueryChange,
        onClear = viewModel::clear,
        colorFor = { authorHex ->
            when (classifyFeedLevel(authorHex, sessionHex, friends)) {
                FeedLevel.Mine -> mineColor
                FeedLevel.Friends -> friendsColor
                FeedLevel.Nostrverse -> nostrverseColor
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
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    colorFor: (authorHex: String) -> Color,
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
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = stringResource(R.string.search_clear),
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            )
            when {
                query.trim().length < 2 -> {
                    SearchHint(stringResource(R.string.search_hint))
                }
                results.isEmpty() -> {
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
                        items(results, key = { it.id }) { hit ->
                            when (hit) {
                                is LocalSearch.Hit.Highlight -> {
                                    SearchHighlightCard(
                                        hit = hit,
                                        color = colorFor(hit.authorHex),
                                        onOpen = { onOpenHit(hit) },
                                        onOpenProfile = { onOpenProfile(hit.authorHex) },
                                    )
                                }
                                else -> {
                                    SearchResultRow(
                                        hit = hit,
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

@Composable
private fun SearchResultRow(
    hit: LocalSearch.Hit,
    onClick: () -> Unit,
) {
    val (kindLabel, icon) = when (hit) {
        is LocalSearch.Hit.Highlight -> error("highlights use SearchHighlightCard")
        is LocalSearch.Hit.Article ->
            stringResource(R.string.search_kind_article) to Icons.AutoMirrored.Outlined.MenuBook
        is LocalSearch.Hit.Bookmark ->
            stringResource(R.string.search_kind_bookmark) to Icons.Outlined.Bookmark
        is LocalSearch.Hit.Person ->
            stringResource(R.string.search_kind_person) to Icons.Outlined.Person
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SearchLeading(hit = hit, fallback = icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = kindLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = hit.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            hit.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchLeading(
    hit: LocalSearch.Hit,
    fallback: ImageVector,
) {
    val picture = (hit as? LocalSearch.Hit.Person)?.pictureUrl
    if (!picture.isNullOrBlank()) {
        AsyncImage(
            model = picture,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Icon(
            imageVector = fallback,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
