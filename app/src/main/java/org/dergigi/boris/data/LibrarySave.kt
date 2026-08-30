package org.dergigi.boris.data

import org.dergigi.boris.nostr.BookmarkRef
import org.dergigi.boris.nostr.BookmarkRefKind
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.NipB0

object LibrarySave {
    fun contentFromShare(url: String, title: String? = null): ReadableContent? {
        val extracted = UrlExtractor.extract(url) ?: return null
        val label = title?.trim()?.takeIf { it.isNotEmpty() && it != extracted }
        return when (val target = NostrLink.parse(extracted)) {
            is NostrTarget.Article -> ReadableContent(
                url = target.uri,
                title = label,
                articleCoordinate = target.ref.coordinate,
                authorPubkey = target.ref.pointer.pubkey,
            )
            is NostrTarget.Note -> ReadableContent(
                url = target.uri,
                title = label,
                eventId = target.eventId,
                authorPubkey = target.author,
            )
            is NostrTarget.Profile -> null
            null -> ReadableContent(url = extracted, title = label)
        }
    }

    fun isWeb(content: ReadableContent): Boolean =
        NostrLink.parse(content.url) == null &&
            content.articleCoordinate.isNullOrBlank() &&
            content.eventId.isNullOrBlank()

    fun kind(content: ReadableContent, privateBookmark: Boolean): LibrarySaveKind = when {
        isWeb(content) -> LibrarySaveKind.Web
        privateBookmark -> LibrarySaveKind.PrivateList
        else -> LibrarySaveKind.PublicList
    }

    fun hiddenTag(content: ReadableContent): List<String>? {
        content.articleCoordinate?.trim()?.takeIf { it.isNotEmpty() }?.let { return listOf("a", it) }
        content.eventId?.trim()?.takeIf { it.isNotEmpty() }?.let { return listOf("e", it.lowercase()) }
        if (isWeb(content)) {
            val url = content.url.trim().takeIf { it.isNotEmpty() } ?: return null
            return listOf("r", url)
        }
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

enum class LibrarySaveKind {
    Web,
    PrivateList,
    PublicList,
}
