package org.dergigi.boris.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import org.dergigi.boris.R
import org.dergigi.boris.nostr.LightningAddress

/** Opens a URI with ACTION_VIEW; toasts if no app can handle it. */
fun openExternalUri(context: Context, uri: String) {
    val opened = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }
    if (opened.isFailure) {
        Toast.makeText(context, context.getString(R.string.highlight_menu_no_app), Toast.LENGTH_SHORT).show()
    }
}

fun openLightningAddress(context: Context, address: String) {
    val parsed = LightningAddress.parse(address) ?: return
    val uri = LightningAddress.uri(parsed) ?: return
    val opened = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }
    if (opened.isFailure) {
        copyLightningAddress(
            context,
            parsed,
            toast = R.string.support_lightning_no_wallet,
        )
    }
}

fun copyLightningAddress(
    context: Context,
    address: String,
    toast: Int = R.string.action_copied,
) {
    val parsed = LightningAddress.parse(address) ?: return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(parsed, parsed))
    Toast.makeText(context, context.getString(toast), Toast.LENGTH_SHORT).show()
}
