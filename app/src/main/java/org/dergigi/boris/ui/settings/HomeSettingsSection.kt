package org.dergigi.boris.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
        SectionOrderList(
            title = stringResource(R.string.settings_home_sections),
            intro = stringResource(R.string.settings_home_sections_intro),
            order = HomeSections.order(settings.homeSectionOrder),
            onMove = { id, delta ->
                onUpdate(
                    settings.withStringList(
                        "homeSectionOrder",
                        HomeSections.move(HomeSections.order(settings.homeSectionOrder), id, delta),
                    ),
                )
            },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
