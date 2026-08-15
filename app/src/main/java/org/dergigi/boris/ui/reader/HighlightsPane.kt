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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dergigi.boris.R
import org.dergigi.boris.data.RelativeTime
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.ui.feed.FeedLevel
import org.dergigi.boris.ui.feed.FeedScope
import org.dergigi.boris.ui.theme.SourceSerif

internal fun highlightFilter(settings: UserSettings) = FeedScope(
    nostrverse = settings.defaultHighlightVisibilityNostrverse,
    friends = settings.defaultHighlightVisibilityFriends,
    mine = settings.defaultHighlightVisibilityMine,
)

internal fun FeedScope.shows(item: PaintedHighlight): Boolean = when {
    item.mine -> mine
    item.friend -> friends
    else -> nostrverse
}

internal fun highlightContextParts(quote: String, context: String?): Triple<String, String, String> {
    if (context.isNullOrBlank()) return Triple("", quote, "")
    val index = context.indexOf(quote)
    if (index < 0) return Triple(context.trim(), quote, "")
    return Triple(
        context.substring(0, index),
        quote,
        context.substring(index + quote.length),
    )
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
    otherColor: Color,
    onDismiss: () -> Unit,
    onSelect: (PaintedHighlight) -> Unit,
    onOpenProfile: (String) -> Unit,
    onToggleMarks: () -> Unit,
) {
    var filter by remember(open) { mutableStateOf(highlightFilter(settings)) }
    val visible = remember(highlights, filter) { highlights.filter(filter::shows) }
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
                    HighlightsPaneHeader(
                        loggedIn = loggedIn,
                        filter = filter,
                        showMarks = settings.showHighlights,
                        mineColor = mineColor,
                        friendsColor = friendsColor,
                        otherColor = otherColor,
                        onDismiss = onDismiss,
                        onToggleNostrverse = { filter = filter.toggle(FeedLevel.Nostrverse) },
                        onToggleFriends = { filter = filter.toggle(FeedLevel.Friends) },
                        onToggleMine = { filter = filter.toggle(FeedLevel.Mine) },
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
                                HighlightPaneCard(
                                    item = item,
                                    selected = item.id == selectedId,
                                    color = when {
                                        item.mine -> mineColor
                                        item.friend -> friendsColor
                                        else -> otherColor
                                    },
                                    onClick = { onSelect(item) },
                                    onOpenProfile = {
                                        if (item.pubkey.isNotBlank()) onOpenProfile(item.pubkey)
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
private fun HighlightsPaneHeader(
    loggedIn: Boolean,
    filter: FeedScope,
    showMarks: Boolean,
    mineColor: Color,
    friendsColor: Color,
    otherColor: Color,
    onDismiss: () -> Unit,
    onToggleNostrverse: () -> Unit,
    onToggleFriends: () -> Unit,
    onToggleMine: () -> Unit,
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

@Composable
private fun HighlightPaneCard(
    item: PaintedHighlight,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val border = color.copy(alpha = if (selected) 0.95f else 0.55f)
    val (before, quote, after) = highlightContextParts(item.quote, item.context)
    val name = item.authorName.ifBlank { Profile.displayName(item.pubkey, null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border, shape)
            .clip(shape)
            .background(
                if (selected) color.copy(alpha = 0.08f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.FormatQuote,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
            if (item.createdAt > 0L) {
                Text(
                    text = RelativeTime.label(item.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = buildAnnotatedString {
                if (before.isNotBlank()) append(before)
                withStyle(
                    SpanStyle(
                        background = color.copy(alpha = 0.45f),
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                ) {
                    append(quote)
                }
                if (after.isNotBlank()) append(after)
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = SourceSerif,
                fontSize = 17.sp,
                lineHeight = 26.sp,
                fontStyle = FontStyle.Italic,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.clickable(onClick = onOpenProfile),
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
