package org.dergigi.boris.data

import org.dergigi.boris.nostr.BookmarkRef
import org.dergigi.boris.nostr.BookmarkRefKind
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.NipB0

object LibrarySave {
    fun isWeb(content: ReadableContent): Boolean =
        NostrLink.parse(content.url) == null &&
            content.articleCoordinate.isNullOrBlank() &&
            content.eventId.isNullOrBlank()

    fun hiddenTag(content: ReadableContent): List<String>? {
        content.articleCoordinate?.trim()?.takeIf { it.isNotEmpty() }?.let { return listOf("a", it) }
        content.eventId?.trim()?.takeIf { it.isNotEmpty() }?.let { return listOf("e", it.lowercase()) }
        return null
    }

    fun isSaved(
        content: ReadableContent,
        listEvent: Nip01Event?,
        webEvents: List<Nip01Event>,
        hiddenTags: List<List<String>>? = null,
    ): Boolean {
        if (isWeb(content)) {
            val norm = ArticleUrl.normalize(content.url)
            if (webEvents.any { event ->
                    NipB0.url(event)?.let { ArticleUrl.normalize(it) } == norm
                }
            ) {
                return true
            }
            return refsOf(listEvent, hiddenTags).any { ref ->
                ref.kind == BookmarkRefKind.Url && ArticleUrl.normalize(ref.value) == norm
            }
        }
        val coordinate = content.articleCoordinate?.lowercase()
        val eventId = content.eventId?.lowercase()
        return refsOf(listEvent, hiddenTags).any { ref ->
            when (ref.kind) {
                BookmarkRefKind.Article -> coordinate != null && ref.value.equals(coordinate, ignoreCase = true)
                BookmarkRefKind.Note -> eventId != null && ref.value == eventId
                BookmarkRefKind.Url -> false
            }
        }
    }

    private fun refsOf(
        listEvent: Nip01Event?,
        hiddenTags: List<List<String>>?,
    ): List<BookmarkRef> = buildList {
        if (listEvent != null) addAll(Nip51.publicRefs(listEvent))
        if (hiddenTags != null) addAll(Nip51.parseTags(hiddenTags))
    }
}
