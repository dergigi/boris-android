package org.dergigi.boris.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.MostHighlightedWindow
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.home.HomeSections

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreSettingsSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val order = HomeSections.exploreOrder(settings.exploreSectionOrder, settings.homeSectionOrder)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SectionOrderList(
            title = stringResource(R.string.settings_explore_sections),
            intro = stringResource(R.string.settings_explore_sections_intro),
            order = order,
            hidden = HomeSections.hidden(settings.exploreHiddenSections, HomeSections.EXPLORE_DEFAULT),
            onMove = { id, delta ->
                onUpdate(settings.withStringList("exploreSectionOrder", HomeSections.move(order, id, delta)))
            },
            onToggleVisible = { id ->
                onUpdate(
                    settings.withStringList(
                        "exploreHiddenSections",
                        HomeSections.toggleHidden(settings.exploreHiddenSections, id, HomeSections.EXPLORE_DEFAULT),
                    ),
                )
            },
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_explore_most_window),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.settings_explore_most_window_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MostHighlightedWindow.entries.forEach { window ->
                    FilterChip(
                        selected = settings.mostHighlightedWindow == window,
                        onClick = { onUpdate(settings.withString("mostHighlightedWindow", window.id)) },
                        label = { Text(mostWindowLabel(window)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun mostWindowLabel(window: MostHighlightedWindow): String = stringResource(
    when (window) {
        MostHighlightedWindow.Day -> R.string.home_most_highlighted_24h_full
        MostHighlightedWindow.Week -> R.string.home_most_highlighted_7d_full
        MostHighlightedWindow.Month -> R.string.home_most_highlighted_30d_full
    },
)
