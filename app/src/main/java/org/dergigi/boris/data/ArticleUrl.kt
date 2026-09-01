package org.dergigi.boris.data

object ArticleUrl {
    fun host(url: String): String? {
        when (val target = NostrLink.parse(url)) {
            is NostrTarget.Article -> return target.ref.pointer.identifier.ifBlank { null }
            is NostrTarget.Note -> return "nostr"
            is NostrTarget.Profile -> return null
            null -> Unit
        }
        return try {
            val raw = if (url.contains("://")) url.trim() else "https://${url.trim()}"
            java.net.URI(raw).host?.lowercase()?.removePrefix("www.")?.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    /** Root website of a web article ("https://example.com"), null for nostr-native content. */
    fun root(url: String): String? {
        if (NostrLink.parse(url) != null) return null
        return try {
            val raw = if (url.contains("://")) url.trim() else "https://${url.trim()}"
            java.net.URI(raw).host
                ?.lowercase()
                ?.removePrefix("www.")
                ?.ifEmpty { null }
                ?.let { "https://$it" }
        } catch (_: Exception) {
            null
        }
    }

    fun normalize(url: String): String {
        val trimmed = url.trim()
        val noFragment = trimmed.substringBefore('#')
        val raw = if (noFragment.contains("://")) noFragment else "https://$noFragment"
        val (host, path) = try {
            val parsed = java.net.URI(raw)
            (parsed.host ?: "") to parsed.path.orEmpty()
        } catch (_: Exception) {
            // URI rejects spaces and stray characters (text fragments, #132);
            // fall back to a by-hand split so such URLs still collapse.
            val rest = raw.substringAfter("://").substringBefore('?')
            val slash = rest.indexOf('/')
            if (slash < 0) rest to "" else rest.substring(0, slash) to rest.substring(slash)
        }
        val cleanHost = host.lowercase().removePrefix("www.")
        return if (cleanHost.isEmpty()) trimmed else "https://$cleanHost${path.trimEnd('/')}"
    }
}
