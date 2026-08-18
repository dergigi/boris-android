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
        val protectedRanges = protectedRanges(markdown)
        val out = StringBuilder(markdown.length)
        var last = 0
        var changed = false
        for (match in entityRegex.findAll(markdown)) {
            if (isProtected(match.range, protectedRanges) ||
                !hasPlainTextBoundary(markdown, match.range)
            ) {
                continue
            }
            val profile = decode(match.groupValues[1].lowercase()) ?: continue
            out.append(markdown, last, match.range.first)
            out.append("[${label(profile)}](${profile.uri})")
            last = match.range.last + 1
            changed = true
        }
        if (!changed) return markdown
        out.append(markdown, last, markdown.length)
        return out.toString()
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
        entityRegex.matchEntire(raw)?.groupValues?.getOrNull(1)?.lowercase()

    private fun label(profile: NostrProfileRef): String {
        val npub = profile.npub
        return "@${npub.take(12)}...${npub.takeLast(6)}"
    }

    private fun protectedRanges(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        for (regex in protectedRegexes) {
            for (match in regex.findAll(text)) ranges.add(match.range)
        }
        if (ranges.isEmpty()) return emptyList()
        return ranges.sortedBy { it.first }.fold(mutableListOf()) { merged, range ->
            val previous = merged.lastOrNull()
            if (previous != null && range.first <= previous.last + 1) {
                merged[merged.lastIndex] = previous.first..maxOf(previous.last, range.last)
            } else {
                merged.add(range)
            }
            merged
        }
    }

    private fun isProtected(range: IntRange, protectedRanges: List<IntRange>): Boolean =
        protectedRanges.any { protected ->
            range.first >= protected.first && range.last <= protected.last
        }

    private fun hasPlainTextBoundary(text: String, range: IntRange): Boolean {
        val before = text.getOrNull(range.first - 1)
        val after = text.getOrNull(range.last + 1)
        return before?.isIdentifierAdjacent() != true && after?.isIdentifierAdjacent() != true
    }

    private fun Char.isIdentifierAdjacent(): Boolean =
        isLetterOrDigit() || this in "._-/:@?=&%#"

    private val protectedRegexes = listOf(
        Regex("""(?s)(^|\n)(?:```|~~~)[^\n]*\n.*?(?:\n(?:```|~~~)[ \t]*(?=\n|$)|$)"""),
        Regex("""`+[^`\n]+`+"""),
        Regex("""!?\[[^\]\n]*]\([^)\n]*\)"""),
        Regex("""!?\[[^\]\n]*]\[[^\]\n]*]"""),
        Regex("""(?m)^[ \t]{0,3}\[[^\]\n]+]:[ \t]*\S.*$"""),
        Regex("""<[A-Za-z][^>\n]*>[^<\n]*</[A-Za-z][^>\n]*>"""),
        Regex("""<[^>\n]+>"""),
    )

    private val entityRegex = Regex(
        """(?:nostr:(?://)?)?(nprofile1[023456789acdefghjklmnpqrstuvwxyz]+|npub1[023456789acdefghjklmnpqrstuvwxyz]+)""",
        RegexOption.IGNORE_CASE,
    )
}
