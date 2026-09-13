package org.dergigi.boris.data

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Device-local library copies. Separate from evictable caches and never published to relays. */
object ImportedArticles {
    const val DIR_NAME = "imported_articles"
    private var root: File? = null
    private val entries = linkedMapOf<String, BookmarkItem>()
    private val _version = MutableStateFlow(0)
    val version = _version.asStateFlow()

    @Synchronized
    fun init(directory: File) {
        root = directory
        directory.mkdirs()
        entries.clear()
        val index = File(directory, "index.json")
        if (index.exists()) {
            val rows = runCatching { JSONArray(index.readText()) }.getOrElse {
                // Do not overwrite an unreadable index or prevent the rest of Boris from opening.
                root = null
                _version.value++
                return
            }
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                if (!row.has("url") || !row.has("title") || !row.has("createdAt")) continue
                val url = row.getString("url")
                val key = key(url)
                if (File(directory, "$key.json").isFile) entries[key] = BookmarkItem(
                    id = "imported:$key", title = row.getString("title"), url = url,
                    host = ArticleUrl.host(url), imageUrl = null,
                    createdAt = row.optLong("createdAt"), bucket = BookmarkBucket.Web,
                )
            }
        }
        _version.value++
    }

    @Synchronized
    fun items(): List<BookmarkItem> = entries.values.toList()

    @Synchronized
    fun contains(url: String): Boolean = entries.containsKey(key(url))

    @Synchronized
    fun load(url: String): ReadableContent? {
        val key = key(url)
        if (key !in entries) return null
        val directory = root ?: return null
        return runCatching { ArticleCache.decode(File(directory, "$key.json").readText()) }.getOrNull()
    }

    /** Commit body before index; a failed or interrupted write is never advertised as imported. */
    @Synchronized
    fun add(content: ReadableContent): Boolean {
        require(content.body.isNotBlank()) { "Article has no readable text." }
        val directory = checkNotNull(root) { "Import storage is not initialized." }
        val key = key(content.url)
        if (key in entries) return false
        val item = BookmarkItem(
            id = "imported:$key", title = content.title?.takeIf(String::isNotBlank) ?: content.url,
            url = content.url, host = ArticleUrl.host(content.url), imageUrl = null,
            createdAt = System.currentTimeMillis() / 1000, bucket = BookmarkBucket.Web,
        )
        writeAtomic(File(directory, "$key.json"), ArticleCache.encode(content))
        val next = entries + (key to item)
        writeIndex(directory, next.values)
        entries[key] = item
        _version.value++
        return true
    }

    @Synchronized
    fun clear() {
        val directory = root ?: return
        writeIndex(directory, emptyList())
        entries.clear()
        directory.listFiles()?.filter { it.name != "index.json" }?.forEach { it.delete() }
        _version.value++
    }

    private fun writeIndex(directory: File, items: Collection<BookmarkItem>) {
        val rows = JSONArray()
        items.forEach { rows.put(JSONObject().put("url", it.url).put("title", it.title).put("createdAt", it.createdAt)) }
        writeAtomic(File(directory, "index.json"), rows.toString())
    }

    private fun writeAtomic(file: File, text: String) {
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(text)
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun key(url: String): String = MessageDigest.getInstance("SHA-256")
        .digest(ArticleUrl.normalize(url).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
