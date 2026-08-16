package org.dergigi.boris.data

/**
 * Title and cover for Home / Library cards. The reader writes this when an
 * article loads so going back does not wait on a live OG fetch or a relay.
 *
 * Keys include the opened URL, the normalized http URL, and every nostr
 * alias (naddr, coordinate, njump) so highlight cards and the reader agree.
 */
object ArticlePreview {
    fun remember(content: ReadableContent) {
        val preview = OgPreview(
            title = content.title?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = content.imageUrl?.trim()?.takeIf { it.isNotEmpty() },
            siteName = ArticleUrl.host(content.url),
            description = content.summary,
        )
        keysFor(content.url, content.articleCoordinate).forEach { key ->
            OgPreviewCache.put(key, preview)
        }
    }

    fun get(url: String): OgPreview? =
        keysFor(url).firstNotNullOfOrNull { OgPreviewCache.get(it) }

    internal fun keysFor(url: String, coordinate: String? = null): List<String> {
        val keys = LinkedHashSet<String>()
        if (url.isNotBlank()) keys.add(url)
        val normalized = ArticleUrl.normalize(url)
        if (normalized.isNotBlank()) keys.add(normalized)
        when (val target = NostrLink.parse(url)) {
            is NostrTarget.Article -> {
                keys.add(target.ref.uri)
                keys.add(target.ref.coordinate)
                keys.add(target.ref.publicUrl)
            }
            is NostrTarget.Note -> keys.add(target.uri)
            null -> Unit
        }
        if (!coordinate.isNullOrBlank()) {
            keys.add(coordinate)
            NostrArticle.fromCoordinate(coordinate)?.let { article ->
                keys.add(article.uri)
                keys.add(article.coordinate)
                keys.add(article.publicUrl)
            }
        }
        return keys.toList()
    }
}
