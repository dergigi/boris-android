package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.NipB0
import org.dergigi.boris.nostr.Profile

/** Local-only search over EventCache. Relay NIP-50 search is backlog. */
object LocalSearch {
    const val DEFAULT_LIMIT = 40

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
            val eventId: String,
            val url: String?,
            val quote: String,
            val context: String?,
            val comment: String? = null,
            val host: String?,
            val authorHex: String,
            val authorName: String,
            val authorPicture: String?,
            val mine: Boolean,
            val friend: Boolean,
            val foaf: Boolean = false,
        ) : Hit()

        data class Article(
            override val id: String,
            override val title: String,
            override val subtitle: String?,
            override val sortAt: Long,
            val url: String,
            val imageUrl: String?,
            val authorName: String,
            val authorPicture: String?,
        ) : Hit()

        data class Bookmark(
            override val id: String,
            override val title: String,
            override val subtitle: String?,
            override val sortAt: Long,
            val url: String,
            val imageUrl: String? = null,
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

    fun query(
        raw: String,
        limit: Int = DEFAULT_LIMIT,
        sessionHex: String? = null,
        friendPubkeys: Set<String> = emptySet(),
        foafPubkeys: Set<String> = emptySet(),
    ): List<Hit> {
        val needle = normalize(raw)
        if (needle.length < 2) return emptyList()
        val session = sessionHex?.lowercase()
        val friends = friendPubkeys.map { it.lowercase() }.toSet()
        val foaf = foafPubkeys.map { it.lowercase() }.toSet()
        val hits = mutableListOf<Hit>()
        hits += searchHighlights(needle, session, friends, foaf)
        hits += searchArticles(needle)
        hits += searchBookmarks(needle)
        hits += searchPeople(needle)
        return hits
            .sortedByDescending { it.sortAt }
            .distinctBy { it.id }
            .take(limit)
    }

    private fun searchHighlights(
        needle: String,
        sessionHex: String?,
        friendPubkeys: Set<String>,
        foafPubkeys: Set<String>,
    ): List<Hit> =
        EventCache.byKind(Nip01Event.KIND_HIGHLIGHT).mapNotNull { event ->
            val quote = event.content.trim()
            if (quote.isEmpty()) return@mapNotNull null
            val context = event.tagValue("context")
            val comment = Nip84.comment(event)
            if (!matches(needle, quote, context, comment)) return@mapNotNull null
            val url = Nip84.articleUrl(event)
            val host = url?.let { ArticleUrl.host(it) }
            val profile = cachedProfile(event.pubkey)
            val author = event.pubkey.lowercase()
            val mine = sessionHex != null && author == sessionHex
            Hit.Highlight(
                id = "hl:${event.id}",
                title = quote,
                subtitle = host,
                sortAt = event.createdAt,
                eventId = event.id,
                url = url,
                quote = quote,
                context = context,
                comment = comment,
                host = host,
                authorHex = author,
                authorName = Profile.displayName(event.pubkey, profile),
                authorPicture = profile?.picture,
                mine = mine,
                friend = !mine && author in friendPubkeys,
                foaf = !mine && author !in friendPubkeys && author in foafPubkeys,
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
                val profile = cachedProfile(event.pubkey)
                Hit.Article(
                    id = "art:${event.pubkey.lowercase()}:$identifier",
                    title = title,
                    subtitle = summary?.take(120),
                    sortAt = Nip23.publishedAt(event),
                    url = article.uri,
                    imageUrl = Nip23.image(event),
                    authorName = Profile.displayName(event.pubkey, profile),
                    authorPicture = profile?.picture,
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

    private fun cachedProfile(pubkeyHex: String): Profile? {
        val event = EventCache.latest(Nip01Event.KIND_METADATA, pubkeyHex) ?: return null
        return Profile.parse(event.content)
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
        ArticleUrl.host(url) ?: url

    private val Whitespace = Regex("\\s+")
}
