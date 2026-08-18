package org.dergigi.boris.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.reader.VolumeKeys

@Composable
fun ScrollBehaviourSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingCheckbox(
            label = stringResource(R.string.settings_hide_top_bar),
            checked = settings.hideTopBarOnScroll,
            onCheckedChange = { onUpdate(settings.withBoolean("hideTopBarOnScroll", it)) },
        )
        SettingCheckbox(
            label = stringResource(R.string.settings_volume_scroll),
            checked = settings.volumeButtonScroll,
            onCheckedChange = { onUpdate(settings.withBoolean("volumeButtonScroll", it)) },
        )
        SettingCheckbox(
            label = stringResource(R.string.settings_sync_reading_position),
            checked = settings.syncReadingPosition,
            onCheckedChange = { onUpdate(settings.withBoolean("syncReadingPosition", it)) },
        )
        SettingCheckbox(
            label = stringResource(R.string.settings_auto_scroll_position),
            checked = settings.autoScrollToReadingPosition,
            onCheckedChange = { onUpdate(settings.withBoolean("autoScrollToReadingPosition", it)) },
        )
        SettingCheckbox(
            label = stringResource(R.string.settings_auto_archive_complete),
            checked = settings.autoMarkAsReadOnCompletion,
            onCheckedChange = { onUpdate(settings.withBoolean("autoMarkAsReadOnCompletion", it)) },
        )
        SettingCheckbox(
            label = stringResource(R.string.settings_archive_closes_reader),
            checked = settings.archiveClosesReader,
            onCheckedChange = { onUpdate(settings.withBoolean("archiveClosesReader", it)) },
        )
        if (settings.volumeButtonScroll) {
            SettingRow(stringResource(R.string.settings_volume_scroll_amount)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    VolumeKeys.AMOUNTS.forEach { amount ->
                        AmountButton(
                            amount = amount,
                            selected = settings.volumeButtonScrollPercent == amount,
                            onClick = { onUpdate(settings.withInt("volumeButtonScrollPercent", amount)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountButton(
    amount: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val border = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 36.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$amount%",
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}
