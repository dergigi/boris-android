package org.dergigi.boris.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.ui.home.HomeSections

@Composable
internal fun SectionOrderList(
    title: String,
    intro: String,
    order: List<String>,
    hidden: List<String>,
    onMove: (id: String, delta: Int) -> Unit,
    onToggleVisible: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = intro,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        order.forEachIndexed { index, id ->
            SectionOrderRow(
                label = sectionLabel(id),
                visible = id !in hidden,
                canMoveUp = index > 0,
                canMoveDown = index < order.lastIndex,
                onMove = { onMove(id, it) },
                onToggleVisible = { onToggleVisible(id) },
            )
        }
    }
}

@Composable
private fun SectionOrderRow(
    label: String,
    visible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onToggleVisible: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .alpha(if (visible) 1f else 0.45f),
        )
        IconButton(onClick = onToggleVisible) {
            Icon(
                imageVector = if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = stringResource(
                    if (visible) R.string.settings_section_hide else R.string.settings_section_show,
                ),
            )
        }
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.settings_section_move_up),
            )
        }
        IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.settings_section_move_down),
            )
        }
    }
}

@Composable
private fun sectionLabel(id: String): String = stringResource(
    when (id) {
        HomeSections.CONTINUE -> R.string.home_continue_reading
        HomeSections.YOURS -> R.string.home_recently_highlighted_by_you
        HomeSections.FRIENDS -> R.string.home_recently_highlighted_by_friends
        HomeSections.LIKED_FRIENDS -> R.string.home_liked_by_friends
        HomeSections.READ_FRIENDS -> R.string.home_recently_read_by_friends
        HomeSections.FOAF -> R.string.home_recently_highlighted_by_foaf
        HomeSections.LIKED_FOAF -> R.string.home_liked_by_foaf
        HomeSections.READ_FOAF -> R.string.home_recently_read_by_foaf
        HomeSections.MOST -> R.string.home_most_highlighted
        HomeSections.LIKED_OTHERS -> R.string.home_liked_by_others
        HomeSections.READ_OTHERS -> R.string.home_recently_read_by_others
        HomeSections.SHORT -> R.string.home_short_reads
        HomeSections.LONG -> R.string.home_long_reads
        HomeSections.RANDOM -> R.string.home_random_articles
        else -> R.string.home_recently_highlighted_by_others
    },
)
