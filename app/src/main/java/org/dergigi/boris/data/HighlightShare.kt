package org.dergigi.boris.data

object HighlightShare {
    fun url(articleUrl: String, quote: String): String {
        val target = NostrLink.parse(articleUrl)
        if (target != null) return target.publicUrl
        if (!isHttp(articleUrl)) return articleUrl
        return TextFragment.apply(articleUrl, MarkdownInline.plain(quote))
    }

    fun articleUrl(articleUrl: String, quote: String): String? {
        val target = NostrLink.parse(articleUrl) ?: return null
        if (target is NostrTarget.Profile) return null
        return TextFragment.apply(target.publicUrl, MarkdownInline.plain(quote))
    }

    private fun isHttp(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.startsWith("https://") || lower.startsWith("http://")
    }
}
