package org.dergigi.boris.nostr

import org.dergigi.boris.data.NostrArticle

/** Article / note / URL a kind 7 or 17 reaction points at. */
object ReactionTarget {
    fun ref(event: Nip01Event): BookmarkRef? {
        if (event.kind != Nip01Event.KIND_REACTION && event.kind != Nip01Event.KIND_URL_REACTION) {
            return null
        }
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

    fun url(event: Nip01Event): String? {
        val target = ref(event) ?: return null
        return when (target.kind) {
            BookmarkRefKind.Article -> NostrArticle.fromCoordinate(target.value)?.uri
            BookmarkRefKind.Url -> target.value
            BookmarkRefKind.Note -> runCatching {
                "nostr:${Nip19.noteEncode(target.value.lowercase())}"
            }.getOrNull()
        }
    }
}
