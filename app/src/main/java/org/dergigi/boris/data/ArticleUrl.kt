package org.dergigi.boris.data

object ArticleUrl {
    fun host(url: String): String? {
        NostrArticle.parse(url)?.pointer?.identifier?.ifBlank { null }?.let { return it }
        return try {
            val raw = if (url.contains("://")) url.trim() else "https://${url.trim()}"
            java.net.URI(raw).host?.lowercase()?.removePrefix("www.")?.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

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
