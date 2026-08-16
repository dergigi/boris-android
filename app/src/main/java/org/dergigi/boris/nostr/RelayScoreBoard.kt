package org.dergigi.boris.nostr

/**
 * Relay coverage derived from the cached NIP-65 lists of a set of authors
 * (typically the user's follows). Persistence comes for free: the underlying
 * kind 10002 events are stored in EventCache.
 */
object RelayScoreBoard {
    /** relay url -> authors (subset of [authors]) that write there. Cache-only. */
    fun coverage(authors: Collection<String>): Map<String, Set<String>> {
        val map = mutableMapOf<String, MutableSet<String>>()
        for (author in authors.map { it.lowercase() }.distinct()) {
            RelayListRepository.writeRelays(author).orEmpty()
                .mapNotNull(LocalRelays::resolve)
                .filterNot(LocalRelays::isLocal)
                .forEach { url -> map.getOrPut(url) { mutableSetOf() }.add(author) }
        }
        return map
    }

    /** How many of [authors] list [relayUrl] as a write relay. */
    fun coverageCounts(authors: Collection<String>): Map<String, Int> =
        coverage(authors).mapValues { it.value.size }

    /** Top relays by how many of [authors] write there. */
    fun topRelays(authors: Collection<String>, limit: Int): List<String> =
        coverage(authors).entries
            .sortedByDescending { it.value.size }
            .take(limit)
            .map { it.key }
}
