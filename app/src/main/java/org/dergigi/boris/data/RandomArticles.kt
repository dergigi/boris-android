package org.dergigi.boris.data

import kotlin.random.Random

/** Picks random unread library bookmarks for the Home screen. */
object RandomArticles {
    fun articles(
        items: List<BookmarkItem>,
        archivedKeys: Set<String>,
        limit: Int,
        random: Random = Random.Default,
    ): List<HighlightedArticle> {
        if (limit <= 0) return emptyList()
        return unreadLibraryItems(items, archivedKeys)
            .shuffled(random)
            .take(limit)
            .mapNotNull { it.toHighlightedArticle() }
    }
}

internal fun unreadLibraryItems(
    items: List<BookmarkItem>,
    archivedKeys: Set<String>,
): List<BookmarkItem> {
    val pool = LinkedHashMap<String, BookmarkItem>()
    for (item in items) {
        val url = item.url ?: continue
        if (ArchivedArticles.isArchived(url, archivedKeys)) continue
        pool.putIfAbsent(url, item)
    }
    return pool.values.toList()
}

internal fun BookmarkItem.toHighlightedArticle(): HighlightedArticle? {
    val url = url ?: return null
    val host = host
        ?: when (val target = NostrLink.parse(url)) {
            is NostrTarget.Article -> target.ref.pointer.identifier.ifBlank { "nostr" }
            is NostrTarget.Note -> "nostr"
            is NostrTarget.Profile -> host ?: url
            null -> ArticleUrl.host(url) ?: url
        }
    return HighlightedArticles.decorate(
        HighlightedArticle(
            url = url,
            host = host,
            title = title.ifBlank { host },
            imageUrl = imageUrl,
            highlightedAt = createdAt,
        ),
    )
}
