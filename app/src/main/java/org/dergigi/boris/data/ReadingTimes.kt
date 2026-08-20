package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache

object ReadingTimes {
    const val MAX_WEB_FETCHES = 12

    fun minutes(
        items: List<BookmarkItem>,
        archivedKeys: Set<String>,
        fetchUnknownWeb: Boolean,
        reader: ReaderRepository = ReaderRepository(),
    ): Map<String, Int> {
        val out = LinkedHashMap<String, Int>()
        val unknownWeb = mutableListOf<String>()
        for (item in unreadLibraryItems(items, archivedKeys)) {
            val url = item.url ?: continue
            val known = ReadingTimeStore.get(url)
            if (known != null) {
                out[url] = known
                continue
            }
            val cached = cachedMinutes(url, reader)
            if (cached != null) {
                ReadingTimeStore.put(url, cached)
                out[url] = cached
                continue
            }
            if (fetchUnknownWeb && NostrLink.parse(url) == null) {
                unknownWeb.add(url)
            }
        }
        if (fetchUnknownWeb) {
            for (url in unknownWeb.shuffled().take(MAX_WEB_FETCHES)) {
                val fetched = runCatching { reader.fetch(url) }.getOrNull() ?: continue
                val mins = ReadingTime.minutes(fetched.body) ?: continue
                out[url] = mins
            }
        }
        return out
    }

    private fun cachedMinutes(url: String, reader: ReaderRepository): Int? {
        return when (val target = NostrLink.parse(url)) {
            is NostrTarget.Article -> {
                val pointer = target.ref.pointer
                val event = EventCache.latest(pointer.kind, pointer.pubkey, pointer.identifier)
                event?.let { ReadingTime.minutes(it.content) }
            }
            is NostrTarget.Note -> {
                val event = EventCache.event(target.eventId)
                event?.let { ReadingTime.minutes(it.content) }
            }
            is NostrTarget.Profile -> null
            null -> reader.peekCached(url)?.let { ReadingTime.minutes(it.body) }
        }
    }
}
