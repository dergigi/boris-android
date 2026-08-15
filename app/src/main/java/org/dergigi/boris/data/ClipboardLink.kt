package org.dergigi.boris.data

import android.content.ClipboardManager
import android.content.Context

/**
 * Reads a URL or nostr link from the clipboard, once per clipboard content.
 * Android 10+ only allows clipboard access while the app window has focus,
 * so callers should read on focus gain, not on resume.
 */
object ClipboardLink {
    @Volatile
    private var handled: String? = null

    fun read(context: Context): String? {
        val manager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val text = try {
            manager.primaryClip?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.coerceToText(context)?.toString()
        } catch (_: Exception) {
            null
        }
        val url = UrlExtractor.extract(text) ?: return null
        if (url == handled) return null
        return url
    }

    fun markHandled(url: String) {
        handled = url
    }
}
