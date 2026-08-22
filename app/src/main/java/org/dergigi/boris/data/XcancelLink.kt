package org.dergigi.boris.data

import java.net.URI

object XcancelLink {
    private val twitterHosts = setOf("x.com", "twitter.com", "mobile.twitter.com")

    /** Equivalent xcancel.com URL, or null when the page is not on X. */
    fun copyUrl(url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        if (host !in twitterHosts) return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        return URI(
            "https",
            uri.userInfo,
            "xcancel.com",
            -1,
            uri.rawPath.ifEmpty { "/" },
            uri.rawQuery,
            uri.rawFragment,
        ).toASCIIString()
    }
}
