package org.dergigi.boris.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R

@Composable
fun OutlinePane(
    open: Boolean,
    items: List<ArticleOutlineItem>,
    activeId: String?,
    onDismiss: () -> Unit,
    onSelect: (ArticleOutlineItem) -> Unit,
    topPadding: Dp = 0.dp,
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
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = topPadding)
                    .animateEnterExit(
                        enter = slideInHorizontally { -it },
                        exit = slideOutHorizontally { -it },
                    )
                    .fillMaxHeight()
                    .fillMaxWidth(0.78f)
                    .widthIn(max = 400.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinePaneHeader(onDismiss = onDismiss)
                    val listState = rememberLazyListState()
                    LaunchedEffect(activeId, items) {
                        val index = items.indexOfFirst { it.id == activeId }
                        if (index >= 0) listState.animateScrollToItem(index)
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(items, key = { it.id }) { item ->
                            OutlineHeadingRow(
                                item = item,
                                selected = item.id == activeId,
                                onClick = { onSelect(item) },
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlinePaneHeader(onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.reader_outline_close),
            )
        }
        Text(
            text = stringResource(R.string.reader_outline_title),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun OutlineHeadingRow(
    item: ArticleOutlineItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val indent = ((item.level - 1).coerceAtLeast(0) * 12).dp
    Text(
        text = item.title,
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp + indent, end = 20.dp, top = 14.dp, bottom = 14.dp),
    )
}
