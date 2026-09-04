package org.dergigi.boris.nostr

import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.NostrArticleRef
import org.dergigi.boris.data.ReadableContent

enum class ArticleReaction(val emoji: String) {
    Love("🧡"),
    Good("👍"),
    Slop("🤖"),
    ;

    companion object {
        /** What a plain tap on the reaction button sends. */
        val DEFAULT = Love

        fun fromContent(content: String): ArticleReaction? {
            val emoji = content.trim()
            return entries.firstOrNull { it.emoji == emoji }
        }
    }
}

/**
 * Emoji reactions on whatever the reader is showing. Long-form articles get an address-first
 * kind 7; notes and web pages reuse the archive targeting (kind 7 with `e`, kind 17 with `r`).
 */
object ArticleReactions {
    fun kind(content: ReadableContent): Int? =
        if (longForm(content) != null) Nip01Event.KIND_REACTION else Archive.kind(content)

    fun tags(content: ReadableContent): List<List<String>>? {
        val article = longForm(content) ?: return Archive.tags(content)
        val author = article.pointer.pubkey.lowercase()
        return buildList {
            add(listOf("a", article.coordinate))
            add(listOf("p", author))
            add(listOf("k", Nip01Event.KIND_LONG_FORM.toString()))
            content.eventId
                ?.trim()
                ?.lowercase()
                ?.takeIf { eventIdRegex.matches(it) }
                ?.let { add(listOf("e", it)) }
        }
    }

    fun unsignedJson(
        reaction: ArticleReaction,
        content: ReadableContent,
        pubkeyHex: String? = null,
        createdAt: Long = System.currentTimeMillis() / 1000,
    ): String? {
        val kind = kind(content) ?: return null
        val tags = tags(content) ?: return null
        return Nip01Event.unsignedJson(
            kind = kind,
            content = reaction.emoji,
            tags = tags,
            pubkeyHex = pubkeyHex,
            createdAt = createdAt,
        )
    }

    fun currentReaction(
        events: List<Nip01Event>,
        content: ReadableContent,
        userPubkeyHex: String? = null,
    ): ArticleReaction? {
        return events
            .filter { event ->
                userPubkeyHex == null || event.pubkey.equals(userPubkeyHex, ignoreCase = true)
            }
            .filter { isReactionTo(it, content) }
            .maxByOrNull { it.createdAt }
            ?.content
            ?.let(ArticleReaction::fromContent)
    }

    fun isReactionTo(event: Nip01Event, content: ReadableContent): Boolean {
        if (event.kind != kind(content)) return false
        if (ArticleReaction.fromContent(event.content) == null) return false
        val article = longForm(content) ?: return Archive.matchesTarget(event, content)
        return event.tags.any {
            it.size >= 2 && it[0] == "a" && it[1] == article.coordinate
        }
    }

    private fun longForm(content: ReadableContent): NostrArticleRef? {
        val coordinate = content.articleCoordinate?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return NostrArticle.fromCoordinate(coordinate)?.takeIf { it.pointer.kind == Nip01Event.KIND_LONG_FORM }
    }

    private val eventIdRegex = Regex("[0-9a-f]{64}")
}
