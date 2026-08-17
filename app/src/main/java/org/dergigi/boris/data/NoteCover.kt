package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Profile

/** Cover/title helpers for kind-1 notes shown as cards. */
object NoteCover {
    fun image(event: Nip01Event?): String? {
        if (event == null) return null
        ArticleCover.firstMarkdownImage(event.content)?.let { return it }
        return authorPicture(event.pubkey)
    }

    fun title(event: Nip01Event?): String {
        if (event == null) return "Note"
        val line = event.content.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && !isBareImageLine(it) }
            ?: return "Note"
        return if (line.length <= 80) line else line.take(79).trimEnd() + "…"
    }

    fun authorPicture(pubkeyHex: String): String? =
        EventCache.latest(Nip01Event.KIND_METADATA, pubkeyHex)
            ?.let { Profile.parse(it.content).picture }

    private fun isBareImageLine(line: String): Boolean {
        val bare = line.trimEnd('.', ',', ';')
        return bare.startsWith("http", ignoreCase = true) && UrlExtractor.isImageUrl(bare)
    }
}
