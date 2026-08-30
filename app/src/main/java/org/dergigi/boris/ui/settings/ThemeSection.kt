package org.dergigi.boris.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dergigi.boris.R
import org.dergigi.boris.data.DisplayType
import org.dergigi.boris.data.DisplayTypeStore
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.theme.Black
import org.dergigi.boris.ui.theme.Charcoal
import org.dergigi.boris.ui.theme.Ivory
import org.dergigi.boris.ui.theme.Paper
import org.dergigi.boris.ui.theme.Sepia
import org.dergigi.boris.ui.theme.Zinc900

private data class ThemeSwatch(
    val id: String,
    val color: Color,
    val label: String,
)

@Composable
fun ThemeSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = settings.theme
    val displayType by DisplayTypeStore.type.collectAsStateWithLifecycle()
    val eink = displayType.eink
    val showDark = appearance != "light"
    val showLight = appearance != "dark"
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingRow(stringResource(R.string.settings_display_type)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !eink,
                    onClick = { DisplayTypeStore.set(DisplayType.Color) },
                    label = { Text(stringResource(R.string.settings_display_color)) },
                )
                FilterChip(
                    selected = eink,
                    onClick = { DisplayTypeStore.set(DisplayType.Eink) },
                    label = { Text(stringResource(R.string.settings_display_eink)) },
                )
            }
        }
        if (eink) {
            Text(
                text = stringResource(R.string.settings_display_eink_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingRow(stringResource(R.string.settings_theme)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconToggle(
                    icon = Icons.Outlined.LightMode,
                    selected = appearance == "light",
                    contentDescription = stringResource(R.string.settings_theme_light),
                    onClick = { onUpdate(settings.withString("theme", "light")) },
                )
                IconToggle(
                    icon = Icons.Outlined.DarkMode,
                    selected = appearance == "dark",
                    contentDescription = stringResource(R.string.settings_theme_dark),
                    onClick = { onUpdate(settings.withString("theme", "dark")) },
                )
                IconToggle(
                    icon = Icons.Outlined.Computer,
                    selected = appearance == "system",
                    contentDescription = stringResource(R.string.settings_theme_system),
                    onClick = { onUpdate(settings.withString("theme", "system")) },
                )
            }
        }
        if (showDark && !eink) {
            SettingRow(stringResource(R.string.settings_dark_theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    darkSwatches().forEach { swatch ->
                        ColorChip(
                            color = swatch.color,
                            selected = settings.darkColorTheme == swatch.id,
                            contentDescription = swatch.label,
                            onClick = { onUpdate(settings.withString("darkColorTheme", swatch.id)) },
                        )
                    }
                }
            }
        }
        if (showLight && !eink) {
            SettingRow(stringResource(R.string.settings_light_theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lightSwatches().forEach { swatch ->
                        ColorChip(
                            color = swatch.color,
                            selected = settings.lightColorTheme == swatch.id,
                            outlined = swatch.id == "paper-white",
                            contentDescription = swatch.label,
                            onClick = { onUpdate(settings.withString("lightColorTheme", swatch.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun darkSwatches() = listOf(
    ThemeSwatch("black", Black, stringResource(R.string.settings_theme_black)),
    ThemeSwatch("midnight", Zinc900, stringResource(R.string.settings_theme_midnight)),
    ThemeSwatch("charcoal", Charcoal, stringResource(R.string.settings_theme_charcoal)),
)

@Composable
private fun lightSwatches() = listOf(
    ThemeSwatch("paper-white", Paper, stringResource(R.string.settings_theme_paper_white)),
    ThemeSwatch("sepia", Sepia, stringResource(R.string.settings_theme_sepia)),
    ThemeSwatch("ivory", Ivory, stringResource(R.string.settings_theme_ivory)),
)
