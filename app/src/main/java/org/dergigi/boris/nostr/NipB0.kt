package org.dergigi.boris.nostr

import java.net.URI

object NipB0 {
    const val KIND = Nip01Event.KIND_WEB_BOOKMARK

    fun dTag(url: String): String? {
        return try {
            val raw = if (url.contains("://")) url.trim() else "https://${url.trim()}"
            val parsed = URI(raw)
            val host = parsed.host?.lowercase() ?: return null
            val path = parsed.path.orEmpty()
            val query = parsed.rawQuery?.let { "?$it" }.orEmpty()
            val fragment = parsed.rawFragment?.let { "#$it" }.orEmpty()
            host + path + query + fragment
        } catch (_: Exception) {
            null
        }
    }

    fun tags(url: String, title: String?, publishedAt: Long): List<List<String>> {
        val d = dTag(url) ?: return emptyList()
        return buildList {
            add(listOf("d", d))
            add(listOf("published_at", publishedAt.toString()))
            title?.trim()?.takeIf { it.isNotEmpty() }?.let { add(listOf("title", it)) }
        }
    }

    fun unsignedJson(
        url: String,
        title: String?,
        pubkeyHex: String? = null,
        createdAt: Long = System.currentTimeMillis() / 1000,
    ): String? {
        val tags = tags(url, title, createdAt)
        if (tags.isEmpty()) return null
        return Nip01Event.unsignedJson(KIND, "", tags, pubkeyHex, createdAt)
    }

    fun url(event: Nip01Event): String? {
        val d = event.tagValue("d")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return when {
            d.startsWith("http://", ignoreCase = true) -> d
            d.startsWith("https://", ignoreCase = true) -> d
            d.contains("://") -> null
            else -> "https://$d"
        }
    }

    fun title(event: Nip01Event): String? =
        event.tagValue("title")?.trim()?.takeIf { it.isNotEmpty() }

    fun publishedAt(event: Nip01Event): Long {
        val tagged = event.tagValue("published_at")?.toLongOrNull()
        return tagged ?: event.createdAt
    }
}
