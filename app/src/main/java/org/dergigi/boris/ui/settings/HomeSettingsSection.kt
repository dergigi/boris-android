package org.dergigi.boris.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.home.HomeSections

@Composable
fun HomeSettingsSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingCheckbox(
            label = stringResource(R.string.settings_hide_archived),
            checked = settings.hideArchivedOnHome,
            onCheckedChange = { onUpdate(settings.withBoolean("hideArchivedOnHome", it)) },
        )
        SettingCheckbox(
            label = stringResource(R.string.settings_hide_completed),
            checked = settings.hideCompletedOnHome,
            onCheckedChange = { onUpdate(settings.withBoolean("hideCompletedOnHome", it)) },
        )
        SettingCheckbox(
            label = stringResource(R.string.settings_hide_nsfw),
            checked = settings.hideNsfwOnHome,
            onCheckedChange = { onUpdate(settings.withBoolean("hideNsfwOnHome", it)) },
        )
        SettingCheckbox(
            label = stringResource(R.string.settings_nsfw_warn),
            checked = settings.nsfwWarnInReader,
            onCheckedChange = { onUpdate(settings.withBoolean("nsfwWarnInReader", it)) },
        )
        Text(
            text = stringResource(R.string.settings_home_sections),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        Text(
            text = stringResource(R.string.settings_home_sections_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val order = HomeSections.order(settings.homeSectionOrder)
        order.forEachIndexed { index, id ->
            SectionOrderRow(
                label = sectionLabel(id),
                canMoveUp = index > 0,
                canMoveDown = index < order.lastIndex,
                onMove = { delta ->
                    onUpdate(
                        settings.withStringList(
                            "homeSectionOrder",
                            HomeSections.move(order, id, delta),
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun SectionOrderRow(
    label: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.settings_section_move_up),
            )
        }
        IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.settings_section_move_down),
            )
        }
    }
}

@Composable
private fun sectionLabel(id: String): String = stringResource(
    when (id) {
        HomeSections.CONTINUE -> R.string.home_continue_reading
        HomeSections.YOURS -> R.string.home_recently_highlighted_by_you
        HomeSections.FRIENDS -> R.string.home_recently_highlighted_by_friends
        HomeSections.MOST -> R.string.home_most_highlighted
        HomeSections.SHORT -> R.string.home_short_reads
        HomeSections.LONG -> R.string.home_long_reads
        HomeSections.RANDOM -> R.string.home_random_articles
        else -> R.string.home_recently_highlighted_by_others
    },
)
