package org.dergigi.boris.ui.feed

import org.dergigi.boris.data.ArchivedArticles

enum class HighlightCollapseReason {
    Article,
    Author,
}

sealed interface FeedHighlightRow {
    val key: String
    val sortAt: Long

    data class Open(val item: FeedItem) : FeedHighlightRow {
        override val key: String get() = "h:${item.id}"
        override val sortAt: Long get() = item.createdAt
    }

    data class Collapsed(
        val collapseKey: String,
        val targetKey: String,
        val count: Int,
        val reason: HighlightCollapseReason,
        val source: String?,
        val authorName: String?,
        val authorHex: String?,
        override val sortAt: Long,
    ) : FeedHighlightRow {
        override val key: String get() = collapseKey
    }
}

object FeedHighlightCollapse {
    fun articleKey(item: FeedItem): String? =
        item.url?.let(ArchivedArticles::key)

    fun rows(
        items: List<FeedItem>,
        collapsedArticles: Set<String>,
        collapsedAuthors: Set<String>,
    ): List<FeedHighlightRow> {
        val articleGroups = items.groupBy { articleKey(it) }
        val remainingForAuthors = items.filter { item ->
            val key = articleKey(item)
            key == null || key !in collapsedArticles
        }
        val authorGroups = remainingForAuthors.groupBy { it.authorHex.lowercase() }
        val seenArticles = mutableSetOf<String>()
        val seenAuthors = mutableSetOf<String>()
        return buildList {
            for (item in items) {
                val aKey = articleKey(item)
                if (aKey != null && aKey in collapsedArticles) {
                    if (seenArticles.add(aKey)) {
                        val group = articleGroups.getValue(aKey)
                        add(
                            FeedHighlightRow.Collapsed(
                                collapseKey = "a:$aKey",
                                targetKey = aKey,
                                count = group.size,
                                reason = HighlightCollapseReason.Article,
                                source = group.firstNotNullOfOrNull { it.host?.takeIf(String::isNotBlank) },
                                authorName = null,
                                authorHex = null,
                                sortAt = item.createdAt,
                            ),
                        )
                    }
                    continue
                }
                val uKey = item.authorHex.lowercase()
                if (uKey in collapsedAuthors) {
                    if (seenAuthors.add(uKey)) {
                        val group = authorGroups.getValue(uKey)
                        add(
                            FeedHighlightRow.Collapsed(
                                collapseKey = "u:$uKey",
                                targetKey = uKey,
                                count = group.size,
                                reason = HighlightCollapseReason.Author,
                                source = null,
                                authorName = item.authorName,
                                authorHex = uKey,
                                sortAt = item.createdAt,
                            ),
                        )
                    }
                    continue
                }
                add(FeedHighlightRow.Open(item))
            }
        }
    }
}
