package org.dergigi.boris.nostr

import org.dergigi.boris.data.ArticleUrl
import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.ReadableContent
import java.net.URL

object Archive {
    const val EMOJI = "📚"

    fun isArchive(event: Nip01Event): Boolean =
        event.content.trim() == EMOJI &&
            (event.kind == Nip01Event.KIND_REACTION || event.kind == Nip01Event.KIND_URL_REACTION)

    fun targetRef(event: Nip01Event): BookmarkRef? {
        if (!isArchive(event)) return null
        if (event.kind == Nip01Event.KIND_URL_REACTION) {
            val url = event.tags.lastOrNull { it.size >= 2 && it[0] == "r" }?.getOrNull(1)
            if (!url.isNullOrBlank()) return BookmarkRef(BookmarkRefKind.Url, url)
            return null
        }
        val address = event.tags.lastOrNull { it.size >= 2 && it[0] == "a" }?.getOrNull(1)
        if (!address.isNullOrBlank() && NostrArticle.fromCoordinate(address) != null) {
            return BookmarkRef(BookmarkRefKind.Article, address)
        }
        val eventId = event.tags.lastOrNull { it.size >= 2 && it[0] == "e" }?.getOrNull(1)?.lowercase()
        if (eventId != null && eventId.length == 64) {
            return BookmarkRef(BookmarkRefKind.Note, eventId)
        }
        return null
    }

    fun isArchiveKind(kind: Int): Boolean =
        kind == Nip01Event.KIND_REACTION || kind == Nip01Event.KIND_URL_REACTION

    fun normalizeUrl(url: String): String {
        return try {
            val parsed = URL(url.trim())
            var normalized = parsed.toString()
            val hash = normalized.indexOf('#')
            if (hash >= 0) normalized = normalized.substring(0, hash)
            normalized.trimEnd('/')
        } catch (_: Exception) {
            url.trim().substringBefore('#').trimEnd('/')
        }
    }

    /** Same identity Home cards use, so www / utm / http variants still match. */
    fun urlKey(url: String): String = "r:${ArticleUrl.normalize(url).lowercase()}"

    fun urlQueryTags(url: String): List<String> =
        listOf(ArticleUrl.normalize(url), normalizeUrl(url))
            .map { it.trim() }
            .filter { it.startsWith("http", ignoreCase = true) }
            .distinct()

    fun tags(content: ReadableContent): List<List<String>>? {
        val eventId = content.eventId?.trim()?.takeIf { it.length == 64 }
        val author = content.authorPubkey?.trim()?.takeIf { it.length == 64 }
        if (eventId != null && author != null) {
            val kind = if (!content.articleCoordinate.isNullOrBlank()) {
                Nip01Event.KIND_LONG_FORM
            } else {
                Nip01Event.KIND_TEXT_NOTE
            }
            return buildList {
                add(listOf("e", eventId.lowercase()))
                add(listOf("p", author.lowercase()))
                add(listOf("k", kind.toString()))
                content.articleCoordinate?.trim()?.takeIf { it.isNotEmpty() }?.let { add(listOf("a", it)) }
            }
        }
        if (content.url.startsWith("http", ignoreCase = true)) {
            return listOf(listOf("r", ArticleUrl.normalize(content.url)))
        }
        return null
    }

    fun kind(content: ReadableContent): Int? {
        val eventId = content.eventId?.trim()?.takeIf { it.length == 64 }
        val author = content.authorPubkey?.trim()?.takeIf { it.length == 64 }
        if (eventId != null && author != null) return Nip01Event.KIND_REACTION
        if (content.url.startsWith("http", ignoreCase = true)) return Nip01Event.KIND_URL_REACTION
        return null
    }

    fun unsignedJson(
        content: ReadableContent,
        pubkeyHex: String? = null,
        createdAt: Long = System.currentTimeMillis() / 1000,
    ): String? {
        val kind = kind(content) ?: return null
        val tags = tags(content) ?: return null
        return Nip01Event.unsignedJson(kind, EMOJI, tags, pubkeyHex, createdAt)
    }

    fun deleteTags(eventIds: List<String>): List<List<String>> =
        eventIds.mapNotNull { id ->
            id.trim().lowercase().takeIf { it.length == 64 }?.let { listOf("e", it) }
        }.distinct()

    fun deleteUnsignedJson(
        eventIds: List<String>,
        pubkeyHex: String? = null,
        createdAt: Long = System.currentTimeMillis() / 1000,
    ): String? {
        val tags = deleteTags(eventIds)
        if (tags.isEmpty()) return null
        return Nip01Event.unsignedJson(Nip01Event.KIND_DELETION, "unarchive", tags, pubkeyHex, createdAt)
    }
}
