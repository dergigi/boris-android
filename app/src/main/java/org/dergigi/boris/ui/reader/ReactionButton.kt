package org.dergigi.boris.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.nostr.ArticleReaction

internal val ReactionOrange = Color(0xFFF7931A)

/**
 * Square button beside [ArchiveButton]. Tap sends the default (orange heart) reaction, or
 * removes the current one; long-press opens the full reaction picker.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ReactionButton(
    reaction: ArticleReaction?,
    onReact: (ArticleReaction?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(8.dp)
    val active = reaction != null
    val label = stringResource(R.string.reader_react)
    Box(modifier) {
        Box(
            modifier = Modifier
                .size(ButtonDefaults.MinHeight)
                .clip(shape)
                .background(if (active) ReactionOrange.copy(alpha = 0.14f) else Color.Transparent)
                .border(1.dp, if (active) ReactionOrange else MaterialTheme.colorScheme.outline, shape)
                .semantics { contentDescription = label }
                .combinedClickable(
                    onClick = { onReact(reaction ?: ArticleReaction.DEFAULT) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (reaction != null) {
                Text(text = reaction.emoji, style = MaterialTheme.typography.titleMedium)
            } else {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = ButtonDefaults.outlinedButtonColors().contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            ArticleReaction.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    leadingIcon = { Text(option.emoji) },
                    trailingIcon = if (option == reaction) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        menuOpen = false
                        onReact(option)
                    },
                )
            }
            if (active) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_reaction_remove)) },
                    onClick = {
                        menuOpen = false
                        onReact(null)
                    },
                )
            }
        }
    }
}

private val ArticleReaction.labelRes: Int
    get() = when (this) {
        ArticleReaction.Love -> R.string.reader_reaction_love
        ArticleReaction.Good -> R.string.reader_reaction_good
        ArticleReaction.Slop -> R.string.reader_reaction_slop
    }
