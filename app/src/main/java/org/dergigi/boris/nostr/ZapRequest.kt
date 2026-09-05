package org.dergigi.boris.nostr

import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.ReadableContent

/** NIP-57 kind 9734 zap request tags for whatever the reader is showing. */
object ZapRequest {
    fun tags(
        content: ReadableContent,
        recipientPubkey: String,
        amountMsats: Long,
        relays: List<String>,
    ): List<List<String>> = buildList {
        add(listOf("relays") + relays)
        add(listOf("amount", amountMsats.toString()))
        add(listOf("p", recipientPubkey.lowercase()))
        val eventId = content.eventId?.trim()?.lowercase()?.takeIf { eventIdRegex.matches(it) }
        val article = content.articleCoordinate?.trim()?.takeIf { it.isNotEmpty() }?.let(NostrArticle::fromCoordinate)
        when {
            article != null -> {
                add(listOf("a", article.coordinate))
                eventId?.let { add(listOf("e", it)) }
                add(listOf("k", article.pointer.kind.toString()))
            }
            eventId != null -> {
                add(listOf("e", eventId))
                add(listOf("k", Nip01Event.KIND_TEXT_NOTE.toString()))
            }
        }
    }

    private val eventIdRegex = Regex("[0-9a-f]{64}")
}
