package org.dergigi.boris.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Visibility
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
import org.dergigi.boris.data.BookmarkBucket
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.theme.BorisIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibrarySettingsSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_library_default_view),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BookmarkBucket.entries.forEach { bucket ->
                ShelfChip(
                    label = stringResource(bucket.labelRes),
                    icon = bucket.icon,
                    selected = settings.defaultLibraryView == bucket,
                    onClick = {
                        onUpdate(settings.withString("defaultLibraryView", bucket.name))
                    },
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_library_default_view_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val BookmarkBucket.labelRes: Int
    get() = when (this) {
        BookmarkBucket.All -> R.string.library_all
        BookmarkBucket.Private -> R.string.library_private
        BookmarkBucket.Public -> R.string.library_public
        BookmarkBucket.Web -> R.string.library_web
        BookmarkBucket.Look -> R.string.library_look
        BookmarkBucket.Archive -> R.string.library_archive
    }

private val BookmarkBucket.icon: ImageVector
    get() = when (this) {
        BookmarkBucket.All -> Icons.Outlined.Apps
        BookmarkBucket.Private -> Icons.Outlined.Lock
        BookmarkBucket.Public -> Icons.Outlined.Public
        BookmarkBucket.Web -> Icons.Outlined.Language
        BookmarkBucket.Look -> Icons.Outlined.Visibility
        BookmarkBucket.Archive -> BorisIcons.Books
    }

@Composable
private fun ShelfChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
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
