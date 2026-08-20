package org.dergigi.boris.data

object ArticleCover {
    fun firstMarkdownImage(markdown: String, baseUrl: String? = null): String? =
        UrlExtractor.imageUrls(markdown, baseUrl).firstOrNull()

    /** Drops a leading markdown image when it is the same URL as the hero cover. */
    fun stripLeadingImage(markdown: String, coverUrl: String): String {
        val match = leadingImage.find(markdown) ?: return markdown
        val found = UrlExtractor.articleUrl(match.groupValues[1].trim()) ?: return markdown
        if (ArticleUrl.normalize(found) != ArticleUrl.normalize(coverUrl)) return markdown
        return markdown.removeRange(match.range).trimStart()
    }

    private val leadingImage = Regex(
        """^\s*!\[[^\]]*]\(\s*<?([^)\s>]+)>?[^)]*\)\s*""",
    )
}
