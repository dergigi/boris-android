package org.dergigi.boris.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.ui.HighlightCard
import org.dergigi.boris.ui.HighlightCardMenu
import org.dergigi.boris.ui.feed.FeedLevel
import org.dergigi.boris.ui.feed.FeedScope

internal fun highlightFilter(
    settings: UserSettings,
    highlights: List<PaintedHighlight> = emptyList(),
): FeedScope {
    val base = FeedScope(
        nostrverse = settings.defaultHighlightVisibilityNostrverse,
        friends = settings.defaultHighlightVisibilityFriends,
        mine = settings.defaultHighlightVisibilityMine,
        foaf = settings.defaultHighlightVisibilityFoaf,
    )
    if (highlights.isEmpty() || highlights.any(base::shows)) return base
    // Match the highlights pill priority so opening the pane never lands on an empty filter.
    return when {
        highlights.any { it.mine } -> base.copy(mine = true)
        highlights.any { it.friend } -> base.copy(friends = true)
        highlights.any { it.foaf } -> base.copy(foaf = true)
        else -> base.copy(nostrverse = true)
    }
}

internal fun FeedScope.shows(item: PaintedHighlight): Boolean = when {
    item.mine -> mine
    item.friend -> friends
    item.foaf -> foaf
    else -> nostrverse
}

@Composable
fun HighlightsPane(
    open: Boolean,
    highlights: List<PaintedHighlight>,
    selectedId: String?,
    loggedIn: Boolean,
    settings: UserSettings,
    mineColor: Color,
    friendsColor: Color,
    foafColor: Color,
    otherColor: Color,
    onDismiss: () -> Unit,
    onSelect: (PaintedHighlight) -> Unit,
    onOpenHighlightSettings: () -> Unit = {},
    onToggleMarks: () -> Unit,
    articleUrl: String? = null,
    articleTexts: List<String> = emptyList(),
    // Provider, not value: it tracks the sliding top bar per frame, and the
    // read must stay inside this pane's scope, not the article tree (#131).
    topPadding: () -> Dp = { 0.dp },
    menuFor: (PaintedHighlight) -> HighlightCardMenu,
) {
    var filter by remember(open) { mutableStateOf(highlightFilter(settings, highlights)) }
    val ordered = remember(highlights, articleTexts) {
        HighlightJump.inDocumentOrder(highlights, articleTexts)
    }
    val visible = remember(ordered, filter) { ordered.filter(filter::shows) }
    LaunchedEffect(open, highlights) {
        if (!open || highlights.isEmpty() || highlights.any(filter::shows)) return@LaunchedEffect
        filter = highlightFilter(settings, highlights)
    }
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
                    .padding(top = topPadding())
                    .animateEnterExit(
                        enter = slideInHorizontally { it },
                        exit = slideOutHorizontally { it },
                    )
                    .fillMaxHeight()
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 400.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    HighlightsPaneHeader(
                        loggedIn = loggedIn,
                        filter = filter,
                        showMarks = settings.showHighlights,
                        mineColor = mineColor,
                        friendsColor = friendsColor,
                        foafColor = foafColor,
                        otherColor = otherColor,
                        onDismiss = onDismiss,
                        onToggleNostrverse = { filter = filter.toggle(FeedLevel.Nostrverse) },
                        onToggleFoaf = { filter = filter.toggle(FeedLevel.Foaf) },
                        onToggleFriends = { filter = filter.toggle(FeedLevel.Friends) },
                        onToggleMine = { filter = filter.toggle(FeedLevel.Mine) },
                        onOpenHighlightSettings = onOpenHighlightSettings,
                        onToggleMarks = onToggleMarks,
                    )
                    if (visible.isEmpty()) {
                        Text(
                            text = stringResource(
                                if (highlights.isEmpty()) {
                                    R.string.reader_highlights_empty
                                } else {
                                    R.string.feed_empty_filtered
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    } else {
                        val listState = rememberLazyListState()
                        LaunchedEffect(selectedId, visible) {
                            val index = visible.indexOfFirst { it.id == selectedId }
                            if (index >= 0) listState.animateScrollToItem(index)
                        }
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(visible, key = { it.id }) { item ->
                                val name = item.authorName.ifBlank {
                                    Profile.displayName(item.pubkey, null)
                                }
                                HighlightCard(
                                    quote = item.quote,
                                    color = when {
                                        item.mine -> mineColor
                                        item.friend -> friendsColor
                                        item.foaf -> foafColor
                                        else -> otherColor
                                    },
                                    createdAt = item.createdAt,
                                    authorName = name,
                                    context = item.context,
                                    comment = item.comment,
                                    url = articleUrl,
                                    authorPicture = item.authorPicture,
                                    selected = item.id == selectedId,
                                    onClick = { onSelect(item) },
                                    menu = menuFor(item),
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
private fun HighlightsPaneHeader(
    loggedIn: Boolean,
    filter: FeedScope,
    showMarks: Boolean,
    mineColor: Color,
    friendsColor: Color,
    foafColor: Color,
    otherColor: Color,
    onDismiss: () -> Unit,
    onToggleNostrverse: () -> Unit,
    onToggleFoaf: () -> Unit,
    onToggleFriends: () -> Unit,
    onToggleMine: () -> Unit,
    onOpenHighlightSettings: () -> Unit,
    onToggleMarks: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.reader_highlights_close),
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterIcon(
                icon = Icons.Outlined.Hub,
                on = filter.nostrverse,
                tint = otherColor,
                contentDescription = stringResource(R.string.feed_scope_nostrverse),
                onClick = onToggleNostrverse,
            )
            if (loggedIn) {
                FilterIcon(
                    icon = Icons.Outlined.Groups,
                    on = filter.foaf,
                    tint = foafColor,
                    contentDescription = stringResource(R.string.feed_scope_foaf),
                    onClick = onToggleFoaf,
                )
                FilterIcon(
                    icon = Icons.Outlined.Group,
                    on = filter.friends,
                    tint = friendsColor,
                    contentDescription = stringResource(R.string.feed_scope_friends),
                    onClick = onToggleFriends,
                )
                FilterIcon(
                    icon = Icons.Outlined.Person,
                    on = filter.mine,
                    tint = mineColor,
                    contentDescription = stringResource(R.string.feed_scope_mine),
                    onClick = onToggleMine,
                )
            }
        }
        IconButton(onClick = onToggleMarks) {
            Icon(
                imageVector = if (showMarks) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = stringResource(
                    if (showMarks) R.string.reader_highlights_hide else R.string.reader_highlights_show,
                ),
            )
        }
        IconButton(onClick = onOpenHighlightSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.reader_highlights_settings),
            )
        }
    }
}

@Composable
private fun FilterIcon(
    icon: ImageVector,
    on: Boolean,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (on) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        )
    }
}
