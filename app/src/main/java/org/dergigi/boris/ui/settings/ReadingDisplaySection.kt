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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.FormatAlignJustify
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dergigi.boris.R
import org.dergigi.boris.data.HexColor
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

@Composable
fun ReadingDisplaySection(
    settings: UserSettings,
    darkTheme: Boolean,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkColors = if (darkTheme) ReadingFonts.LINK_COLORS_DARK else ReadingFonts.LINK_COLORS_LIGHT
    val linkColor = if (darkTheme) settings.linkColorDark else settings.linkColorLight
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_reading_display),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SettingRow(stringResource(R.string.settings_highlight_style)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconToggle(
                    icon = Icons.Outlined.Highlight,
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
        SettingRow(stringResource(R.string.settings_paragraph_alignment)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconToggle(
                    icon = Icons.AutoMirrored.Outlined.FormatAlignLeft,
                    selected = !settings.justifyParagraphs,
                    contentDescription = stringResource(R.string.settings_align_left),
                    onClick = { onUpdate(settings.withString("paragraphAlignment", "left")) },
                )
                IconToggle(
                    icon = Icons.Outlined.FormatAlignJustify,
                    selected = settings.justifyParagraphs,
                    contentDescription = stringResource(R.string.settings_align_justify),
                    onClick = { onUpdate(settings.withString("paragraphAlignment", "justify")) },
                )
            }
        }
        SettingRow(stringResource(R.string.settings_highlight_visibility)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VisibilityToggle(
                    icon = Icons.Outlined.Hub,
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
                    icon = Icons.Outlined.Group,
                    on = settings.defaultHighlightVisibilityFriends,
                    tint = hexColor(settings.highlightColorFriends, Color(0xFFF97316)),
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
                    icon = Icons.Outlined.Person,
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
        SettingRow(stringResource(R.string.settings_reading_font)) {
            FontDropdown(
                selected = settings.readingFont,
                onSelect = { onUpdate(settings.withString("readingFont", it)) },
            )
        }
        SettingRow(stringResource(R.string.settings_link_color)) {
            ColorSwatches(
                colors = linkColors,
                selected = linkColor,
                onSelect = { color ->
                    val key = if (darkTheme) "linkColorDark" else "linkColorLight"
                    onUpdate(settings.withString(key, color))
                },
            )
        }
        SettingRow(stringResource(R.string.settings_font_size)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ReadingFonts.SIZES.forEach { size ->
                    FontSizeButton(
                        size = size,
                        selected = settings.fontSize == size,
                        onClick = { onUpdate(settings.withInt("fontSize", size)) },
                    )
                }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onUpdate(settings.withBoolean("showHighlights", !settings.showHighlights))
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = settings.showHighlights,
                onCheckedChange = { onUpdate(settings.withBoolean("showHighlights", it)) },
            )
            Text(
                text = stringResource(R.string.settings_show_highlights),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun VisibilityToggle(
    icon: ImageVector,
    on: Boolean,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = if (on) 1f else 0.4f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontDropdown(
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = ReadingFonts.label(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(0.62f),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = ReadingFonts.family(selected),
            ),
            shape = RoundedCornerShape(8.dp),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ReadingFonts.ALL.forEach { font ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = font.label,
                            fontFamily = ReadingFonts.family(font.id),
                        )
                    },
                    onClick = {
                        onSelect(font.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatches(
    colors: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { hex ->
            val color = hexColor(hex, Color.Gray)
            val isSelected = hex.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                        },
                    )
                    .clickable { onSelect(hex) },
            )
        }
    }
}

@Composable
private fun FontSizeButton(
    size: Int,
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
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "A",
            color = fg,
            fontSize = (size - 8).coerceAtLeast(10).sp,
            textAlign = TextAlign.Center,
        )
    }
}

internal fun hexColor(hex: String, fallback: Color): Color {
    val argb = HexColor.argb(hex) ?: return fallback
    return Color(argb)
}
