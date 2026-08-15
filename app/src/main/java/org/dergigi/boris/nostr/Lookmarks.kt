package org.dergigi.boris.nostr

import org.dergigi.boris.data.NostrArticle

object Lookmarks {
    const val EMOJI = "👀"

    fun isLook(event: Nip01Event): Boolean =
        event.kind == Nip01Event.KIND_REACTION && event.content.trim() == EMOJI

    fun targetRef(event: Nip01Event): BookmarkRef? {
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
}
