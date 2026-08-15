package org.dergigi.boris.data

object ArticleCover {
    fun imageFromJina(text: String): String? {
        val header = header(text)
        val raw = imageField.find(header)?.groupValues?.getOrNull(1)?.trim()
        return raw?.takeIf { it.startsWith("http") }?.let { UrlExtractor.articleUrl(it) }
    }

    fun descriptionFromJina(text: String): String? {
        val header = header(text)
        return descriptionField.find(header)?.groupValues?.getOrNull(1)?.trim()?.ifBlank { null }
    }

    fun firstMarkdownImage(markdown: String, baseUrl: String? = null): String? =
        UrlExtractor.imageUrls(markdown, baseUrl).firstOrNull()

    /** Drops a leading markdown image when it is the same URL as the hero cover. */
    fun stripLeadingImage(markdown: String, coverUrl: String): String {
        val match = leadingImage.find(markdown) ?: return markdown
        val found = UrlExtractor.articleUrl(match.groupValues[1].trim()) ?: return markdown
        if (ArticleUrl.normalize(found) != ArticleUrl.normalize(coverUrl)) return markdown
        return markdown.removeRange(match.range).trimStart()
    }

    private fun header(text: String): String {
        val end = text.indexOf("Markdown Content:", ignoreCase = true)
        return if (end >= 0) text.substring(0, end) else text.take(2_000)
    }

    private val imageField = Regex(
        """^Image URL:\s*(.+)$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )
    private val descriptionField = Regex(
        """^Description:\s*(.+)$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )
    private val leadingImage = Regex(
        """^\s*!\[[^\]]*]\(\s*<?([^)\s>]+)>?[^)]*\)\s*""",
    )
}
