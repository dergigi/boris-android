package org.dergigi.boris.data

import kotlin.random.Random

enum class TimedReadKind {
    Short,
    Long,
}

object TimedReads {
    fun articles(
        items: List<BookmarkItem>,
        archivedKeys: Set<String>,
        kind: TimedReadKind,
        minutes: Map<String, Int>,
        limit: Int,
        random: Random = Random.Default,
    ): List<HighlightedArticle> {
        if (limit <= 0) return emptyList()
        return unreadLibraryItems(items, archivedKeys)
            .filter { item ->
                val mins = minutes[item.url] ?: return@filter false
                when (kind) {
                    TimedReadKind.Short -> mins <= ReadingTime.SHORT_MAX_MINUTES
                    TimedReadKind.Long -> mins >= ReadingTime.LONG_MIN_MINUTES
                }
            }
            .shuffled(random)
            .take(limit)
            .mapNotNull { it.toHighlightedArticle() }
    }
}
