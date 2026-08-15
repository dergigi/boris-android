package org.dergigi.boris.data

object UrlExtractor {
    private val urlRegex = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val hostLike = Regex(
        """^(https?://)?(www\.)?[\w.-]+\.[a-zA-Z]{2,}(/[^\s]*)?$""",
        RegexOption.IGNORE_CASE,
    )

    fun extract(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val trimmed = text.trim()
        NostrLink.parse(trimmed)?.uri?.let { return it }
        if (looksLikeUrl(trimmed)) return normalize(trimmed)
        val match = urlRegex.find(trimmed)?.value?.trimEnd('.', ',', ';', ')', ']', '"', '\'')
        return match?.let { normalize(it) }
    }

    fun normalize(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun looksLikeUrl(value: String): Boolean {
        val candidate = value.trim()
        if (candidate.contains(' ') || candidate.contains('\n')) return false
        return hostLike.matches(candidate)
    }

    fun articleUrl(href: String?, baseUrl: String? = null): String? {
        if (href.isNullOrBlank()) return null
        val trimmed = href.trim()
        NostrLink.parse(trimmed)?.uri?.let { return it }
        val scheme = trimmed.substringBefore(':', missingDelimiterValue = "").lowercase()
        if (scheme in nonHttpSchemes) return null
        if (trimmed.startsWith("#")) return null

        val absolute = try {
            if (baseUrl.isNullOrBlank()) {
                trimmed
            } else {
                java.net.URI(baseUrl).resolve(trimmed).toString()
            }
        } catch (_: Exception) {
            trimmed
        }
        return NostrLink.parse(absolute)?.uri ?: extract(absolute.substringBefore('#'))
    }

    fun isImageUrl(url: String): Boolean {
        val path = try {
            java.net.URI(url.trim()).path.orEmpty()
        } catch (_: Exception) {
            url.substringBefore('?').substringBefore('#')
        }
        val ext = path.substringAfterLast('/').substringAfterLast('.', "").lowercase()
        return ext in imageExtensions
    }

    fun embedImageLinks(markdown: String): String {
        val linked = markdownLinkRegex.replace(markdown) { match ->
            val url = match.groupValues[2]
            if (isImageUrl(url)) "![${match.groupValues[1]}]($url)" else match.value
        }
        return urlRegex.replace(linked) { match ->
            if (alreadyLinked(linked, match.range.first)) return@replace match.value
            val raw = match.value.trimEnd('.', ',', ';', ')', ']', '"', '\'')
            val suffix = match.value.removePrefix(raw)
            if (isImageUrl(raw)) "![]($raw)$suffix" else match.value
        }
    }

    fun imageUrls(markdown: String, baseUrl: String? = null): List<String> {
        val hits = mutableListOf<Pair<Int, String>>()
        markdownImageRegex.findAll(markdown).forEach { hits += it.range.first to it.groupValues[1] }
        htmlImgRegex.findAll(markdown).forEach { hits += it.range.first to it.groupValues[1] }
        urlRegex.findAll(markdown).forEach { match ->
            val url = match.value.trimEnd('.', ',', ';', ')', ']', '"', '\'')
            if (isImageUrl(url)) hits += match.range.first to url
        }
        return hits.sortedBy { it.first }
            .mapNotNull { articleUrl(it.second, baseUrl) }
            .distinct()
    }

    private fun alreadyLinked(text: String, urlStart: Int): Boolean {
        if (urlStart == 0) return false
        val before = text[urlStart - 1]
        return before == '(' || before == '<'
    }

    private val markdownImageRegex = Regex("""!\[[^\]]*]\(\s*<?([^)\s>]+)>?""")
    private val markdownLinkRegex = Regex(
        """(?<!!)\[([^]]*)]\(\s*<?(https?://[^)\s>]+)>?[^)]*\)""",
        RegexOption.IGNORE_CASE,
    )
    private val htmlImgRegex = Regex(
        """<img\b[^>]*\bsrc\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE,
    )
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "avif", "ico")

    private val nonHttpSchemes = setOf("mailto", "tel", "javascript", "sms", "geo", "blob", "data")
}
