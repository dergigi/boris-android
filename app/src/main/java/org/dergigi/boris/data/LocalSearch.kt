package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.NipB0
import org.dergigi.boris.nostr.Profile

/** Local-only search over EventCache. Relay NIP-50 search is backlog. */
object LocalSearch {
    private const val MAX_RESULTS = 40

    sealed class Hit {
        abstract val id: String
        abstract val title: String
        abstract val subtitle: String?
        abstract val sortAt: Long

        data class Highlight(
            override val id: String,
            override val title: String,
            override val subtitle: String?,
            override val sortAt: Long,
            val url: String?,
            val quote: String,
        ) : Hit()

        data class Article(
            override val id: String,
            override val title: String,
            override val subtitle: String?,
            override val sortAt: Long,
            val url: String,
        ) : Hit()

        data class Bookmark(
            override val id: String,
            override val title: String,
            override val subtitle: String?,
            override val sortAt: Long,
            val url: String,
        ) : Hit()

        data class Person(
            override val id: String,
            override val title: String,
            override val subtitle: String?,
            override val sortAt: Long,
            val pubkeyHex: String,
            val pictureUrl: String?,
        ) : Hit()
    }

    fun query(raw: String, limit: Int = MAX_RESULTS): List<Hit> {
        val needle = normalize(raw)
        if (needle.length < 2) return emptyList()
        val hits = mutableListOf<Hit>()
        hits += searchHighlights(needle)
        hits += searchArticles(needle)
        hits += searchBookmarks(needle)
        hits += searchPeople(needle)
        return hits
            .sortedByDescending { it.sortAt }
            .distinctBy { it.id }
            .take(limit)
    }

    private fun searchHighlights(needle: String): List<Hit> =
        EventCache.byKind(Nip01Event.KIND_HIGHLIGHT).mapNotNull { event ->
            val quote = event.content.trim()
            if (quote.isEmpty()) return@mapNotNull null
            val context = event.tagValue("context")
            if (!matches(needle, quote, context)) return@mapNotNull null
            val url = Nip84.articleUrl(event)
            Hit.Highlight(
                id = "hl:${event.id}",
                title = quote,
                subtitle = url?.let { hostish(it) },
                sortAt = event.createdAt,
                url = url,
                quote = quote,
            )
        }

    private fun searchArticles(needle: String): List<Hit> =
        EventCache.byKind(Nip01Event.KIND_LONG_FORM)
            .groupBy { "${it.pubkey.lowercase()}:${Nip23.identifier(it).orEmpty().lowercase()}" }
            .mapNotNull { (_, events) -> events.maxByOrNull { it.createdAt } }
            .mapNotNull { event ->
                val identifier = Nip23.identifier(event) ?: return@mapNotNull null
                val title = Nip23.title(event) ?: identifier
                val summary = Nip23.summary(event)
                if (!matches(needle, title, summary, event.content.take(2_000))) return@mapNotNull null
                val article = NostrArticle.fromCoordinate(
                    "${event.kind}:${event.pubkey.lowercase()}:$identifier",
                ) ?: return@mapNotNull null
                Hit.Article(
                    id = "art:${event.pubkey.lowercase()}:$identifier",
                    title = title,
                    subtitle = summary?.take(120),
                    sortAt = Nip23.publishedAt(event),
                    url = article.uri,
                )
            }

    private fun searchBookmarks(needle: String): List<Hit> =
        EventCache.byKind(Nip01Event.KIND_WEB_BOOKMARK).mapNotNull { event ->
            val url = NipB0.url(event) ?: return@mapNotNull null
            val title = NipB0.title(event) ?: url
            if (!matches(needle, title, url)) return@mapNotNull null
            Hit.Bookmark(
                id = "bm:${event.id}",
                title = title,
                subtitle = hostish(url),
                sortAt = NipB0.publishedAt(event),
                url = url,
            )
        }

    private fun searchPeople(needle: String): List<Hit> =
        EventCache.byKind(Nip01Event.KIND_METADATA)
            .groupBy { it.pubkey.lowercase() }
            .mapNotNull { (_, events) -> events.maxByOrNull { it.createdAt } }
            .mapNotNull { event ->
                val profile = Profile.parse(event.content)
                val pubkey = event.pubkey.lowercase()
                val pubkeyHit = needle.length >= 8 && pubkey.contains(needle)
                if (!pubkeyHit && !matches(needle, profile.name, profile.about)) {
                    return@mapNotNull null
                }
                Hit.Person(
                    id = "p:$pubkey",
                    title = Profile.displayName(event.pubkey, profile),
                    subtitle = profile.about?.take(120),
                    sortAt = event.createdAt,
                    pubkeyHex = pubkey,
                    pictureUrl = profile.picture,
                )
            }

    internal fun normalize(raw: String): String =
        raw.trim().lowercase().replace(Whitespace, " ")

    internal fun matches(needle: String, vararg haystacks: String?): Boolean {
        if (needle.isEmpty()) return false
        return haystacks.any { hay ->
            !hay.isNullOrBlank() && normalize(hay).contains(needle)
        }
    }

    private fun hostish(url: String): String =
        runCatching { java.net.URI(url).host }.getOrNull()?.removePrefix("www.") ?: url

    private val Whitespace = Regex("\\s+")
}
