package org.dergigi.boris.ui.reader

import org.dergigi.boris.nostr.QuoteMatch

data class ArticleFindHit(
    val index: Int,
    val start: Int,
    val end: Int,
    val match: String,
    val snippet: String,
)

object ArticleFind {
    const val HIGHLIGHT_ID = "find"

    fun hits(haystack: String, query: String, contextChars: Int = 42): List<ArticleFindHit> {
        val q = query.trim()
        if (q.isEmpty() || haystack.isEmpty()) return emptyList()
        return QuoteMatch.occurrences(haystack, q, ignoreCase = true).mapIndexed { index, range ->
            val end = range.last + 1
            ArticleFindHit(
                index = index,
                start = range.first,
                end = end,
                match = haystack.substring(range.first, end),
                snippet = snippet(haystack, range.first, end, contextChars),
            )
        }
    }

    fun painted(query: String): PaintedHighlight? {
        val q = query.trim()
        if (q.isEmpty()) return null
        return PaintedHighlight(
            id = HIGHLIGHT_ID,
            quote = q,
            mine = false,
            find = true,
            ignoreCase = true,
        )
    }

    private fun snippet(haystack: String, start: Int, end: Int, contextChars: Int): String {
        val from = (start - contextChars).coerceAtLeast(0)
        val to = (end + contextChars).coerceAtMost(haystack.length)
        val prefix = if (from > 0) "…" else ""
        val suffix = if (to < haystack.length) "…" else ""
        return prefix + haystack.substring(from, to).replace(Regex("\\s+"), " ").trim() + suffix
    }
}
