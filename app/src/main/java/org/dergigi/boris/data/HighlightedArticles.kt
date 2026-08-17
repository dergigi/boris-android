package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.Profile
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
                is NostrTarget.Article -> authorHost(target.ref.pointer.pubkey)
                is NostrTarget.Note -> "nostr"
                null -> ArticleUrl.host(url) ?: continue
            }
            out.add(decorate(HighlightedArticle(url, host, host, null, event.createdAt)))
            if (out.size >= limit) break
        }
        return out
    }

    /** Articles ranked by highlight count in the last week; needs at least two to rank. */
    fun mostHighlighted(
        events: List<Nip01Event>,
        limit: Int,
        since: Long = System.currentTimeMillis() / 1000 - WEEK_SECONDS,
    ): List<HighlightedArticle> {
        val counts = LinkedHashMap<String, MutableList<Nip01Event>>()
        val seen = HashSet<String>()
        for (event in events) {
            if (event.createdAt < since) continue
            if (!seen.add(event.id)) continue
            val raw = Nip84.articleUrl(event) ?: continue
            val target = NostrLink.parse(raw)
            val url = target?.uri ?: ArticleUrl.normalize(raw)
            if (target == null && !url.startsWith("http")) continue
            counts.getOrPut(url) { mutableListOf() }.add(event)
        }
        return counts.entries
            .filter { it.value.size >= 2 }
            .sortedWith(
                compareByDescending<Map.Entry<String, List<Nip01Event>>> { it.value.size }
                    .thenByDescending { entry -> entry.value.maxOf { it.createdAt } },
            )
            .take(limit)
            .mapNotNull { (url, hits) ->
                val target = NostrLink.parse(url)
                val host = when (target) {
                    is NostrTarget.Article -> authorHost(target.ref.pointer.pubkey)
                    is NostrTarget.Note -> "nostr"
                    null -> ArticleUrl.host(url) ?: return@mapNotNull null
                }
                decorate(HighlightedArticle(url, host, host, null, hits.maxOf { it.createdAt }))
            }
    }

    private const val WEEK_SECONDS = 7L * 24 * 60 * 60

    /** Fetches missing article/note events and author profiles, then re-decorates. */
    fun hydrate(items: List<HighlightedArticle>): List<HighlightedArticle> {
        val notes = ArrayList<NostrTarget.Note>()
        val authorKeys = LinkedHashSet<String>()
        for (item in items) {
            when (val target = NostrLink.parse(item.url)) {
                is NostrTarget.Article -> {
                    val pointer = target.ref.pointer
                    authorKeys.add(pointer.pubkey)
                    if (EventCache.latest(pointer.kind, pointer.pubkey, pointer.identifier) == null) {
                        RelayQuery.fetchArticle(pointer)
                    }
                }
                is NostrTarget.Note -> notes.add(target)
                null -> Unit
            }
        }
        for (note in notes) {
            if (EventCache.event(note.eventId) == null) {
                runCatching { RelayQuery.fetchEvent(note.eventId, note.relays) }
            }
        }
        notes.mapNotNull { EventCache.event(it.eventId)?.pubkey }.forEach { authorKeys.add(it) }
        for (pubkey in authorKeys) {
            if (EventCache.latest(Nip01Event.KIND_METADATA, pubkey) == null) {
                runCatching { RelayQuery.fetchProfile(pubkey) }
            }
        }
        return items.map { decorate(it) }
    }

    fun decorate(
        article: HighlightedArticle,
        preview: OgPreview? = ArticlePreview.get(article.url),
    ): HighlightedArticle {
        return when (val target = NostrLink.parse(article.url)) {
            is NostrTarget.Article -> decorateArticle(article, target, preview)
            is NostrTarget.Note -> decorateNote(article, target, preview)
            null -> decorateWeb(article, preview)
        }
    }

    private fun decorateArticle(
        article: HighlightedArticle,
        target: NostrTarget.Article,
        preview: OgPreview?,
    ): HighlightedArticle {
        val event = EventCache.latest(
            target.ref.pointer.kind,
            target.ref.pointer.pubkey,
            target.ref.pointer.identifier,
        )
        val title = event?.let { Nip23.title(it) }
            ?: preview?.title?.takeIf { it.isNotBlank() }
            ?: article.title.takeUnless { it == article.host }
            ?: article.title
        val image = event?.let { Nip23.image(it) }
            ?: event?.content?.let { ArticleCover.firstMarkdownImage(it) }
            ?: preview?.imageUrl
            ?: article.imageUrl
        return article.copy(title = title, imageUrl = image, host = authorHost(target.ref.pointer.pubkey))
    }

    private fun decorateNote(
        article: HighlightedArticle,
        target: NostrTarget.Note,
        preview: OgPreview?,
    ): HighlightedArticle {
        val event = EventCache.event(target.eventId)
        val title = event?.let(NoteCover::title)
            ?: preview?.title?.takeIf { it.isNotBlank() }
            ?: article.title.takeUnless { it == article.host || it == "nostr" }
            ?: article.title
        val image = NoteCover.image(event)
            ?: preview?.imageUrl
            ?: article.imageUrl
        val host = event?.let { ev ->
            val profile = EventCache.latest(Nip01Event.KIND_METADATA, ev.pubkey)
                ?.let { Profile.parse(it.content) }
            Profile.displayName(ev.pubkey, profile)
        } ?: article.host
        return article.copy(title = title, imageUrl = image, host = host)
    }

    private fun decorateWeb(
        article: HighlightedArticle,
        preview: OgPreview?,
    ): HighlightedArticle {
        val title = preview?.title?.takeIf { it.isNotBlank() }
            ?: article.title.takeUnless { it == article.host }
            ?: article.title
        val image = preview?.imageUrl ?: article.imageUrl
        val host = preview?.siteName?.takeIf { it.isNotBlank() } ?: article.host
        return article.copy(title = title, imageUrl = image, host = host)
    }

    /** Author name for nostr-native cards; short npub when metadata is missing. */
    private fun authorHost(pubkeyHex: String): String {
        val profile = EventCache.latest(Nip01Event.KIND_METADATA, pubkeyHex)
            ?.let { Profile.parse(it.content) }
        return Profile.displayName(pubkeyHex, profile)
    }
}
