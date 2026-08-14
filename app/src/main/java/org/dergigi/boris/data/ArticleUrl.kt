package org.dergigi.boris.data

object ArticleUrl {
    fun normalize(url: String): String {
        val trimmed = url.trim()
        return try {
            val raw = if (trimmed.contains("://")) trimmed else "https://$trimmed"
            val parsed = java.net.URI(raw)
            val host = (parsed.host ?: "").lowercase().removePrefix("www.")
            val path = parsed.path.orEmpty().trimEnd('/')
            if (host.isEmpty()) trimmed else "https://$host$path"
        } catch (_: Exception) {
            trimmed
        }
    }
}
