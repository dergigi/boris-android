package org.dergigi.boris.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.nostr.NeventPointer
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.RelayList

/** Actions for the 3-dot menu on a highlight card. Null callbacks hide their item. */
data class HighlightCardMenu(
    val highlightId: String,
    val authorHex: String?,
    val onGoToQuote: (() -> Unit)? = null,
    val onViewProfile: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null,
)

private const val NJUMP_BASE = "https://njump.to"

@Composable
fun HighlightCardMenuButton(
    menu: HighlightCardMenu,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val nevent = remember(menu.highlightId, menu.authorHex) {
        runCatching {
            Nip19.neventEncode(
                NeventPointer(
                    eventId = menu.highlightId,
                    relays = RelayList.FALLBACK.take(3),
                    author = menu.authorHex,
                    kind = Nip01Event.KIND_HIGHLIGHT,
                ),
            )
        }.getOrNull()
    }
    Box(modifier = modifier) {
        IconButton(
            onClick = { open = true },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = stringResource(R.string.highlight_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
        ) {
            menu.onGoToQuote?.let { action ->
                MenuItem(R.string.highlight_menu_go_to_quote, Icons.Outlined.FormatQuote) {
                    open = false
                    action()
                }
            }
            menu.onViewProfile?.let { action ->
                MenuItem(R.string.highlight_menu_view_profile, Icons.Outlined.Person) {
                    open = false
                    action()
                }
            }
            if (nevent != null) {
                MenuItem(R.string.highlight_menu_open_njump, Icons.AutoMirrored.Outlined.OpenInNew) {
                    open = false
                    openExternal(context, "$NJUMP_BASE/$nevent")
                }
                MenuItem(R.string.highlight_menu_open_native, Icons.Outlined.Smartphone) {
                    open = false
                    openExternal(context, "nostr:$nevent")
                }
            }
            menu.onDelete?.let {
                MenuItem(R.string.highlight_menu_delete, Icons.Outlined.Delete) {
                    open = false
                    confirmDelete = true
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.highlight_delete_title)) },
            text = { Text(stringResource(R.string.highlight_delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        menu.onDelete?.invoke()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.highlight_menu_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.highlight_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun MenuItem(
    labelRes: Int,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(labelRes)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

private fun openExternal(context: Context, uri: String) {
    openExternalUri(context, uri)
}
