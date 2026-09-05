package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.HintedRelays
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.NprofilePointer
import org.dergigi.boris.nostr.Profile

object NostrMentions {
    private val FENCE = Regex("""(?s)(?:```|~~~)[^\n]*\n.*?(?:```|~~~)""")
    private val INLINE_CODE = Regex("""`+[^`]+`+""")
    private val bech32Body = "023456789acdefghjklmnpqrstuvwxyz"
    internal val PROFILE_MENTION = Regex(
        """(?<![/\w])(?:nostr:(?://)?)(nprofile1[$bech32Body]+|npub1[$bech32Body]+)""",
        RegexOption.IGNORE_CASE,
    )

    internal fun profilesIn(text: String): List<NprofilePointer> {
        if (text.isBlank()) return emptyList()
        val seen = linkedSetOf<String>()
        val out = mutableListOf<NprofilePointer>()
        for (match in PROFILE_MENTION.findAll(text)) {
            val pointer = decodeProfile(match.groupValues[1].lowercase()) ?: continue
            if (seen.add(pointer.pubkey.lowercase())) out += pointer
        }
        return out
    }

    fun rewrite(markdown: String): String {
        val (protected, restore) = protectCode(markdown)
        val linkUrls = markdownLinkUrlRanges(protected)
        val out = StringBuilder()
        var last = 0
        for (match in PROFILE_MENTION.findAll(protected)) {
            if (linkUrls.any { match.range.first in it || match.range.last in it }) continue
            var start = match.range.first
            var endExclusive = match.range.last + 1
            val wrapped = start > 0 &&
                endExclusive < protected.length &&
                protected[start - 1] == '<' &&
                protected[endExclusive] == '>'
            if (wrapped) {
                start -= 1
                endExclusive += 1
            }
            out.append(protected, last, start)
            out.append(replacementFor(match))
            last = endExclusive
        }
        out.append(protected, last, protected.length)
        return restore(out.toString())
    }

    private fun replacementFor(match: MatchResult): String {
        val encoded = match.groupValues[1].lowercase()
        val pointer = decodeProfile(encoded)
        if (pointer == null) {
            return match.value.take(20) + "…"
        }
        HintedRelays.remember(pointer.pubkey, pointer.relays)
        val cached = EventCache.latest(Nip01Event.KIND_METADATA, pointer.pubkey)
            ?.let { Profile.parse(it.content) }
        val label = "@" + Profile.displayName(pointer.pubkey, cached)
        return "[$label](nostr:$encoded)"
    }

    internal fun decodeProfile(encoded: String): NprofilePointer? {
        return try {
            when {
                encoded.startsWith("nprofile1") -> Nip19.nprofileDecode(encoded)
                encoded.startsWith("npub1") -> NprofilePointer(Nip19.npubDecode(encoded))
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    internal fun markdownLinkUrlRanges(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var i = 0
        while (i < text.length) {
            val urlStartMatch = text.indexOf("](", i)
            if (urlStartMatch == -1) break
            val urlStart = urlStartMatch + 2
            var pos = urlStart
            var depth = 1
            var urlEnd = -1
            while (pos < text.length && depth > 0) {
                val char = text[pos]
                if (char == '\\' && pos + 1 < text.length) {
                    pos += 2
                    continue
                }
                when (char) {
                    '(' -> depth++
                    ')' -> {
                        depth--
                        if (depth == 0) {
                            urlEnd = pos
                            break
                        }
                    }
                }
                pos++
            }
            if (urlEnd != -1) {
                ranges += urlStart until urlEnd
                i = urlEnd + 1
            } else {
                i = urlStart + 1
            }
        }
        return ranges
    }

    internal fun protectCode(text: String): Pair<String, (String) -> String> {
        val slots = mutableListOf<String>()
        fun stash(match: MatchResult): String {
            slots += match.value
            return "\u0000${slots.lastIndex}\u0000"
        }
        val fenced = FENCE.replace(text, ::stash)
        val protected = INLINE_CODE.replace(fenced, ::stash)
        return protected to { restored ->
            var next = restored
            slots.indices.reversed().forEach { i ->
                next = next.replace("\u0000$i\u0000", slots[i])
            }
            next
        }
    }
}
