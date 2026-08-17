package org.dergigi.boris.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Clear
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.R
import org.dergigi.boris.data.LocalSearch
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.ui.ArticleRow
import org.dergigi.boris.ui.AuthorCard
import org.dergigi.boris.ui.HighlightCard
import org.dergigi.boris.ui.HighlightCardMenu
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
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshRelation()
    }
    val mineColor = hexColor(settings.highlightColorMine, HighlightMine)
    val friendsColor = hexColor(settings.highlightColorFriends, HighlightFriends)
    val nostrverseColor = hexColor(settings.highlightColorNostrverse, HighlightOther)
    SearchScreenContent(
        query = query,
        results = state.results,
        onQueryChange = viewModel::onQueryChange,
        onClear = viewModel::clear,
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
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
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
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
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
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            )
            when {
                query.trim().length < 2 -> Unit
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
