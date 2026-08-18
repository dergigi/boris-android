package org.dergigi.boris.data

object RssDiscovery {
    fun rootFeedUrl(articleUrl: String): String? =
        feedCandidates(articleUrl).firstOrNull()

    fun feedCandidates(articleUrl: String): List<String> {
        if (NostrLink.parse(articleUrl) != null) return emptyList()
        return try {
            val raw = if (articleUrl.contains("://")) articleUrl.trim() else "https://${articleUrl.trim()}"
            val uri = java.net.URI(raw)
            val scheme = uri.scheme?.lowercase()?.takeIf { it == "http" || it == "https" } ?: "https"
            val host = uri.host?.lowercase()?.ifEmpty { null } ?: return emptyList()
            val origin = origin(scheme, host, uri.port)
            buildList {
                add("$origin/feed.xml")
                val firstPath = uri.path.orEmpty()
                    .split('/')
                    .firstOrNull { it.isNotBlank() }
                    ?.takeIf { it != "feed.xml" }
                if (firstPath != null) add("$origin/$firstPath/feed.xml")
            }.distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun origin(scheme: String, host: String, port: Int): String {
        val defaultPort = if (scheme == "http") 80 else 443
        return if (port > 0 && port != defaultPort) "$scheme://$host:$port" else "$scheme://$host"
    }
}
