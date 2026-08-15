package org.dergigi.boris.data

import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Persistent url -> OgPreview cache so article covers and titles render
 * instantly (and offline) instead of waiting on a live OG fetch.
 */
object OgPreviewCache {
    private data class Entry(val preview: OgPreview, val storedAt: Long)

    private val entries = ConcurrentHashMap<String, Entry>()

    @Volatile
    private var storage: File? = null

    private val diskExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "og-preview-cache").apply { isDaemon = true }
    }

    fun init(file: File) {
        if (storage != null) return
        storage = file
        runCatching { load(file) }
    }

    fun get(url: String): OgPreview? = entries[url]?.preview

    fun put(url: String, preview: OgPreview) {
        if (preview.title == null && preview.imageUrl == null && preview.siteName == null) return
        entries[url] = Entry(preview, System.currentTimeMillis())
        val file = storage ?: return
        diskExecutor.execute { runCatching { persist(file) } }
    }

    internal fun clear() {
        entries.clear()
    }

    private fun persist(file: File) {
        val snapshot = entries.entries
            .sortedByDescending { it.value.storedAt }
            .take(MAX_ENTRIES)
        val root = JSONObject()
        for ((url, entry) in snapshot) {
            root.put(
                url,
                JSONObject()
                    .putOpt("title", entry.preview.title)
                    .putOpt("image", entry.preview.imageUrl)
                    .putOpt("site", entry.preview.siteName)
                    .put("at", entry.storedAt),
            )
        }
        file.parentFile?.mkdirs()
        file.writeText(root.toString())
    }

    private fun load(file: File) {
        if (!file.exists()) return
        val root = JSONObject(file.readText())
        for (url in root.keys()) {
            val obj = root.optJSONObject(url) ?: continue
            val preview = OgPreview(
                title = obj.optString("title").ifEmpty { null },
                imageUrl = obj.optString("image").ifEmpty { null },
                siteName = obj.optString("site").ifEmpty { null },
            )
            entries[url] = Entry(preview, obj.optLong("at"))
        }
    }

    private const val MAX_ENTRIES = 500
}
