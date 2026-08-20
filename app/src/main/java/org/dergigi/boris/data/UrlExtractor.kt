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
            is NostrTarget.Article, is NostrTarget.Note, is NostrTarget.Profile -> return target.uri
            null -> Unit
        }
        if (looksLikeUrl(trimmed)) return normalize(trimmed)
        val match = urlRegex.find(trimmed)?.value?.trimEnd('.', ',', ';', ')', ']', '"', '\'')
        match?.let { return normalize(it) }
        return bareProfileUri(trimmed)
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
     * Cleartext HTTP is blocked app-wide. Article GETs and image fetches
     * (Coil, gallery, save) upgrade `http://` to `https://` before the request.
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
            null -> extract(absolute.substringBefore('#')).takeUnless {
                NostrLink.parse(it) is NostrTarget.Profile
            }
        }
    }

    fun isImageUrl(url: String): Boolean {
        val trimmed = url.trim()
        val uri = try {
            java.net.URI(trimmed)
        } catch (_: Exception) {
            null
        }
        val path = uri?.path.orEmpty().ifEmpty {
            trimmed.substringBefore('?').substringBefore('#')
        }
        val ext = path.substringAfterLast('/').substringAfterLast('.', "").lowercase()
        if (ext in imageExtensions) return true
        val host = uri?.host?.lowercase() ?: return false
        return host in imageHosts
    }

    fun embedImageLinks(markdown: String): String {
        val (protected, restore) = protectCode(markdown)
        val prepared = upgradeImageHttpUrls(protected)
        val embedded = urlRegex.replace(prepared) { match ->
            if (alreadyLinked(prepared, match.range.first)) return@replace match.value
            val raw = match.value.trimEnd('.', ',', ';', ')', ']', '"', '\'')
            val suffix = match.value.removePrefix(raw)
            if (isImageUrl(raw)) "![](${preferHttps(raw)})$suffix" else match.value
        }
        return restore(embedded)
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

    private fun bareProfileUri(text: String): String? {
        if (text.any { it.isWhitespace() }) return null
        val token = text.lowercase()
        if (!token.startsWith("npub1") && !token.startsWith("nprofile1")) return null
        return (NostrLink.parse("nostr:$token") as? NostrTarget.Profile)?.uri
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
    private val htmlImgRegex = Regex(
        """<img\b[^>]*\bsrc\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE,
    )
    private fun protectCode(text: String): Pair<String, (String) -> String> {
        val slots = mutableListOf<String>()
        fun stash(match: MatchResult): String {
            slots += match.value
            return "\u0000${slots.lastIndex}\u0000"
        }
        val fenced = codeFenceRegex.replace(text, ::stash)
        val protected = inlineCodeRegex.replace(fenced, ::stash)
        return protected to { restored ->
            var next = restored
            slots.indices.reversed().forEach { i ->
                next = next.replace("\u0000$i\u0000", slots[i])
            }
            next
        }
    }

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "avif", "ico")
    private val imageHosts = setOf(
        "image.nostr.build",
        "media.nostr.build",
        "i.nostr.build",
    )
    private val codeFenceRegex = Regex("""(?s)(?:```|~~~)[^\n]*\n.*?(?:```|~~~)""")
    private val inlineCodeRegex = Regex("""`+[^`]+`+""")

    private val nonHttpSchemes = setOf("mailto", "tel", "javascript", "sms", "geo", "blob", "data")
}
