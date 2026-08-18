package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip19

data class NostrProfileRef(
    val pubkey: String,
    val encoded: String,
    val relays: List<String> = emptyList(),
) {
    val npub: String get() = Nip19.npubEncode(pubkey)
    val uri: String get() = "nostr:$encoded"
    val publicUrl: String get() = NostrLink.gatewayUrl(encoded)
}

object NostrProfile {
    fun parse(raw: String?): NostrProfileRef? {
        if (raw.isNullOrBlank()) return null
        val encoded = findEntity(raw.trim()) ?: return null
        return decode(encoded)
    }

    fun linkify(markdown: String): String {
        if (!markdown.contains("npub1", ignoreCase = true) &&
            !markdown.contains("nprofile1", ignoreCase = true)
        ) {
            return markdown
        }
        return entityRegex.replace(markdown) { match ->
            if (alreadyLinked(markdown, match.range.first)) return@replace match.value
            val profile = decode(match.groupValues[1].lowercase()) ?: return@replace match.value
            "[${label(profile)}](${profile.uri})"
        }
    }

    private fun decode(encoded: String): NostrProfileRef? = try {
        when {
            encoded.startsWith("npub1") -> {
                val pubkey = Nip19.npubDecode(encoded)
                NostrProfileRef(pubkey = pubkey, encoded = encoded)
            }
            encoded.startsWith("nprofile1") -> {
                val profile = Nip19.nprofileDecode(encoded)
                NostrProfileRef(
                    pubkey = profile.pubkey,
                    encoded = encoded,
                    relays = profile.relays,
                )
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    private fun findEntity(raw: String): String? =
        entityRegex.find(raw)?.groupValues?.getOrNull(1)?.lowercase()

    private fun label(profile: NostrProfileRef): String {
        val npub = profile.npub
        return "@${npub.take(12)}...${npub.takeLast(6)}"
    }

    private fun alreadyLinked(text: String, start: Int): Boolean {
        if (start == 0) return false
        val before = text[start - 1]
        return before == '(' || before == '<'
    }

    private val entityRegex = Regex(
        """(?:nostr:(?://)?)?(nprofile1[023456789acdefghjklmnpqrstuvwxyz]+|npub1[023456789acdefghjklmnpqrstuvwxyz]+)""",
        RegexOption.IGNORE_CASE,
    )
}
