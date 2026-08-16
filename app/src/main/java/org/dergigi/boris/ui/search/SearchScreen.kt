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
import androidx.compose.material.icons.outlined.FormatQuote
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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

@Composable
fun SearchScreen(
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, quote: String) -> Unit,
    onOpenProfile: (pubkeyHex: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreenContent(
        query = query,
        results = state.results,
        onQueryChange = viewModel::onQueryChange,
        onClear = viewModel::clear,
        onOpenHit = { hit ->
            when (hit) {
                is LocalSearch.Hit.Highlight -> {
                    val url = hit.url ?: return@SearchScreenContent
                    onOpenHighlight(url, hit.quote)
                }
                is LocalSearch.Hit.Article -> onOpenArticle(hit.url)
                is LocalSearch.Hit.Bookmark -> onOpenArticle(hit.url)
                is LocalSearch.Hit.Person -> onOpenProfile(hit.pubkeyHex)
            }
        },
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
    onOpenHit: (LocalSearch.Hit) -> Unit,
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
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(results, key = { it.id }) { hit ->
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
        is LocalSearch.Hit.Highlight ->
            stringResource(R.string.search_kind_highlight) to Icons.Outlined.FormatQuote
        is LocalSearch.Hit.Article ->
            stringResource(R.string.search_kind_article) to Icons.AutoMirrored.Outlined.MenuBook
        is LocalSearch.Hit.Bookmark ->
            stringResource(R.string.search_kind_bookmark) to Icons.Outlined.Bookmark
        is LocalSearch.Hit.Person ->
            stringResource(R.string.search_kind_person) to Icons.Outlined.Person
    }
    val clickable = when (hit) {
        is LocalSearch.Hit.Highlight -> hit.url != null
        else -> true
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
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
