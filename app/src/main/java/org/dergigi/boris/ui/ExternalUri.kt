package org.dergigi.boris.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import org.dergigi.boris.R

/** Opens a URI with ACTION_VIEW; toasts if no app can handle it. */
fun openExternalUri(context: Context, uri: String) {
    val opened = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }
    if (opened.isFailure) {
        Toast.makeText(context, context.getString(R.string.highlight_menu_no_app), Toast.LENGTH_SHORT).show()
    }
}
