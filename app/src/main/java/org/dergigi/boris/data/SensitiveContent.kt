package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23

/**
 * NSFW / sensitive-content classification (issue #95).
 * Explicit Nostr signals win; keyword matching on title then summary is a
 * fallback treated as "might be NSFW".
 */
object SensitiveContent {
    data class Warning(
        val reason: String?,
        val confirmed: Boolean,
    )

    fun classify(content: ReadableContent): Warning? {
        fromTags(content.tags)?.let { return it }
        return fromText(content.title, content.summary)
    }

    fun classify(article: HighlightedArticle): Warning? {
        eventFor(article.url)?.let { event ->
            fromEvent(event)?.let { return it }
        }
        return fromText(article.title, null)
    }

    fun classify(url: String?, title: String?, summary: String?): Warning? {
        if (!url.isNullOrBlank()) {
            eventFor(url)?.let { event ->
                fromEvent(event)?.let { return it }
            }
        }
        return fromText(title, summary)
    }

    fun fromEvent(event: Nip01Event): Warning? =
        fromTags(event.tags) ?: fromText(Nip23.title(event), Nip23.summary(event))

    fun fromTags(tags: List<List<String>>): Warning? {
        if (tags.isEmpty()) return null
        tags.firstOrNull { it.isNotEmpty() && it[0] == "content-warning" }?.let { tag ->
            return Warning(reason = tag.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }, confirmed = true)
        }
        val labeled = tags.firstOrNull { tag ->
            tag.size >= 3 && tag[0] == "l" && tag[2].equals("content-warning", ignoreCase = true)
        }
        if (labeled != null) {
            return Warning(reason = labeled[1].trim().takeIf { it.isNotEmpty() }, confirmed = true)
        }
        if (tags.any { it.size >= 2 && it[0] == "L" && it[1].equals("content-warning", ignoreCase = true) }) {
            return Warning(reason = null, confirmed = true)
        }
        val hash = tags.firstOrNull { tag ->
            tag.size >= 2 && tag[0] == "t" && SensitiveKeywords.isHashtag(tag[1])
        }
        if (hash != null) {
            return Warning(reason = hash[1].trim().lowercase(), confirmed = true)
        }
        return null
    }

    fun fromText(title: String?, summary: String?): Warning? {
        SensitiveKeywords.match(title)?.let { return Warning(reason = it, confirmed = false) }
        SensitiveKeywords.match(summary)?.let { return Warning(reason = it, confirmed = false) }
        return null
    }

    private fun eventFor(url: String): Nip01Event? {
        return when (val target = NostrLink.parse(url)) {
            is NostrTarget.Article -> EventCache.latest(
                target.ref.pointer.kind,
                target.ref.pointer.pubkey,
                target.ref.pointer.identifier,
            )
            is NostrTarget.Note -> EventCache.event(target.eventId)
            is NostrTarget.Profile -> null
            null -> null
        }
    }
}
