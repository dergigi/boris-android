package org.dergigi.boris.ui.support

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.dergigi.boris.R
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.ui.theme.HighlightFriends

private const val FADE_MS = 1_400

/** Displays the support action with a rotating avatar from recent positive zap receipts. */
@Composable
fun SupportHeart(
    onOpenSupport: () -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { SupportStore.ensureLoaded() }
    val state by SupportStore.state.collectAsStateWithLifecycle()
    val avatars = remember(state) {
        val ready = state as? SupportUiState.Ready ?: return@remember emptyList()
        SupportAvatars.from(ready.avatarSupporters, ready.profiles)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenSupport) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.support_title),
                tint = HighlightFriends,
            )
        }
        if (avatars.isNotEmpty()) {
            CyclingSupporterAvatar(
                avatars = avatars,
                onOpenProfile = onOpenProfile,
            )
        }
    }
}

/** Cycles through supporter avatars and opens the matching profile when tapped. */
@Composable
private fun CyclingSupporterAvatar(
    avatars: List<SupportAvatar>,
    onOpenProfile: (String) -> Unit,
) {
    var index by remember(avatars) { mutableIntStateOf(0) }
    LaunchedEffect(avatars) {
        if (avatars.size < 2) return@LaunchedEffect
        while (isActive) {
            delay(SupportAvatars.CYCLE_MS)
            index = (index + 1) % avatars.size
        }
    }
    val current = avatars[index.coerceIn(0, avatars.lastIndex)]
    Crossfade(
        targetState = current,
        animationSpec = tween(FADE_MS),
        label = "supporter-avatar",
    ) { avatar ->
        val name = Profile.displayName(avatar.pubkey, null)
        AsyncImage(
            model = avatar.pictureUrl,
            contentDescription = stringResource(R.string.support_featured_supporter, name),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onOpenProfile(avatar.pubkey) },
        )
    }
}
