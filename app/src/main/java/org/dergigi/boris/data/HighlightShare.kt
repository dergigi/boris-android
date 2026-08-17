package org.dergigi.boris.data

/** Share target for a highlight card: web URL + text fragment, or a nostr public URL. */
object HighlightShare {
    fun url(articleUrl: String, quote: String): String {
        val target = NostrLink.parse(articleUrl)
        if (target != null) return target.publicUrl
        if (!isHttp(articleUrl)) return articleUrl
        return TextFragment.apply(articleUrl, MarkdownInline.plain(quote))
    }

    private fun isHttp(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.startsWith("https://") || lower.startsWith("http://")
    }
}
