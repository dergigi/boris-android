package org.dergigi.boris.ui.you

import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import org.dergigi.boris.R
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.ui.TopBarMenuItem
import org.dergigi.boris.ui.openExternalUri

/** Copy / share / njump / native actions for a profile npub. */
@Composable
fun profileLinkMenuItems(npub: String): List<TopBarMenuItem> {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val publicUrl = remember(npub) { NostrLink.gatewayUrl(npub) }
    val copyLabel = stringResource(R.string.action_copy_link)
    val shareLabel = stringResource(R.string.action_share)
    val njumpLabel = stringResource(R.string.highlight_menu_open_njump)
    val nativeLabel = stringResource(R.string.highlight_menu_open_native)
    return listOf(
        TopBarMenuItem(
            label = copyLabel,
            icon = Icons.Filled.ContentCopy,
            onClick = {
                clipboard.setText(AnnotatedString(npub))
                Toast.makeText(context, R.string.action_copied, Toast.LENGTH_SHORT).show()
            },
        ),
        TopBarMenuItem(
            label = shareLabel,
            icon = Icons.Filled.Share,
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, publicUrl)
                }
                context.startActivity(Intent.createChooser(intent, shareLabel))
            },
        ),
        TopBarMenuItem(
            label = njumpLabel,
            icon = Icons.AutoMirrored.Outlined.OpenInNew,
            onClick = { openExternalUri(context, publicUrl) },
        ),
        TopBarMenuItem(
            label = nativeLabel,
            icon = Icons.Outlined.Smartphone,
            onClick = { openExternalUri(context, "nostr:$npub") },
        ),
    )
}
