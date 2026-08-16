package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip19

/** Builds the Continue Reading section from device-local reading positions. */
object ContinueReading {
    private const val MIN_FRACTION = 0.02f
    private const val MAX_FRACTION = 0.95f
    private val hexId = Regex("^[0-9a-f]{64}$")

    fun articles(limit: Int): List<HighlightedArticle> {
        val out = ArrayList<HighlightedArticle>(limit)
        for ((key, fraction) in ReadingPositionStore.entries()) {
            if (!inProgress(fraction)) continue
            val url = urlForKey(key) ?: continue
            val host = when (val target = NostrLink.parse(url)) {
                is NostrTarget.Article -> target.ref.pointer.identifier.ifBlank { "nostr" }
                is NostrTarget.Note -> "nostr"
                null -> ArticleUrl.host(url) ?: continue
            }
            out.add(HighlightedArticles.decorate(HighlightedArticle(url, host, host, null, 0L)))
            if (out.size >= limit) break
        }
        return out
    }

    internal fun inProgress(fraction: Float): Boolean =
        fraction in MIN_FRACTION..MAX_FRACTION

    /** Maps a canonical position key back to an openable reader URL. */
    internal fun urlForKey(key: String): String? = when {
        key.startsWith("http") -> key
        hexId.matches(key) -> runCatching { "nostr:${Nip19.noteEncode(key)}" }.getOrNull()
        else -> NostrArticle.fromCoordinate(key)?.uri
    }
}
