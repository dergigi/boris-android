package com.readwithboris.data

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
}
