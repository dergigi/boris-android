package org.dergigi.boris.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import org.dergigi.boris.R
import org.dergigi.boris.data.NostrLink

fun shareArticleLink(context: Context, title: String?, url: String) {
    val shareUrl = NostrLink.parse(url)?.publicUrl ?: url
    val text = if (title.isNullOrBlank()) shareUrl else "$title\n$shareUrl"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (!title.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.reader_share_article)),
    )
}

fun copyArticleLink(context: Context, clipboard: ClipboardManager, url: String) {
    clipboard.setText(AnnotatedString(NostrLink.copyText(url)))
    Toast.makeText(context, context.getString(R.string.reader_copied), Toast.LENGTH_SHORT).show()
}

fun openOriginalArticle(context: Context, url: String) {
    val target = NostrLink.parse(url)?.publicUrl ?: url
    openExternalUri(context, target)
}
