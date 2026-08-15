package org.dergigi.boris.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.dergigi.boris.R
import org.dergigi.boris.data.HighlightedArticle
import org.dergigi.boris.data.UrlExtractor

const val DEFAULT_ARTICLE_URL = "https://dergigi.com/2023/04/04/purple-text-orange-highlights/"

@Composable
fun HomeScreen(
    onRead: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    HomeScreenContent(
        highlights = highlights,
        onRead = onRead,
        modifier = modifier,
    )
}

@Composable
fun HomeScreenContent(
    highlights: HomeHighlightsState,
    onRead: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var url by rememberSaveable { mutableStateOf("") }
    val extracted = UrlExtractor.extract(url.trim().ifEmpty { DEFAULT_ARTICLE_URL })
    val canRead = extracted != null

    fun submit() {
        extracted?.let(onRead)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_hello),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(DEFAULT_ARTICLE_URL) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                )
                Button(
                    onClick = ::submit,
                    enabled = canRead,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(
                        stringResource(R.string.home_read),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            when (highlights) {
                HomeHighlightsState.Hidden -> Unit
                HomeHighlightsState.Loading -> {
                    RecentlyHighlightedSection {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(CardImageSize + 72.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                }
                is HomeHighlightsState.Ready -> {
                    RecentlyHighlightedSection {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(232.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(highlights.items, key = { it.url }) { article ->
                                HighlightedArticleCard(
                                    article = article,
                                    onOpen = { onRead(article.url) },
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
private fun RecentlyHighlightedSection(
    content: @Composable () -> Unit,
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
                imageVector = Icons.Outlined.Highlight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.home_recently_highlighted),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        content()
    }
}

@Composable
private fun HighlightedArticleCard(
    article: HighlightedArticle,
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
                    imageVector = Icons.Outlined.Highlight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
    }
}

private val CardWidth = 140.dp
private val CardImageSize = 140.dp
