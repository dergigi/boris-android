package org.dergigi.boris.data

data class ReadableContent(
    val url: String,
    val title: String? = null,
    val markdown: String? = null,
    val html: String? = null,
    val publishedAt: Long? = null,
    val articleCoordinate: String? = null,
    val eventId: String? = null,
    val authorPubkey: String? = null,
    val imageUrl: String? = null,
    val summary: String? = null,
    val sourceZapTags: List<List<String>> = emptyList(),
    val tags: List<List<String>> = emptyList(),
) {
    val displaySummary: String?
        get() = summary?.takeUnless { summaryDuplicatesOpeningParagraph(it, body) }

    val body: String
        get() = markdown?.takeIf { it.isNotBlank() } ?: html?.let(::stripHtml).orEmpty()
}

internal fun summaryDuplicatesOpeningParagraph(summary: String, body: String): Boolean {
    val normalizedSummary = normalizeArticleTextForComparison(summary)
    if (normalizedSummary.isBlank()) return false
    return body
        .split(Regex("\\n\\s*\\n"))
        .asSequence()
        .map(::normalizeArticleTextForComparison)
        .filter { it.isNotBlank() }
        .firstOrNull()
        ?.let { it == normalizedSummary || it.startsWith("$normalizedSummary ") }
        ?: false
}

private fun normalizeArticleTextForComparison(text: String): String {
    return text
        .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), " ")
        .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("[*_`#>~]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun stripHtml(html: String): String {
    return html
        .replace(Regex("(?i)<script[\\s\\S]*?</script>"), "")
        .replace(Regex("(?i)<style[\\s\\S]*?</style>"), "")
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</p>"), "\n\n")
        .replace(Regex("(?i)</h[1-6]>"), "\n\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .trim()
}
