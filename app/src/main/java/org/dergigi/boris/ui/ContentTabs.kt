package org.dergigi.boris.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.ui.theme.BorisIcons

enum class ContentTab {
    All,
    Highlights,
    Writings,
    Articles,
    Public,
    Web,
    Rss,
    ;

    companion object {
        /** Tabs shown on the Feeds screen (and as Feeds default-view options). */
        val feedEntries: List<ContentTab> = listOf(All, Highlights, Writings, Rss)

        fun fromSettings(name: String): ContentTab = when (name) {
            Highlights.name -> Highlights
            Writings.name -> Writings
            Rss.name -> Rss
            else -> All
        }
    }
}

@Composable
fun ContentTab.label(): String = when (this) {
    ContentTab.All -> stringResource(R.string.feed_tab_all)
    ContentTab.Highlights -> stringResource(R.string.you_tab_highlights)
    ContentTab.Writings -> stringResource(R.string.you_tab_writings)
    ContentTab.Articles -> stringResource(R.string.you_tab_articles)
    ContentTab.Public -> stringResource(R.string.library_public)
    ContentTab.Web -> stringResource(R.string.library_web)
    ContentTab.Rss -> stringResource(R.string.feed_tab_rss)
}

fun ContentTab.icon(): ImageVector = when (this) {
    ContentTab.All -> Icons.Outlined.Apps
    ContentTab.Highlights -> BorisIcons.Highlighter
    ContentTab.Writings -> Icons.Outlined.Edit
    ContentTab.Articles -> Icons.AutoMirrored.Outlined.Article
    ContentTab.Public -> Icons.Outlined.Public
    ContentTab.Web -> Icons.Outlined.Language
    ContentTab.Rss -> Icons.Outlined.RssFeed
}

@Composable
fun FilterChipRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun ContentTabs(
    tab: ContentTab,
    onSelect: (ContentTab) -> Unit,
    modifier: Modifier = Modifier,
    showRss: Boolean = false,
    showAll: Boolean = false,
    showArticles: Boolean = false,
    showBookmarks: Boolean = false,
) {
    FilterChipRow(modifier = modifier) {
        if (showAll) {
            ContentTabChip(
                selected = tab == ContentTab.All,
                label = ContentTab.All.label(),
                icon = ContentTab.All.icon(),
                onClick = { onSelect(ContentTab.All) },
            )
        }
        ContentTabChip(
            selected = tab == ContentTab.Highlights,
            label = ContentTab.Highlights.label(),
            icon = ContentTab.Highlights.icon(),
            onClick = { onSelect(ContentTab.Highlights) },
        )
        ContentTabChip(
            selected = tab == ContentTab.Writings,
            label = ContentTab.Writings.label(),
            icon = ContentTab.Writings.icon(),
            onClick = { onSelect(ContentTab.Writings) },
        )
        if (showArticles) {
            ContentTabChip(
                selected = tab == ContentTab.Articles,
                label = ContentTab.Articles.label(),
                icon = ContentTab.Articles.icon(),
                onClick = { onSelect(ContentTab.Articles) },
            )
        }
        if (showBookmarks) {
            ContentTabChip(
                selected = tab == ContentTab.Public,
                label = ContentTab.Public.label(),
                icon = ContentTab.Public.icon(),
                onClick = { onSelect(ContentTab.Public) },
            )
            ContentTabChip(
                selected = tab == ContentTab.Web,
                label = ContentTab.Web.label(),
                icon = ContentTab.Web.icon(),
                onClick = { onSelect(ContentTab.Web) },
            )
        }
        if (showRss) {
            ContentTabChip(
                selected = tab == ContentTab.Rss,
                label = ContentTab.Rss.label(),
                icon = ContentTab.Rss.icon(),
                onClick = { onSelect(ContentTab.Rss) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentTabChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val intoView = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) {
        if (selected) intoView.bringIntoView()
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.bringIntoViewRequester(intoView),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    )
}
