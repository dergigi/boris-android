package org.dergigi.boris.data

import android.content.Context
import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ImageSaveLocationStore {
    private const val PREFS = "image-save-location"
    private const val KEY_TREE_URI = "treeUri"

    fun load(context: Context): Uri? =
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TREE_URI, null)
            ?.let(Uri::parse)

    fun save(context: Context, treeUri: Uri) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TREE_URI, treeUri.toString())
            .apply()
    }

    fun clear(context: Context) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TREE_URI)
            .apply()
    }

    fun hasWritePermission(context: Context, treeUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == treeUri && permission.isWritePermission
        }

    fun displayName(treeUri: Uri?): String? = displayName(treeUri?.toString())

    internal fun displayName(rawUri: String?): String? =
        rawUri
            ?.substringAfterLast('/')
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
            ?.substringAfterLast(':')
            ?.takeIf { it.isNotBlank() }
}
