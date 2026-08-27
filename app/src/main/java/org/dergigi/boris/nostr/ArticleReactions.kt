package org.dergigi.boris.nostr

import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.ReadableContent

enum class ArticleReaction(val emoji: String) {
    Slop("🤖"),
    Love("❤️"),
    Good("👍"),
    ;

    companion object {
        fun fromContent(content: String): ArticleReaction? {
            val emoji = content.trim()
            return entries.firstOrNull { it.emoji == emoji }
        }
    }
}

object ArticleReactions {
    fun kind(content: ReadableContent): Int? =
        if (tags(content) == null) null else Nip01Event.KIND_REACTION

    fun tags(content: ReadableContent): List<List<String>>? {
        val coordinate = content.articleCoordinate?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val article = NostrArticle.fromCoordinate(coordinate) ?: return null
        if (article.pointer.kind != Nip01Event.KIND_LONG_FORM) return null
        val author = content.authorPubkey
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.length == 64 }
            ?: article.pointer.pubkey.lowercase()
        return buildList {
            add(listOf("a", coordinate))
            add(listOf("p", author))
            add(listOf("k", Nip01Event.KIND_LONG_FORM.toString()))
            content.eventId
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.length == 64 }
                ?.let { add(listOf("e", it)) }
        }
    }

    fun unsignedJson(
        reaction: ArticleReaction,
        content: ReadableContent,
        pubkeyHex: String? = null,
        createdAt: Long = System.currentTimeMillis() / 1000,
    ): String? {
        val tags = tags(content) ?: return null
        return Nip01Event.unsignedJson(
            kind = Nip01Event.KIND_REACTION,
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
        if (event.kind != Nip01Event.KIND_REACTION) return false
        if (ArticleReaction.fromContent(event.content) == null) return false
        val coordinate = content.articleCoordinate?.trim()?.takeIf { it.isNotEmpty() }
        if (coordinate != null && event.tags.any {
            it.size >= 2 && it[0] == "a" && it[1] == coordinate
        }) {
            return true
        }
        val eventId = content.eventId?.trim()?.lowercase()?.takeIf { it.length == 64 }
        return eventId != null && event.tags.any {
            it.size >= 2 && it[0] == "e" && it[1].equals(eventId, ignoreCase = true)
        }
    }
}
