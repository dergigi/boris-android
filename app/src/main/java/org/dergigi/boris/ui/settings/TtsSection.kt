package org.dergigi.boris.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dergigi.boris.R
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.tts.TtsLanguage
import org.dergigi.boris.tts.TtsPlayback
import org.dergigi.boris.tts.TtsPreview
import org.dergigi.boris.tts.TtsSpeed
import org.dergigi.boris.ui.reader.openTtsSettings

/**
 * Webapp-shaped Text-to-Speech settings (D-06):
 * speed cycle, speaker language, locked preview sentence, follow-along.
 * Writes go to the existing NIP-78 keys only (D-07, D-08, D-09, D-13).
 */
@Composable
fun TtsSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val session by TtsPlayback.session.collectAsStateWithLifecycle()
    val previewing by TtsPlayback.previewing.collectAsStateWithLifecycle()
    val previewError by TtsPlayback.previewError.collectAsStateWithLifecycle()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingRow(stringResource(R.string.tts_speed_label)) {
            SpeedCycleChip(
                rate = TtsSpeed.snap(settings.ttsDefaultSpeed),
                onCycle = {
                    val next = TtsSpeed.cycle(settings.ttsDefaultSpeed)
                    onUpdate(settings.withDouble("ttsDefaultSpeed", next))
                    if (session != null) TtsPlayback.setRate(next)
                },
            )
        }
        SettingRow(stringResource(R.string.tts_language_label)) {
            LanguageDropdown(
                selected = TtsLanguage.mode(
                    settings.ttsLanguageMode,
                    settings.ttsUseSystemLanguage,
                    settings.ttsDetectContentLanguage,
                ),
                onSelect = { value ->
                    onUpdate(
                        settings
                            .withString("ttsLanguageMode", value)
                            .withBoolean("ttsUseSystemLanguage", value == "system")
                            .withBoolean("ttsDetectContentLanguage", value == "content"),
                    )
                    TtsPlayback.applyLanguage()
                },
            )
        }
        TtsPreviewBox(
            previewing = previewing,
            onPlay = { TtsPlayback.preview(context, TtsPreview.EXAMPLE_TEXT) },
            onStop = { TtsPlayback.stopPreview() },
        )
        val error = session?.errorMessage ?: previewError
        if (error != null) {
            TtsErrorNotice(
                error = error,
                onOpenSettings = { openTtsSettings(context) },
            )
        }
        SettingCheckbox(
            label = stringResource(R.string.tts_follow_along),
            checked = settings.ttsFollowAlong,
            onCheckedChange = { onUpdate(settings.withBoolean("ttsFollowAlong", it)) },
        )
    }
}

@Composable
private fun SpeedCycleChip(
    rate: Double,
    onCycle: () -> Unit,
) {
    val description = stringResource(R.string.tts_cycle_speed)
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onCycle)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(SettingChipSize)
                .defaultMinSize(minWidth = 48.dp)
                .clip(SettingChipShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, SettingChipShape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = rateLabel(rate),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = stringResource(languageLabelRes(selected)),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(0.62f),
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(8.dp),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TtsLanguage.MODES.forEach { mode ->
                if (mode == "en-US") HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(languageLabelRes(mode))) },
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TtsPreviewBox(
    previewing: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = TtsPreview.EXAMPLE_TEXT,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(
            onClick = { if (previewing) onStop() else onPlay() },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = if (previewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (previewing) R.string.tts_pause_playback else R.string.tts_listen_to_article,
                ),
            )
        }
    }
}

/**
 * D-11: an engine or language failure never hides the controls above; it only
 * adds this notice with a deep link into the system TTS settings.
 */
@Composable
private fun TtsErrorNotice(
    error: String,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                if (error == TtsPlayback.ERROR_LANGUAGE) {
                    R.string.tts_error_language
                } else {
                    R.string.tts_error_engine
                },
            ),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        TextButton(onClick = onOpenSettings) {
            Text(stringResource(R.string.tts_open_settings))
        }
    }
}

private fun rateLabel(rate: Double): String =
    if (rate % 1.0 == 0.0) "${rate.toInt()}x" else "${rate}x"

private fun languageLabelRes(mode: String): Int = when (mode) {
    "system" -> R.string.tts_lang_system
    "content" -> R.string.tts_lang_content
    "en-US" -> R.string.tts_lang_en_us
    "en-GB" -> R.string.tts_lang_en_gb
    "zh" -> R.string.tts_lang_zh
    "es" -> R.string.tts_lang_es
    "hi" -> R.string.tts_lang_hi
    "ar" -> R.string.tts_lang_ar
    "fr" -> R.string.tts_lang_fr
    "pt" -> R.string.tts_lang_pt
    "de" -> R.string.tts_lang_de
    "ja" -> R.string.tts_lang_ja
    "ru" -> R.string.tts_lang_ru
    else -> R.string.tts_lang_content
}
