package org.dergigi.boris.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    ContentTab.Rss -> stringResource(R.string.feed_tab_rss)
}

fun ContentTab.icon(): ImageVector = when (this) {
    ContentTab.All -> Icons.Outlined.Apps
    ContentTab.Highlights -> BorisIcons.Highlighter
    ContentTab.Writings -> Icons.Outlined.Edit
    ContentTab.Rss -> Icons.Outlined.RssFeed
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentTabs(
    tab: ContentTab,
    onSelect: (ContentTab) -> Unit,
    modifier: Modifier = Modifier,
    showRss: Boolean = false,
    showAll: Boolean = false,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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

@Composable
fun ContentTabChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
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
