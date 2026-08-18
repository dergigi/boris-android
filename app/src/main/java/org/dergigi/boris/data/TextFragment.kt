package org.dergigi.boris.data

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Builds [scroll-to-text](https://wicg.github.io/scroll-to-text-fragment/)
 * URLs (`#:~:text=`) for sharing a highlight on a regular web article.
 */
object TextFragment {
    private const val MAX_EXACT = 300
    private const val RANGE_WORDS = 6

    fun apply(url: String, text: String): String {
        val value = fragmentValue(text) ?: return url
        val hashAt = url.indexOf('#')
        val base = if (hashAt >= 0) url.substring(0, hashAt) else url
        val fragment = if (hashAt >= 0) url.substring(hashAt + 1) else ""
        val beforeDirective = if (fragment.contains(":~:")) {
            fragment.substringBefore(":~:")
        } else {
            fragment
        }
        val hash = buildString {
            if (beforeDirective.isNotEmpty()) append(beforeDirective)
            append(":~:text=")
            append(value)
        }
        return "$base#$hash"
    }

    internal fun fragmentValue(text: String): String? {
        val normalized = text.trim().replace(WHITESPACE, " ")
        if (normalized.isEmpty()) return null
        if (normalized.length <= MAX_EXACT) return encodeToken(normalized)
        val words = normalized.split(' ')
        if (words.size <= RANGE_WORDS * 2) return encodeToken(normalized.take(MAX_EXACT))
        val start = words.take(RANGE_WORDS).joinToString(" ")
        val end = words.takeLast(RANGE_WORDS).joinToString(" ")
        return "${encodeToken(start)},${encodeToken(end)}"
    }

    private fun encodeToken(text: String): String =
        URLEncoder.encode(text, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("-", "%2D")

    private val WHITESPACE = Regex("\\s+")
}
