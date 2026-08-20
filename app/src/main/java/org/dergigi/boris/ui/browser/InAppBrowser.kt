package org.dergigi.boris.ui.browser

import org.dergigi.boris.data.NostrLink
import java.net.URI

object InAppBrowser {
    fun targetUrl(raw: String): String? {
        val resolved = NostrLink.parse(raw)?.publicUrl ?: raw.trim()
        return resolved.takeIf { isHttp(it) }
    }

    fun isHttp(url: String): Boolean {
        val scheme = runCatching { URI(url).scheme }.getOrNull()?.lowercase()
        return scheme == "http" || scheme == "https"
    }
}
