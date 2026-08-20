package org.dergigi.boris.ui.browser

import org.dergigi.boris.data.NostrLink
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object InAppBrowser {
    fun targetUrl(raw: String): String? {
        val resolved = NostrLink.parse(raw)?.publicUrl ?: raw.trim()
        return resolved.takeIf { isHttp(it) }
    }

    fun waybackUrl(raw: String): String? {
        val page = pageToArchive(raw) ?: return null
        return "https://web.archive.org/web/$page"
    }

    fun archivePhUrl(raw: String): String? {
        val page = pageToArchive(raw) ?: return null
        val encoded = URLEncoder.encode(page, StandardCharsets.UTF_8.name())
        return "https://archive.ph/?run=1&url=$encoded"
    }

    fun isHttp(url: String): Boolean {
        val scheme = runCatching { URI(url).scheme }.getOrNull()?.lowercase()
        return scheme == "http" || scheme == "https"
    }

    private fun pageToArchive(raw: String): String? {
        if (NostrLink.parse(raw) != null) return null
        val page = targetUrl(raw) ?: return null
        return page.takeUnless { isArchiveHost(it) }
    }

    private fun isArchiveHost(url: String): Boolean {
        val host = runCatching { URI(url).host }.getOrNull()?.lowercase() ?: return false
        return host == "web.archive.org" ||
            host.endsWith(".archive.org") ||
            host == "archive.ph" ||
            host == "archive.is" ||
            host == "archive.today" ||
            host == "archive.vn" ||
            host == "archive.fo"
    }
}
