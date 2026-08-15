package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip84

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
            out.add(
                HighlightedArticle(
                    url = url,
                    host = host,
                    title = host,
                    imageUrl = null,
                    highlightedAt = event.createdAt,
                ),
            )
            if (out.size >= limit) break
        }
        return out
    }
}
