package org.dergigi.boris.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Disk cache of successfully parsed web articles so reopening skips the
 * fetch + readability extraction + markdown conversion pipeline (issue #78).
 * Entries live until the user refreshes the article or LRU trimming removes them.
 */
object ArticleCache {
    const val DIR_NAME = "article_cache"

    private val lock = Any()
    private var dir: File? = null

    fun init(target: File) {
        synchronized(lock) {
            dir = target
            target.mkdirs()
        }
    }

    /** Test hook: forgets the directory so other tests see an uninitialized cache. */
    internal fun reset() {
        synchronized(lock) { dir = null }
    }

    fun cacheKey(url: String): String? {
        val canonical = UrlExtractor.preferHttps(UrlExtractor.normalize(url)).trim()
        if (canonical.isBlank()) return null
        return sha256Hex(canonical).take(16)
    }

    fun load(url: String): ReadableContent? {
        val file = fileFor(url) ?: return null
        if (!file.isFile || file.length() <= 0L) return null
        val content = runCatching { decode(file.readText()) }.getOrNull() ?: return null
        if (content.body.isBlank()) return null
        file.setLastModified(System.currentTimeMillis())
        return content
    }

    fun save(url: String, content: ReadableContent) {
        val file = fileFor(url) ?: return
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(encode(content))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
    }

    fun remove(url: String) {
        fileFor(url)?.delete()
    }

    fun trim(context: Context) {
        val root = synchronized(lock) { dir } ?: return
        val limit = CacheLimit.bytes(context)
        var used = CacheUsage.bytes(context)
        if (used <= limit) return
        val files = root.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() }.orEmpty()
        for (file in files) {
            if (used <= limit) break
            val size = file.length()
            if (file.delete()) used -= size
        }
    }

    internal fun encode(content: ReadableContent): String =
        JSONObject()
            .put("url", content.url)
            .putOpt("title", content.title)
            .putOpt("markdown", content.markdown)
            .putOpt("html", content.html)
            .putOpt("publishedAt", content.publishedAt)
            .putOpt("articleCoordinate", content.articleCoordinate)
            .putOpt("eventId", content.eventId)
            .putOpt("authorPubkey", content.authorPubkey)
            .putOpt("imageUrl", content.imageUrl)
            .putOpt("summary", content.summary)
            .put(
                "sourceZapTags",
                JSONArray().apply {
                    content.sourceZapTags.forEach { tag ->
                        put(JSONArray().apply { tag.forEach(::put) })
                    }
                },
            )
            .put("savedAt", System.currentTimeMillis())
            .toString()

    internal fun decode(json: String): ReadableContent {
        val obj = JSONObject(json)
        fun text(name: String): String? = if (obj.isNull(name)) null else obj.getString(name)
        val zapTags = buildList {
            val rows = obj.optJSONArray("sourceZapTags") ?: return@buildList
            for (i in 0 until rows.length()) {
                val row = rows.getJSONArray(i)
                add(buildList { for (j in 0 until row.length()) add(row.getString(j)) })
            }
        }
        return ReadableContent(
            url = obj.getString("url"),
            title = text("title"),
            markdown = text("markdown"),
            html = text("html"),
            publishedAt = if (obj.isNull("publishedAt")) null else obj.getLong("publishedAt"),
            articleCoordinate = text("articleCoordinate"),
            eventId = text("eventId"),
            authorPubkey = text("authorPubkey"),
            imageUrl = text("imageUrl"),
            summary = text("summary"),
            sourceZapTags = zapTags,
        )
    }

    private fun fileFor(url: String): File? {
        val root = synchronized(lock) { dir } ?: return null
        val key = cacheKey(url) ?: return null
        return File(root, "$key.json")
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
