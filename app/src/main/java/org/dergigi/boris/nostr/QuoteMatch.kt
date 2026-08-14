package org.dergigi.boris.nostr

object QuoteMatch {
    fun occurrences(haystack: String, quote: String): List<IntRange> {
        val q = quote.trim()
        if (q.isEmpty()) return emptyList()
        val out = mutableListOf<IntRange>()
        var start = 0
        while (true) {
            val i = haystack.indexOf(q, start)
            if (i < 0) break
            out.add(i until i + q.length)
            start = i + q.length
        }
        return out
    }
}
