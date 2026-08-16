package org.dergigi.boris.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.feed.FeedLevel
import org.dergigi.boris.ui.feed.FeedScope
import org.dergigi.boris.ui.feed.FeedScopeStore
import org.dergigi.boris.ui.feed.withExploreScope
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

@Composable
fun FeedSettingsSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = FeedScope.fromSettings(settings)
    fun toggle(level: FeedLevel) {
        val next = scope.toggle(level)
        if (next == scope) return
        FeedScopeStore.clear(context)
        onUpdate(settings.withExploreScope(next))
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingRow(stringResource(R.string.settings_feed_scope)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScopeIcon(
                    icon = Icons.Outlined.Hub,
                    on = scope.nostrverse,
                    tint = hexColor(settings.highlightColorNostrverse, HighlightOther),
                    contentDescription = stringResource(R.string.feed_scope_nostrverse),
                    onClick = { toggle(FeedLevel.Nostrverse) },
                )
                ScopeIcon(
                    icon = Icons.Outlined.Group,
                    on = scope.friends,
                    tint = hexColor(settings.highlightColorFriends, HighlightFriends),
                    contentDescription = stringResource(R.string.feed_scope_friends),
                    onClick = { toggle(FeedLevel.Friends) },
                )
                ScopeIcon(
                    icon = Icons.Outlined.Person,
                    on = scope.mine,
                    tint = hexColor(settings.highlightColorMine, HighlightMine),
                    contentDescription = stringResource(R.string.feed_scope_mine),
                    onClick = { toggle(FeedLevel.Mine) },
                )
            }
        }
    }
}

@Composable
private fun ScopeIcon(
    icon: ImageVector,
    on: Boolean,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (on) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        )
    }
}
