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
        when (val target = NostrLink.parse(trimmed)) {
            is NostrTarget.Article, is NostrTarget.Note -> return target.uri
            is NostrTarget.Profile -> return null
            null -> Unit
        }
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

    /**
     * Cleartext HTTP is blocked app-wide. Image GETs (Coil, gallery, save) must use HTTPS.
     * Article opens still keep `http://` for Jina; only image fetches call this.
     */
    fun preferHttps(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true)) {
            "https://" + trimmed.substring(7)
        } else {
            trimmed
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
        when (val target = NostrLink.parse(trimmed)) {
            is NostrTarget.Article, is NostrTarget.Note -> return target.uri
            is NostrTarget.Profile -> return null
            null -> Unit
        }
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
        return when (val target = NostrLink.parse(absolute)) {
            is NostrTarget.Article, is NostrTarget.Note -> target.uri
            is NostrTarget.Profile -> null
            null -> extract(absolute.substringBefore('#'))
        }
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
        val prepared = upgradeImageHttpUrls(markdown)
        val linked = markdownLinkRegex.replace(prepared) { match ->
            val url = match.groupValues[2]
            if (isImageUrl(url)) {
                "![${match.groupValues[1]}](${preferHttps(url)})"
            } else {
                match.value
            }
        }
        return urlRegex.replace(linked) { match ->
            if (alreadyLinked(linked, match.range.first)) return@replace match.value
            val raw = match.value.trimEnd('.', ',', ';', ')', ']', '"', '\'')
            val suffix = match.value.removePrefix(raw)
            if (isImageUrl(raw)) "![](${preferHttps(raw)})$suffix" else match.value
        }
    }

    /** Rewrite http:// image srcs in markdown/HTML so Coil can fetch them. */
    fun upgradeImageHttpUrls(markdown: String): String {
        var out = markdownImageRegex.replace(markdown) { match ->
            replaceUrlInMatch(match.value, match.groupValues[1])
        }
        out = htmlImgRegex.replace(out) { match ->
            replaceUrlInMatch(match.value, match.groupValues[1])
        }
        return out
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
            .mapNotNull { articleUrl(it.second, baseUrl)?.let(::preferHttps) }
            .distinct()
    }

    private fun replaceUrlInMatch(whole: String, url: String): String {
        val upgraded = preferHttps(url)
        return if (upgraded == url) whole else whole.replace(url, upgraded)
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
