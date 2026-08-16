package org.dergigi.boris.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R

@Composable
fun FindPane(
    open: Boolean,
    query: String,
    hits: List<ArticleFindHit>,
    activeIndex: Int,
    matchCount: Int,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    BackHandler(enabled = open, onBack = onDismiss)
    AnimatedVisibility(
        visible = open,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss),
            )
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .animateEnterExit(
                        enter = slideInHorizontally { it },
                        exit = slideOutHorizontally { it },
                    )
                    .fillMaxHeight()
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 400.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    FindPaneHeader(
                        query = query,
                        activeIndex = activeIndex,
                        matchCount = matchCount,
                        onQueryChange = onQueryChange,
                        onDismiss = onDismiss,
                        onPrevious = onPrevious,
                        onNext = onNext,
                    )
                    if (query.isBlank()) {
                        Text(
                            text = stringResource(R.string.reader_find_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    } else if (hits.isEmpty()) {
                        Text(
                            text = stringResource(R.string.reader_find_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    } else {
                        val listState = rememberLazyListState()
                        LaunchedEffect(activeIndex, hits) {
                            if (activeIndex in hits.indices) {
                                listState.animateScrollToItem(activeIndex)
                            }
                        }
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(hits, key = { it.index }) { hit ->
                                FindHitRow(
                                    hit = hit,
                                    selected = hit.index == activeIndex,
                                    onClick = { onSelect(hit.index) },
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
private fun FindPaneHeader(
    query: String,
    activeIndex: Int,
    matchCount: Int,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.reader_find_close),
                )
            }
            Text(
                text = stringResource(R.string.reader_find_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onPrevious,
                enabled = matchCount > 0,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.reader_find_previous),
                )
            }
            IconButton(
                onClick = onNext,
                enabled = matchCount > 0,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.reader_find_next),
                )
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null)
            },
            placeholder = { Text(stringResource(R.string.reader_find_placeholder)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onNext() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .focusRequester(focus),
        )
        if (query.isNotBlank()) {
            Text(
                text = if (matchCount == 0) {
                    stringResource(R.string.reader_find_empty)
                } else {
                    stringResource(
                        R.string.reader_find_count,
                        (activeIndex + 1).coerceAtMost(matchCount),
                        matchCount,
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun FindHitRow(
    hit: ArticleFindHit,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            text = hit.snippet,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
