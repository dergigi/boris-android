package org.dergigi.boris.nostr

/**
 * NIP-65 outbox routing: groups authors by their write relays so content is
 * fetched from where the authors actually publish.
 */
object OutboxRouter {
    /** How many of an author's write relays to query, for redundancy. */
    const val MIN_REDUNDANCY = 3

    /** Cap on additional outbox relays per query round, kept by author coverage. */
    const val MAX_OUTBOX_RELAYS = 15

    /**
     * Groups [authors] by write relay. Every fallback relay receives the full
     * author list as a safety net (same coverage as before outbox routing), and
     * each author is additionally placed on up to [MIN_REDUNDANCY] of their write
     * relays. The extra outbox relay set is capped at [maxOutboxRelays], keeping
     * the relays that cover the most authors.
     */
    fun route(
        authors: Collection<String>,
        fallbackRelays: List<String>,
        writeRelaysOf: (String) -> List<String>? = RelayListRepository::writeRelays,
        skip: (String) -> Boolean = { false },
        maxOutboxRelays: Int = MAX_OUTBOX_RELAYS,
    ): Map<String, Set<String>> {
        val keys = authors.map { it.lowercase() }.distinct()
        val fallback = fallbackRelays.toSet()
        val outbox = mutableMapOf<String, MutableSet<String>>()
        for (author in keys) {
            writeRelaysOf(author).orEmpty()
                .mapNotNull(LocalRelays::resolve)
                .filterNot { it in fallback || LocalRelays.isLocal(it) || skip(it) }
                .take(MIN_REDUNDANCY)
                .forEach { url -> outbox.getOrPut(url) { mutableSetOf() }.add(author) }
        }
        val kept = outbox.entries
            .sortedByDescending { it.value.size }
            .take(maxOutboxRelays)
        val routes = mutableMapOf<String, Set<String>>()
        val all = keys.toSet()
        fallback.forEach { routes[it] = all }
        kept.forEach { (url, subset) -> routes[url] = subset }
        return routes
    }

    /**
     * Relay targets for a single author's content: [base] plus up to
     * [MIN_REDUNDANCY] of their cached write relays.
     */
    fun authorTargets(
        pubkeyHex: String,
        base: List<String>,
        writeRelaysOf: (String) -> List<String>? = RelayListRepository::writeRelays,
    ): List<String> = buildList {
        addAll(base)
        writeRelaysOf(pubkeyHex.lowercase()).orEmpty()
            .mapNotNull(LocalRelays::resolve)
            .filterNot { LocalRelays.isLocal(it) }
            .take(MIN_REDUNDANCY)
            .forEach { add(it) }
    }.distinct()
}
