package org.dergigi.boris.ui.home

import org.dergigi.boris.data.HighlightedArticle

object ExploreRows {
    const val LIMIT = 21
    const val CANDIDATES = LIMIT * 3

    /**
     * Walk [order] and keep the first row that claims each URL, then fill
     * each row back up to [limit] from leftover candidates.
     *
     * Most highlighted is a ranking, not another recency row, so it keeps
     * its own list instead of inheriting whatever is left.
     */
    fun fill(
        order: List<String>,
        rows: Map<String, List<HighlightedArticle>>,
        limit: Int = LIMIT,
    ): Map<String, List<HighlightedArticle>> {
        val seen = LinkedHashSet<String>()
        return order.associateWith { id ->
            val exclusive = id != HomeSections.MOST
            val kept = ArrayList<HighlightedArticle>(limit)
            for (item in rows[id].orEmpty()) {
                if (exclusive && !seen.add(item.url)) continue
                kept.add(item)
                if (kept.size >= limit) break
            }
            kept
        }
    }
}
