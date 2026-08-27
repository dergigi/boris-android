package org.dergigi.boris.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.dergigi.boris.R
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.data.UserSettings

@Composable
fun ContentFilterMenu(settings: UserSettings) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = stringResource(R.string.home_filters),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
        ) {
            FilterToggle(
                label = stringResource(R.string.home_hide_archived),
                checked = settings.hideArchivedOnHome,
                onCheckedChange = {
                    SettingsSync.apply(settings.withBoolean("hideArchivedOnHome", it))
                },
            )
            FilterToggle(
                label = stringResource(R.string.home_hide_completed),
                checked = settings.hideCompletedOnHome,
                onCheckedChange = {
                    SettingsSync.apply(settings.withBoolean("hideCompletedOnHome", it))
                },
            )
            FilterToggle(
                label = stringResource(R.string.home_hide_nsfw),
                checked = settings.hideNsfwOnHome,
                onCheckedChange = {
                    SettingsSync.apply(settings.withBoolean("hideNsfwOnHome", it))
                },
            )
        }
    }
}

@Composable
private fun FilterToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
        },
        onClick = { onCheckedChange(!checked) },
    )
}
