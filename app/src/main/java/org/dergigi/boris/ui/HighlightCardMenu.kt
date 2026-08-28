package org.dergigi.boris.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.HighlightShare
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
    val onIgnoreArticle: (() -> Unit)? = null,
    val onIgnoreAuthor: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null,
)

val HighlightCardMenu.hasHideActions: Boolean
    get() = onIgnoreArticle != null || onIgnoreAuthor != null

private const val NJUMP_BASE = "https://njump.to"

@Composable
fun HighlightCardMenuButton(
    menu: HighlightCardMenu? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val nevent = remember(menu?.highlightId, menu?.authorHex) {
        val id = menu?.highlightId ?: return@remember null
        runCatching {
            Nip19.neventEncode(
                NeventPointer(
                    eventId = id,
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
            menu?.onGoToQuote?.let { action ->
                MenuItem(R.string.highlight_menu_go_to_quote, Icons.Outlined.FormatQuote) {
                    open = false
                    action()
                }
            }
            menu?.onViewProfile?.let { action ->
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
            menu?.onDelete?.let {
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
                        menu?.onDelete?.invoke()
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
fun HighlightShareMenuButton(
    shareUrl: String,
    shareQuote: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val shareQuoteLabel = stringResource(R.string.highlight_menu_share_quote)
    val shareArticleLabel = stringResource(R.string.highlight_menu_share_article)
    val shareQuoteTarget = remember(shareUrl, shareQuote) {
        HighlightShare.url(shareUrl, shareQuote)
    }
    val shareArticleTarget = remember(shareUrl, shareQuote) {
        HighlightShare.articleUrl(shareUrl, shareQuote)
    }
    if (shareQuoteTarget.isBlank() && shareArticleTarget.isNullOrBlank()) return
    Box(modifier = modifier) {
        IconButton(
            onClick = { open = true },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.highlight_share_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
        ) {
            if (shareQuoteTarget.isNotBlank()) {
                MenuItem(R.string.highlight_menu_share_quote, Icons.Outlined.Share) {
                    open = false
                    sharePlainText(context, shareQuoteTarget, shareQuoteLabel)
                }
            }
            shareArticleTarget?.takeIf { it.isNotBlank() }?.let { target ->
                MenuItem(R.string.highlight_menu_share_article, Icons.AutoMirrored.Outlined.Article) {
                    open = false
                    sharePlainText(context, target, shareArticleLabel)
                }
            }
        }
    }
}

@Composable
fun HighlightHideMenuButton(
    menu: HighlightCardMenu,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { open = true },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.VisibilityOff,
                contentDescription = stringResource(R.string.highlight_hide_menu),
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
        ) {
            menu.onIgnoreArticle?.let { action ->
                MenuItem(R.string.highlight_menu_ignore_article, Icons.Outlined.VisibilityOff) {
                    open = false
                    action()
                }
            }
            menu.onIgnoreAuthor?.let { action ->
                MenuItem(R.string.highlight_menu_ignore_author, Icons.Outlined.VisibilityOff) {
                    open = false
                    action()
                }
            }
        }
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

private fun sharePlainText(context: Context, text: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private fun openExternal(context: Context, uri: String) {
    openExternalUri(context, uri)
}
