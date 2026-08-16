package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.RelayQuery

data class HighlightedArticle(
    val url: String,
    val host: String,
    val title: String,
    val imageUrl: String?,
    val highlightedAt: Long,
)

object HighlightedArticles {
    fun fromEvents(events: List<Nip01Event>, limit: Int): List<HighlightedArticle> {
        val seen = LinkedHashSet<String>()
        val out = ArrayList<HighlightedArticle>(limit)
        for (event in events.sortedByDescending { it.createdAt }) {
            val raw = Nip84.articleUrl(event) ?: continue
            val target = NostrLink.parse(raw)
            val url = target?.uri ?: ArticleUrl.normalize(raw)
            if (target == null && !url.startsWith("http")) continue
            if (!seen.add(url)) continue
            val host = when (target) {
                is NostrTarget.Article -> target.ref.pointer.identifier.ifBlank { "nostr" }
                is NostrTarget.Note -> "nostr"
                null -> ArticleUrl.host(url) ?: continue
            }
            out.add(decorate(HighlightedArticle(url, host, host, null, event.createdAt)))
            if (out.size >= limit) break
        }
        return out
    }

    /** Fetches kind-30023 events that are not in the cache, then re-applies titles and covers. */
    fun hydrate(items: List<HighlightedArticle>): List<HighlightedArticle> {
        for (item in items) {
            val article = NostrLink.parse(item.url) as? NostrTarget.Article ?: continue
            val pointer = article.ref.pointer
            if (EventCache.latest(pointer.kind, pointer.pubkey, pointer.identifier) == null) {
                RelayQuery.fetchArticle(pointer)
            }
        }
        return items.map { decorate(it) }
    }

    fun decorate(
        article: HighlightedArticle,
        preview: OgPreview? = ArticlePreview.get(article.url),
    ): HighlightedArticle {
        val target = NostrLink.parse(article.url)
        val event = (target as? NostrTarget.Article)?.ref?.pointer?.let { pointer ->
            EventCache.latest(pointer.kind, pointer.pubkey, pointer.identifier)
        }
        val title = event?.let { Nip23.title(it) }
            ?: preview?.title?.takeIf { it.isNotBlank() }
            ?: article.title.takeUnless { it == article.host }
            ?: article.title
        val image = event?.let { Nip23.image(it) }
            ?: event?.content?.let { ArticleCover.firstMarkdownImage(it) }
            ?: preview?.imageUrl
            ?: article.imageUrl
        val host = preview?.siteName?.takeIf { it.isNotBlank() } ?: article.host
        return article.copy(title = title, imageUrl = image, host = host)
    }
}
