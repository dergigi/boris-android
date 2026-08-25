package org.dergigi.boris.data

/**
 * Fallback keyword list for issue #95. Kept here so it can be tuned without
 * touching UI code. Matching is word-boundary and case-insensitive.
 */
object SensitiveKeywords {
    private val terms = listOf(
        "nsfw",
        "nsfl",
        "nude",
        "nudes",
        "nudity",
        "porn",
        "pornography",
        "xxx",
        "explicit",
        "erotic",
        "erotica",
        "onlyfans",
    )

    private val hashtags = terms.toSet()

    private val pattern = Regex(
        """\b(?:${terms.joinToString("|") { Regex.escape(it) }})\b""",
        RegexOption.IGNORE_CASE,
    )

    fun isHashtag(value: String): Boolean = value.trim().lowercase() in hashtags

    /** The matched term, or null when the text is clean. */
    fun match(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return pattern.find(text)?.value?.lowercase()
    }
}
