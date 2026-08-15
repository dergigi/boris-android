package org.dergigi.boris.data

data class OgPreview(
    val title: String?,
    val imageUrl: String?,
    val siteName: String?,
)

object OgMeta {
    fun parse(html: String, baseUrl: String): OgPreview {
        val title = meta(html, "og:title")
            ?: meta(html, "twitter:title")
            ?: htmlTitle(html)
        val image = meta(html, "og:image")
            ?: meta(html, "og:image:url")
            ?: meta(html, "twitter:image")
        val siteName = meta(html, "og:site_name")
        return OgPreview(
            title = unescape(title),
            imageUrl = image?.let { absoluteUrl(it, baseUrl) },
            siteName = unescape(siteName),
        )
    }

    private fun meta(html: String, key: String): String? {
        val escaped = Regex.escape(key)
        val forward = Regex(
            """<meta[^>]+(?:property|name)\s*=\s*["']$escaped["'][^>]*content\s*=\s*["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        )
        val reversed = Regex(
            """<meta[^>]+content\s*=\s*["']([^"']+)["'][^>]*(?:property|name)\s*=\s*["']$escaped["']""",
            RegexOption.IGNORE_CASE,
        )
        return forward.find(html)?.groupValues?.getOrNull(1)?.trim()?.ifEmpty { null }
            ?: reversed.find(html)?.groupValues?.getOrNull(1)?.trim()?.ifEmpty { null }
    }

    private fun htmlTitle(html: String): String? {
        val raw = titleRegex.find(html)?.groupValues?.getOrNull(1) ?: return null
        return raw.replace(Regex("\\s+"), " ").trim().ifEmpty { null }
    }

    private fun unescape(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .trim()
            .ifEmpty { null }
    }

    private fun absoluteUrl(raw: String, baseUrl: String): String? {
        val trimmed = raw.trim()
        if (trimmed.startsWith("//")) {
            return UrlExtractor.extract("https:$trimmed")
        }
        return UrlExtractor.articleUrl(trimmed, baseUrl)
    }

    private val titleRegex = Regex(
        """<title[^>]*>(.*?)</title>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
}
