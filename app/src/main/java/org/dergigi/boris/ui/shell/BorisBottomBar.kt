package org.dergigi.boris.ui.shell

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun BorisBottomBar(
    selected: MainTab,
    pictureUrl: String?,
    onSelect: (MainTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = {
                    if (tab == MainTab.Account && !pictureUrl.isNullOrBlank()) {
                        val fallback = rememberVectorPainter(Icons.Outlined.AccountCircle)
                        AsyncImage(
                            model = pictureUrl,
                            contentDescription = stringResource(tab.labelRes),
                            contentScale = ContentScale.Crop,
                            placeholder = fallback,
                            error = fallback,
                            fallback = fallback,
                            modifier = Modifier
                                .size(24.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = stringResource(tab.labelRes),
                        )
                    }
                },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}
