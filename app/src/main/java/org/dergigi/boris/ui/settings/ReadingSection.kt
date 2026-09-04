package org.dergigi.boris.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.FormatAlignJustify
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dergigi.boris.R
import org.dergigi.boris.data.DisplayTypeStore
import org.dergigi.boris.data.UserSettings

@Composable
fun ReadingSection(
    settings: UserSettings,
    darkTheme: Boolean,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayType by DisplayTypeStore.type.collectAsStateWithLifecycle()
    val linkColors = if (darkTheme) ReadingFonts.LINK_COLORS_DARK else ReadingFonts.LINK_COLORS_LIGHT
    val linkColor = if (darkTheme) settings.linkColorDark else settings.linkColorLight
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingRow(stringResource(R.string.settings_reading_font)) {
            FontDropdown(
                selected = settings.readingFont,
                onSelect = { onUpdate(settings.withString("readingFont", it)) },
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
        if (!displayType.eink) SettingRow(stringResource(R.string.settings_link_color)) {
            ColorSwatches(
                colors = linkColors,
                selected = linkColor,
                onSelect = { color ->
                    val key = if (darkTheme) "linkColorDark" else "linkColorLight"
                    onUpdate(settings.withString(key, color))
                },
            )
        }
        SettingCheckbox(
            label = stringResource(R.string.settings_show_reader_progress_bar),
            checked = settings.showReaderProgressBar,
            onCheckedChange = { onUpdate(settings.withBoolean("showReaderProgressBar", it)) },
        )
        if (settings.showReaderProgressBar) {
            SettingCheckbox(
                label = stringResource(R.string.settings_show_reader_progress_heading),
                checked = settings.showReaderProgressHeading,
                onCheckedChange = { onUpdate(settings.withBoolean("showReaderProgressHeading", it)) },
            )
        }
        SettingCheckbox(
            label = stringResource(R.string.settings_open_links_in_reader),
            checked = settings.openLinksInReader,
            onCheckedChange = { onUpdate(settings.withBoolean("openLinksInReader", it)) },
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
private fun FontSizeButton(
    size: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = RoundedCornerShape(8.dp)
    val border = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
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
