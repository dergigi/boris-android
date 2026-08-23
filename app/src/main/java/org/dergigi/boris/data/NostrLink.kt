package org.dergigi.boris.data

import org.dergigi.boris.nostr.LocalRelays
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

    data class Profile(
        val pubkeyHex: String,
        val encoded: String,
        override val relays: List<String> = emptyList(),
    ) : NostrTarget() {
        override val uri get() = "nostr:$encoded"
        override val publicUrl get() = NostrLink.gatewayUrl(encoded)
    }
}

object NostrLink {
    const val GATEWAY = "https://njump.to"
    private const val MAX_IDENTIFIER_LENGTH = 5000

    fun gatewayUrl(identifier: String): String = "$GATEWAY/$identifier"

    fun copyText(url: String): String = parse(url)?.uri ?: url

    /** njump URL for readable Nostr content; null for web pages and profiles. */
    fun njumpCopyUrl(url: String): String? =
        when (val target = parse(url)) {
            is NostrTarget.Article, is NostrTarget.Note -> target.publicUrl
            else -> null
        }

    fun readableEventKind(kind: Int): Boolean =
        kind == Nip01Event.KIND_TEXT_NOTE ||
            kind == Nip01Event.KIND_LONG_FORM ||
            kind == Nip01Event.KIND_HIGHLIGHT ||
            kind == Nip01Event.KIND_COMMENT

    fun parse(raw: String?): NostrTarget? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        findEntity(trimmed)?.let { encoded -> return decode(encoded) }
        hexEventId(trimmed)?.let { id ->
            return NostrTarget.Note(eventId = id, encoded = Nip19.noteEncode(id))
        }
        profilePath(trimmed)?.let { encoded -> return decode(encoded) }
        return null
    }

    private fun decode(encoded: String): NostrTarget? {
        if (encoded.length > MAX_IDENTIFIER_LENGTH) return null
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
                    if (kind != null && !readableEventKind(kind)) {
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
                encoded.startsWith("nprofile1") -> {
                    val pointer = Nip19.nprofileDecode(encoded)
                    val relays = pointer.relays.mapNotNull { LocalRelays.resolve(it) }
                    NostrTarget.Profile(
                        pubkeyHex = pointer.pubkey,
                        encoded = encoded,
                        relays = relays,
                    )
                }
                encoded.startsWith("npub1") -> {
                    val pubkey = Nip19.npubDecode(encoded)
                    NostrTarget.Profile(pubkeyHex = pubkey, encoded = encoded)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findEntity(raw: String): String? {
        val match = entityRegex.find(raw) ?: return null
        val encoded = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
            ?: match.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }
            ?: return null
        return encoded.lowercase()
    }

    private fun hexEventId(raw: String): String? =
        eventPathRegex.find(raw)?.groupValues?.getOrNull(1)?.lowercase()

    private fun profilePath(raw: String): String? =
        profilePathRegex.find(raw)?.groupValues?.getOrNull(1)?.lowercase()

    private val bech32Body = "023456789acdefghjklmnpqrstuvwxyz"

    private val entityRegex = Regex(
        """(?:nostr:(?://)?)?(naddr1[$bech32Body]+|note1[$bech32Body]+|nevent1[$bech32Body]+)|(?:nostr:(?://)?)(nprofile1[$bech32Body]+|npub1[$bech32Body]+)""",
        RegexOption.IGNORE_CASE,
    )

    private val eventPathRegex = Regex(
        """(?:njump\.to|readwithboris\.com)/e/([0-9a-f]{64})""",
        RegexOption.IGNORE_CASE,
    )

    private val profilePathRegex = Regex(
        """(?:njump\.to|readwithboris\.com)/(npub1[$bech32Body]+|nprofile1[$bech32Body]+)""",
        RegexOption.IGNORE_CASE,
    )
}
