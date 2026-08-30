package org.dergigi.boris.data

data class IncomingShare(
    val url: String? = null,
    val highlightQuote: String? = null,
)

data class IncomingSave(
    val url: String,
    val title: String? = null,
)

object IncomingShares {
    fun isSaveShare(componentClassName: String?): Boolean {
        val name = componentClassName?.substringAfterLast('.').orEmpty()
        return name == "ShareSaveAlias"
    }

    fun fromProcessText(text: String?, originatingUrl: String? = null): IncomingShare {
        val quote = text?.trim().orEmpty()
        if (quote.isEmpty()) return IncomingShare()
        val asUrl = UrlExtractor.extract(quote)
        if (asUrl != null && isUrlOnly(quote, asUrl)) {
            return IncomingShare(url = asUrl)
        }
        return IncomingShare(
            highlightQuote = quote,
            url = pageUrl(originatingUrl),
        )
    }

    internal fun isUrlOnly(text: String, extracted: String): Boolean {
        val trimmed = text.trim().trimEnd('.', ',', ';', ')', ']')
        return trimmed.equals(extracted, ignoreCase = true) ||
            UrlExtractor.normalize(trimmed).equals(extracted, ignoreCase = true)
    }

    internal fun pageUrl(originatingUrl: String?): String? {
        val url = UrlExtractor.extract(originatingUrl) ?: return null
        if (url.startsWith("android-app:", ignoreCase = true)) return null
        return url
    }

    internal fun firstPageUrl(candidates: Iterable<String?>): String? =
        candidates.firstNotNullOfOrNull(::pageUrl)
}
