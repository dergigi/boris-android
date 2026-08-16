package org.dergigi.boris.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

@Composable
fun HighlightsSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingCheckbox(
            label = stringResource(R.string.settings_show_highlights),
            checked = settings.showHighlights,
            onCheckedChange = { onUpdate(settings.withBoolean("showHighlights", it)) },
        )
        SettingRow(stringResource(R.string.settings_highlight_style)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconToggle(
                    icon = BorisIcons.Highlighter,
                    selected = settings.markerStyle,
                    contentDescription = stringResource(R.string.settings_style_marker),
                    onClick = { onUpdate(settings.withString("highlightStyle", "marker")) },
                )
                IconToggle(
                    icon = Icons.Outlined.FormatUnderlined,
                    selected = !settings.markerStyle,
                    contentDescription = stringResource(R.string.settings_style_underline),
                    onClick = { onUpdate(settings.withString("highlightStyle", "underline")) },
                )
            }
        }
        SettingRow(stringResource(R.string.settings_color_mine)) {
            ColorSwatches(
                colors = ReadingFonts.HIGHLIGHT_COLORS,
                selected = settings.highlightColorMine,
                onSelect = { onUpdate(settings.withString("highlightColorMine", it)) },
            )
        }
        SettingRow(stringResource(R.string.settings_color_friends)) {
            ColorSwatches(
                colors = ReadingFonts.HIGHLIGHT_COLORS,
                selected = settings.highlightColorFriends,
                onSelect = { onUpdate(settings.withString("highlightColorFriends", it)) },
            )
        }
        SettingRow(stringResource(R.string.settings_color_nostrverse)) {
            ColorSwatches(
                colors = ReadingFonts.HIGHLIGHT_COLORS,
                selected = settings.highlightColorNostrverse,
                onSelect = { onUpdate(settings.withString("highlightColorNostrverse", it)) },
            )
        }
        SettingRow(stringResource(R.string.settings_highlight_visibility)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VisibilityToggle(
                    on = settings.defaultHighlightVisibilityNostrverse,
                    tint = hexColor(settings.highlightColorNostrverse, HighlightOther),
                    contentDescription = stringResource(R.string.settings_visibility_nostrverse),
                    onClick = {
                        onUpdate(
                            settings.withBoolean(
                                "defaultHighlightVisibilityNostrverse",
                                !settings.defaultHighlightVisibilityNostrverse,
                            ),
                        )
                    },
                )
                VisibilityToggle(
                    on = settings.defaultHighlightVisibilityFriends,
                    tint = hexColor(settings.highlightColorFriends, HighlightFriends),
                    contentDescription = stringResource(R.string.settings_visibility_friends),
                    onClick = {
                        onUpdate(
                            settings.withBoolean(
                                "defaultHighlightVisibilityFriends",
                                !settings.defaultHighlightVisibilityFriends,
                            ),
                        )
                    },
                )
                VisibilityToggle(
                    on = settings.defaultHighlightVisibilityMine,
                    tint = hexColor(settings.highlightColorMine, HighlightMine),
                    contentDescription = stringResource(R.string.settings_visibility_mine),
                    onClick = {
                        onUpdate(
                            settings.withBoolean(
                                "defaultHighlightVisibilityMine",
                                !settings.defaultHighlightVisibilityMine,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun VisibilityToggle(
    on: Boolean,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SettingChipSize)
            .clip(SettingChipShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, SettingChipShape)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = BorisIcons.Highlighter,
            contentDescription = null,
            tint = tint.copy(alpha = if (on) 1f else 0.4f),
            modifier = Modifier.size(20.dp),
        )
    }
}
