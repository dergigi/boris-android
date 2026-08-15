package org.dergigi.boris.data

data class ReadableContent(
    val url: String,
    val title: String? = null,
    val markdown: String? = null,
    val html: String? = null,
    val publishedAt: Long? = null,
) {
    val body: String
        get() = markdown?.takeIf { it.isNotBlank() } ?: html?.let(::stripHtml).orEmpty()
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
