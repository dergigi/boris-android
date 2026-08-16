package org.dergigi.boris.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import java.util.Locale
import kotlin.math.roundToInt

private data class ZapPreset(
    val labelRes: Int,
    val highlighter: Double,
    val boris: Double,
    val author: Double,
)

private val PRESETS = listOf(
    ZapPreset(R.string.settings_zap_preset_default, 50.0, 2.1, 50.0),
    ZapPreset(R.string.settings_zap_preset_generous, 5.0, 10.0, 75.0),
    ZapPreset(R.string.settings_zap_preset_selfless, 1.0, 19.0, 80.0),
    ZapPreset(R.string.settings_zap_preset_boris, 10.0, 80.0, 10.0),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZapSplitsSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val yours = settings.zapSplitHighlighterWeight
    val boris = settings.zapSplitBorisWeight
    val author = settings.zapSplitAuthorWeight
    val total = yours + boris + author

    fun percent(weight: Double): String =
        String.format(Locale.US, "%.1f", if (total > 0) weight / total * 100 else 0.0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_zap_presets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PRESETS.forEach { preset ->
                    PresetButton(
                        label = stringResource(preset.labelRes),
                        selected = yours == preset.highlighter &&
                            boris == preset.boris &&
                            author == preset.author,
                        onClick = {
                            onUpdate(
                                settings
                                    .withDouble("zapSplitHighlighterWeight", preset.highlighter)
                                    .withDouble("zapSplitBorisWeight", preset.boris)
                                    .withDouble("zapSplitAuthorWeight", preset.author),
                            )
                        },
                    )
                }
            }
        }
        WeightSlider(
            label = stringResource(R.string.settings_zap_your_share, yours.roundToInt().toString()),
            percentLabel = stringResource(R.string.settings_zap_percent, percent(yours)),
            value = yours,
            maxValue = 100.0,
            onChange = { value ->
                onUpdate(settings.withDouble("zapSplitHighlighterWeight", value.roundToInt().toDouble()))
            },
        )
        WeightSlider(
            label = stringResource(R.string.settings_zap_author_share, author.roundToInt().toString()),
            percentLabel = stringResource(R.string.settings_zap_percent, percent(author)),
            value = author,
            maxValue = 100.0,
            onChange = { value ->
                onUpdate(settings.withDouble("zapSplitAuthorWeight", value.roundToInt().toDouble()))
            },
        )
        WeightSlider(
            label = stringResource(
                R.string.settings_zap_boris_share,
                String.format(Locale.US, "%.1f", boris),
            ),
            percentLabel = stringResource(R.string.settings_zap_percent, percent(boris)),
            value = boris,
            maxValue = 10.0,
            onChange = { value ->
                onUpdate(settings.withDouble("zapSplitBorisWeight", (value * 10).roundToInt() / 10.0))
            },
        )
        Text(
            text = stringResource(R.string.settings_zap_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeightSlider(
    label: String,
    percentLabel: String,
    value: Double,
    maxValue: Double,
    onChange: (Double) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = percentLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.toFloat().coerceIn(0f, maxValue.toFloat()),
            onValueChange = { onChange(it.toDouble()) },
            valueRange = 0f..maxValue.toFloat(),
        )
    }
}

@Composable
private fun PresetButton(
    label: String,
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
            .height(36.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
        )
    }
}
