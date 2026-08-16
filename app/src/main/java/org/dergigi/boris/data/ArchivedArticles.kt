package org.dergigi.boris.data

import org.dergigi.boris.nostr.Archive
import org.dergigi.boris.nostr.BookmarkRef
import org.dergigi.boris.nostr.BookmarkRefKind
import org.dergigi.boris.nostr.Nip01Event

object ArchivedArticles {
    fun keys(events: List<Nip01Event>): Set<String> =
        events.mapNotNull { event -> Archive.targetRef(event)?.let(::key) }.toSet()

    fun key(ref: BookmarkRef): String = when (ref.kind) {
        BookmarkRefKind.Article -> "a:${ref.value.lowercase()}"
        BookmarkRefKind.Note -> "e:${ref.value.lowercase()}"
        BookmarkRefKind.Url -> "r:${Archive.normalizeUrl(ref.value).lowercase()}"
    }

    fun key(url: String): String? {
        val target = NostrLink.parse(url)
        return when (target) {
            is NostrTarget.Article -> "a:${target.ref.coordinate.lowercase()}"
            is NostrTarget.Note -> "e:${target.eventId.lowercase()}"
            null -> if (url.startsWith("http", ignoreCase = true)) {
                "r:${Archive.normalizeUrl(url).lowercase()}"
            } else {
                null
            }
        }
    }

    fun isArchived(url: String, keys: Set<String>): Boolean {
        val found = key(url) ?: return false
        return found in keys
    }

    fun visible(
        articles: List<HighlightedArticle>,
        keys: Set<String>,
        hideArchived: Boolean,
    ): List<HighlightedArticle> {
        if (!hideArchived || keys.isEmpty()) return articles
        return articles.filterNot { isArchived(it.url, keys) }
    }
}
