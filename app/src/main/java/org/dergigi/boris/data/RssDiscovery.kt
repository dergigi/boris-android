package org.dergigi.boris.data

object RssDiscovery {
    fun rootFeedUrl(articleUrl: String): String? =
        feedCandidates(articleUrl).firstOrNull()

    fun feedCandidates(articleUrl: String): List<String> {
        if (NostrLink.parse(articleUrl) != null) return emptyList()
        return try {
            val raw = if (articleUrl.contains("://")) articleUrl.trim() else "https://${articleUrl.trim()}"
            val uri = java.net.URI(raw)
            val host = uri.host?.lowercase()?.removePrefix("www.")?.ifEmpty { null } ?: return emptyList()
            val origin = "https://$host"
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
}
