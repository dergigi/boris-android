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
        return extract(absolute.substringBefore('#'))
    }

    fun imageUrls(markdown: String, baseUrl: String? = null): List<String> {
        return markdownImageRegex.findAll(markdown).mapNotNull { match ->
            articleUrl(match.groupValues[1], baseUrl)
        }.distinct().toList()
    }

    private val markdownImageRegex = Regex("""!\[[^\]]*]\(\s*<?([^)\s>]+)>?""")

    private val nonHttpSchemes = setOf("mailto", "tel", "javascript", "sms", "geo", "blob", "data")
}
