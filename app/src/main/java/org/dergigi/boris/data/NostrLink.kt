package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19

sealed class NostrTarget {
    abstract val uri: String
    abstract val publicUrl: String
    abstract val relays: List<String>

    data class Article(val ref: NostrArticleRef) : NostrTarget() {
        override val uri get() = ref.uri
        override val publicUrl get() = ref.publicUrl
        override val relays get() = ref.pointer.relays
    }

    data class Note(
        val eventId: String,
        val encoded: String,
        override val relays: List<String> = emptyList(),
        val author: String? = null,
        val kind: Int? = null,
    ) : NostrTarget() {
        override val uri get() = "nostr:$encoded"
        override val publicUrl get() = NostrLink.gatewayUrl(encoded)
    }
}

object NostrLink {
    const val GATEWAY = "https://njump.to"

    fun gatewayUrl(identifier: String): String = "$GATEWAY/$identifier"

    fun parse(raw: String?): NostrTarget? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        findEntity(trimmed)?.let { encoded -> return decode(encoded) }
        hexEventId(trimmed)?.let { id ->
            return NostrTarget.Note(eventId = id, encoded = Nip19.noteEncode(id))
        }
        return null
    }

    private fun decode(encoded: String): NostrTarget? {
        return try {
            when {
                encoded.startsWith("naddr1") -> {
                    val article = NostrArticle.parse(encoded) ?: return null
                    NostrTarget.Article(article)
                }
                encoded.startsWith("note1") -> {
                    val id = Nip19.noteDecode(encoded)
                    NostrTarget.Note(eventId = id, encoded = encoded)
                }
                encoded.startsWith("nevent1") -> {
                    val pointer = Nip19.neventDecode(encoded)
                    val kind = pointer.kind
                    if (kind != null &&
                        kind != Nip01Event.KIND_TEXT_NOTE &&
                        kind != Nip01Event.KIND_LONG_FORM
                    ) {
                        return null
                    }
                    NostrTarget.Note(
                        eventId = pointer.eventId,
                        encoded = encoded,
                        relays = pointer.relays,
                        author = pointer.author,
                        kind = kind,
                    )
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findEntity(raw: String): String? {
        entityRegex.find(raw)?.groupValues?.getOrNull(1)?.lowercase()?.let { return it }
        return null
    }

    private fun hexEventId(raw: String): String? =
        eventPathRegex.find(raw)?.groupValues?.getOrNull(1)?.lowercase()

    private val entityRegex = Regex(
        """(?:nostr:(?://)?)?(naddr1[023456789acdefghjklmnpqrstuvwxyz]+|note1[023456789acdefghjklmnpqrstuvwxyz]+|nevent1[023456789acdefghjklmnpqrstuvwxyz]+)""",
        RegexOption.IGNORE_CASE,
    )

    private val eventPathRegex = Regex(
        """(?:njump\.to|readwithboris\.com)/e/([0-9a-f]{64})""",
        RegexOption.IGNORE_CASE,
    )
}
