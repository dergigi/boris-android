package org.dergigi.boris.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import org.dergigi.boris.R

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
