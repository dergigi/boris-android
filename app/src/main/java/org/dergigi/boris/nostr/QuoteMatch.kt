package org.dergigi.boris.nostr

object QuoteMatch {
    fun occurrences(haystack: String, quote: String): List<IntRange> {
        val q = quote.trim()
        if (q.isEmpty()) return emptyList()
        val exact = exactOccurrences(haystack, q)
        if (exact.isNotEmpty()) return exact
        return normalizedOccurrences(haystack, q)
    }

    private fun exactOccurrences(haystack: String, quote: String): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = 0
        while (true) {
            val i = haystack.indexOf(quote, start)
            if (i < 0) break
            out.add(i until i + quote.length)
            start = i + quote.length
        }
        return out
    }

    private fun normalizedOccurrences(haystack: String, quote: String): List<IntRange> {
        val nHay = normalizeWhitespace(haystack)
        val nQuote = normalizeWhitespace(quote)
        if (nHay.isEmpty() || nQuote.isEmpty()) return emptyList()
        val out = mutableListOf<IntRange>()
        var from = 0
        while (true) {
            val i = nHay.indexOf(nQuote, from)
            if (i < 0) break
            val range = mapNormalizedRange(haystack, i, i + nQuote.length)
            if (range != null) out.add(range)
            from = i + nQuote.length
        }
        return out
    }

    internal fun normalizeWhitespace(value: String): String =
        value.replace(Regex("\\s+"), " ").trim()

    internal fun mapNormalizedRange(original: String, normStart: Int, normEnd: Int): IntRange? {
        if (normStart < 0 || normEnd <= normStart) return null
        var i = 0
        while (i < original.length && original[i].isWhitespace()) i++
        var norm = 0
        var startOrig: Int? = null
        var prevWs = false
        while (i <= original.length) {
            if (norm == normStart && startOrig == null) startOrig = i.coerceAtMost(original.length)
            if (norm == normEnd) {
                val start = startOrig ?: return null
                val end = i.coerceAtMost(original.length)
                return if (end > start) start until end else null
            }
            if (i == original.length) break
            val ws = original[i].isWhitespace()
            if (ws) {
                if (!prevWs) norm++
                prevWs = true
            } else {
                norm++
                prevWs = false
            }
            i++
        }
        val start = startOrig ?: return null
        return if (i > start) start until i else null
    }
}
