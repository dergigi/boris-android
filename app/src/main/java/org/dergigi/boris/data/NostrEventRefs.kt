package org.dergigi.boris.data

import org.dergigi.boris.nostr.HintedRelays
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.Profile

data class NostrEventRef(
    val encoded: String,
    val eventId: String,
    val relays: List<String> = emptyList(),
    val author: String? = null,
    val kind: Int? = null,
) {
    val uri: String get() = "nostr:$encoded"
}

data class ResolvedEventRef(
    val event: Nip01Event,
    val profile: Profile? = null,
)

object NostrEventRefs {
    private val bech32Body = "023456789acdefghjklmnpqrstuvwxyz"
    private val EVENT_MENTION = Regex(
        """(?<![/\w])(?:nostr:(?://)?)(nevent1[$bech32Body]+|note1[$bech32Body]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val MARKDOWN_LINK = Regex("""^\[([^\[\]]*)\]\(([^)]+)\)$""")
    private val ANGLE_LINK = Regex("""^<([^>]+)>$""")

    fun collect(markdown: String): List<NostrEventRef> {
        val (protected, _) = NostrMentions.protectCode(markdown)
        val linkUrls = NostrMentions.markdownLinkUrlRanges(protected)
        val refs = linkedMapOf<String, NostrEventRef>()
        for (match in EVENT_MENTION.findAll(protected)) {
            if (linkUrls.any { match.range.first in it || match.range.last in it }) continue
            val ref = decode(match.groupValues[1]) ?: continue
            rememberHints(ref)
            refs[ref.eventId] = refs[ref.eventId]?.let { merge(it, ref) } ?: ref
        }
        return refs.values.toList()
    }

    fun parseStandalone(text: String): NostrEventRef? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || MARKDOWN_LINK.matches(trimmed)) return null
        val dest = ANGLE_LINK.matchEntire(trimmed)?.groupValues?.get(1)?.trim() ?: trimmed
        if (dest.contains(Regex("\\s"))) return null
        val match = EVENT_MENTION.find(dest) ?: return null
        if (match.range.first != 0 || match.range.last != dest.lastIndex) return null
        return decode(match.groupValues[1])
    }

    fun rewrite(
        markdown: String,
        resolved: Map<String, ResolvedEventRef>,
        noteByAuthor: (String) -> String,
    ): String {
        if (resolved.isEmpty()) return markdown
        val (protected, restore) = NostrMentions.protectCode(markdown)
        val linkUrls = NostrMentions.markdownLinkUrlRanges(protected)
        val out = StringBuilder()
        var last = 0
        for (match in EVENT_MENTION.findAll(protected)) {
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
            if (isStandaloneParagraph(protected, start, endExclusive)) {
                out.append(protected, start, endExclusive)
            } else {
                out.append(replacementFor(match, resolved, noteByAuthor))
            }
            last = endExclusive
        }
        out.append(protected, last, protected.length)
        return restore(out.toString())
    }

    fun inlineLabel(resolved: ResolvedEventRef, noteByAuthor: (String) -> String): String? {
        return when (resolved.event.kind) {
            Nip01Event.KIND_LONG_FORM -> Nip23.title(resolved.event)
            Nip01Event.KIND_TEXT_NOTE ->
                noteByAuthor(Profile.displayName(resolved.event.pubkey, resolved.profile))
            else -> null
        }
    }

    fun cardTitle(resolved: ResolvedEventRef): String {
        return when (resolved.event.kind) {
            Nip01Event.KIND_LONG_FORM ->
                Nip23.title(resolved.event) ?: NoteCover.title(resolved.event)
            else -> NoteCover.title(resolved.event)
        }
    }

    fun resolvedFrom(
        events: Map<String, Nip01Event>,
        profiles: Map<String, Profile>,
    ): Map<String, ResolvedEventRef> =
        events.mapNotNull { (id, event) ->
            if (
                event.kind != Nip01Event.KIND_TEXT_NOTE &&
                event.kind != Nip01Event.KIND_LONG_FORM
            ) {
                return@mapNotNull null
            }
            id.lowercase() to ResolvedEventRef(event, profiles[event.pubkey.lowercase()])
        }.toMap()

    fun decode(encoded: String): NostrEventRef? {
        val target = NostrLink.parse("nostr:${encoded.lowercase()}") as? NostrTarget.Note ?: return null
        return NostrEventRef(
            encoded = target.encoded,
            eventId = target.eventId,
            relays = target.relays,
            author = target.author,
            kind = target.kind,
        )
    }

    private fun replacementFor(
        match: MatchResult,
        resolved: Map<String, ResolvedEventRef>,
        noteByAuthor: (String) -> String,
    ): String {
        val ref = decode(match.groupValues[1]) ?: return match.value
        val found = resolved[ref.eventId] ?: return match.value
        val label = inlineLabel(found, noteByAuthor) ?: return match.value
        return "[${escapeLabel(label)}](${ref.uri})"
    }

    private fun merge(existing: NostrEventRef, incoming: NostrEventRef): NostrEventRef {
        val relays = (existing.relays + incoming.relays).distinct()
        return existing.copy(
            relays = relays,
            author = existing.author ?: incoming.author,
            kind = existing.kind ?: incoming.kind,
            encoded = if (existing.relays.isEmpty() && incoming.relays.isNotEmpty()) {
                incoming.encoded
            } else {
                existing.encoded
            },
        )
    }

    private fun isStandaloneParagraph(text: String, start: Int, endExclusive: Int): Boolean {
        val paraStart = text.lastIndexOf("\n\n", start - 1).let { if (it < 0) 0 else it + 2 }
        val paraEnd = text.indexOf("\n\n", endExclusive).let { if (it < 0) text.length else it }
        return parseStandalone(text.substring(paraStart, paraEnd)) != null
    }

    private fun rememberHints(ref: NostrEventRef) {
        val author = ref.author?.takeIf { it.length == 64 } ?: return
        if (ref.relays.isNotEmpty()) HintedRelays.remember(author, ref.relays)
    }

    private fun escapeLabel(label: String): String =
        label.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]")
}

internal fun <T> T.takeIfActive(active: Boolean): T? = takeIf { active }
