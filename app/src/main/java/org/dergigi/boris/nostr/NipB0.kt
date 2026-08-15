package org.dergigi.boris.nostr

object NipB0 {
    const val KIND = Nip01Event.KIND_WEB_BOOKMARK

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
