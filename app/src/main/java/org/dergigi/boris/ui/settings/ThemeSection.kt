package org.dergigi.boris.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.theme.Black
import org.dergigi.boris.ui.theme.Charcoal
import org.dergigi.boris.ui.theme.Ivory
import org.dergigi.boris.ui.theme.Paper
import org.dergigi.boris.ui.theme.Sepia
import org.dergigi.boris.ui.theme.Zinc200
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
    val showDark = appearance != "light"
    val showLight = appearance != "dark"
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SettingRow(stringResource(R.string.settings_appearance)) {
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
        if (showDark) {
            SettingRow(stringResource(R.string.settings_dark_theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    darkSwatches().forEach { swatch ->
                        PaletteSwatch(
                            color = swatch.color,
                            selected = settings.darkColorTheme == swatch.id,
                            lightCheck = false,
                            contentDescription = swatch.label,
                            onClick = { onUpdate(settings.withString("darkColorTheme", swatch.id)) },
                        )
                    }
                }
            }
        }
        if (showLight) {
            SettingRow(stringResource(R.string.settings_light_theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lightSwatches().forEach { swatch ->
                        PaletteSwatch(
                            color = swatch.color,
                            selected = settings.lightColorTheme == swatch.id,
                            lightCheck = true,
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
private fun PaletteSwatch(
    color: Color,
    selected: Boolean,
    lightCheck: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    outlined: Boolean = false,
) {
    val shape = RoundedCornerShape(8.dp)
    val border = when {
        selected -> MaterialTheme.colorScheme.primary
        outlined -> MaterialTheme.colorScheme.outline
        else -> Color.Transparent
    }
    val check = if (lightCheck) Zinc900 else Zinc200
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .background(color)
            .border(if (selected) 2.dp else 1.dp, border, shape)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = check,
                modifier = Modifier.size(16.dp),
            )
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
