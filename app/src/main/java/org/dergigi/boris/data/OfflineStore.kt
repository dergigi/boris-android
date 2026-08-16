package org.dergigi.boris.data

import android.content.Context
import org.json.JSONArray
import java.io.File

/** Persistent record of article URLs whose text has been cached for offline reading. */
object OfflineStore {
    private val lock = Any()
    private var file: File? = null
    private val downloaded = LinkedHashSet<String>()

    fun init(target: File) {
        synchronized(lock) {
            file = target
            downloaded.clear()
            if (target.exists()) {
                runCatching {
                    val array = JSONArray(target.readText())
                    for (i in 0 until array.length()) downloaded.add(array.getString(i))
                }
            }
        }
    }

    fun isDownloaded(url: String): Boolean = synchronized(lock) { url in downloaded }

    fun downloadedCount(urls: Collection<String>): Int =
        synchronized(lock) { urls.count { it in downloaded } }

    fun markDownloaded(url: String) {
        synchronized(lock) {
            if (!downloaded.add(url)) return
            val target = file ?: return
            runCatching {
                val array = JSONArray()
                downloaded.forEach(array::put)
                target.writeText(array.toString())
            }
        }
    }
}

/** Device-local storage limit for the article and image caches. */
object CacheLimit {
    private const val PREFS = "offline_prefs"
    private const val KEY_MB = "cacheLimitMb"
    const val DEFAULT_MB = 1024
    val OPTIONS_MB = listOf(210, 512, 1024, 2048, 5120)

    fun megabytes(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MB, DEFAULT_MB)

    fun bytes(context: Context): Long = megabytes(context) * 1024L * 1024L

    fun set(context: Context, megabytes: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MB, megabytes)
            .apply()
    }
}
