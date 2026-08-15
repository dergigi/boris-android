package org.dergigi.boris.nostr

object Nip23 {
    const val KIND = Nip01Event.KIND_LONG_FORM

    fun identifier(event: Nip01Event): String? =
        event.tagValue("d")?.trim()?.takeIf { it.isNotEmpty() }

    fun title(event: Nip01Event): String? =
        event.tagValue("title")?.trim()?.takeIf { it.isNotEmpty() }

    fun summary(event: Nip01Event): String? =
        event.tagValue("summary")?.trim()?.takeIf { it.isNotEmpty() }

    fun image(event: Nip01Event): String? =
        event.tagValue("image")?.trim()?.takeIf { it.isNotEmpty() }

    fun publishedAt(event: Nip01Event): Long =
        event.tagValue("published_at")?.toLongOrNull() ?: event.createdAt
}
