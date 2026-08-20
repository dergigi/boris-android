package org.dergigi.boris.data

import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object ReadingTimeStore {
    private val entries = ConcurrentHashMap<String, Int>()

    @Volatile
    private var storage: File? = null

    private val diskExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "reading-time-store").apply { isDaemon = true }
    }

    fun init(file: File) {
        if (storage != null) return
        storage = file
        runCatching { load(file) }
    }

    fun get(url: String): Int? =
        keys(url).firstNotNullOfOrNull { entries[it] }

    fun put(url: String, minutes: Int) {
        if (minutes <= 0) return
        keys(url).forEach { entries[it] = minutes }
        val file = storage ?: return
        diskExecutor.execute { runCatching { persist(file) } }
    }

    internal fun clear() {
        entries.clear()
    }

    private fun keys(url: String): List<String> =
        ArticlePreview.keysFor(url)

    private fun persist(file: File) {
        val root = JSONObject()
        entries.entries.take(MAX_ENTRIES).forEach { (url, minutes) ->
            root.put(url, minutes)
        }
        file.parentFile?.mkdirs()
        file.writeText(root.toString())
    }

    private fun load(file: File) {
        if (!file.exists()) return
        val root = JSONObject(file.readText())
        for (url in root.keys()) {
            val minutes = root.optInt(url, 0)
            if (minutes > 0) entries[url] = minutes
        }
    }

    private const val MAX_ENTRIES = 500
}
