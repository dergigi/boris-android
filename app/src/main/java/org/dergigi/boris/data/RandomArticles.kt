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
        val pool = LinkedHashMap<String, BookmarkItem>()
        for (item in items) {
            val url = item.url ?: continue
            if (ArchivedArticles.isArchived(url, archivedKeys)) continue
            pool.putIfAbsent(url, item)
        }
        if (pool.isEmpty()) return emptyList()
        return pool.values
            .shuffled(random)
            .take(limit)
            .map { item ->
                val url = item.url!!
                val host = item.host
                    ?: when (val target = NostrLink.parse(url)) {
                        is NostrTarget.Article -> target.ref.pointer.identifier.ifBlank { "nostr" }
                        is NostrTarget.Note -> "nostr"
                        null -> ArticleUrl.host(url) ?: url
                    }
                HighlightedArticles.decorate(
                    HighlightedArticle(
                        url = url,
                        host = host,
                        title = item.title.ifBlank { host },
                        imageUrl = item.imageUrl,
                        highlightedAt = item.createdAt,
                    ),
                )
            }
    }
}
