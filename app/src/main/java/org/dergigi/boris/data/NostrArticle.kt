package org.dergigi.boris.data

import org.dergigi.boris.nostr.NaddrPointer
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19

data class NostrArticleRef(
    val naddr: String,
    val pointer: NaddrPointer,
) {
    val uri: String get() = "nostr:$naddr"
    val publicUrl: String get() = NostrLink.gatewayUrl(naddr)
    val coordinate: String get() = pointer.coordinate
}

object NostrArticle {
    const val KIND = Nip01Event.KIND_LONG_FORM

    fun parse(raw: String?): NostrArticleRef? {
        if (raw.isNullOrBlank()) return null
        val match = naddrRegex.find(raw.trim()) ?: return null
        val encoded = match.groupValues[1].lowercase()
        return try {
            val pointer = Nip19.naddrDecode(encoded)
            if (pointer.kind != KIND) null else NostrArticleRef(encoded, pointer)
        } catch (_: Exception) {
            null
        }
    }

    fun fromCoordinate(coordinate: String, relays: List<String> = emptyList()): NostrArticleRef? {
        val parts = coordinate.split(":", limit = 3)
        if (parts.size != 3) return null
        val kind = parts[0].toIntOrNull() ?: return null
        if (kind != KIND) return null
        val pubkey = parts[1].lowercase()
        if (!pubkeyRegex.matches(pubkey)) return null
        val identifier = parts[2]
        if (identifier.isEmpty()) return null
        val pointer = NaddrPointer(identifier, pubkey, kind, relays)
        return NostrArticleRef(Nip19.naddrEncode(pointer), pointer)
    }

    private val naddrRegex = Regex(
        """(?:nostr:)?(naddr1[023456789acdefghjklmnpqrstuvwxyz]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val pubkeyRegex = Regex("[0-9a-f]{64}")
}
