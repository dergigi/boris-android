package org.dergigi.boris.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.ui.theme.BorisIcons

enum class ContentTab {
    Highlights,
    Writings,
}

@Composable
fun ContentTabs(
    tab: ContentTab,
    onSelect: (ContentTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContentTabChip(
            selected = tab == ContentTab.Highlights,
            label = stringResource(R.string.you_tab_highlights),
            icon = BorisIcons.Highlighter,
            onClick = { onSelect(ContentTab.Highlights) },
        )
        ContentTabChip(
            selected = tab == ContentTab.Writings,
            label = stringResource(R.string.you_tab_writings),
            icon = Icons.Outlined.Edit,
            onClick = { onSelect(ContentTab.Writings) },
        )
    }
}

@Composable
private fun ContentTabChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    )
}
