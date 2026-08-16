package org.dergigi.boris.nostr

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Batch resolution of NIP-65 relay lists (kind 10002) for arbitrary authors.
 * Backed by EventCache (which persists relay lists); missing lists are fetched
 * in chunks from indexer relays plus the fallback set.
 */
object RelayListRepository {
    val INDEXER_RELAYS = listOf(
        "wss://purplepag.es",
        "wss://relay.nostr.band",
    )

    private const val AUTHOR_CHUNK = 50

    /** Authors attempted this session, so unknown authors are not re-queried every refresh. */
    private val attempted = ConcurrentHashMap.newKeySet<String>()

    /** Cached relay list for an author, or null when unknown. Never falls back. */
    fun cached(pubkeyHex: String): RelayList? =
        EventCache.latest(Nip01Event.KIND_RELAY_LIST, pubkeyHex)
            ?.let { RelayList.parse(listOf(it)) }

    /** Cached write relays for an author, or null when their relay list is unknown. */
    fun writeRelays(pubkeyHex: String): List<String>? = cached(pubkeyHex)?.write

    /**
     * Ensure kind 10002 lists for [pubkeys] are cached, fetching missing ones once
     * per session. Blocking; call from an IO context.
     */
    fun prefetch(pubkeys: Collection<String>) {
        val missing = pubkeys
            .map { it.lowercase() }
            .distinct()
            .filter { key ->
                EventCache.latest(Nip01Event.KIND_RELAY_LIST, key) == null && attempted.add(key)
            }
        if (missing.isEmpty()) return
        val urls = buildList {
            addAll(INDEXER_RELAYS)
            addAll(RelayList.FALLBACK)
        }
        for (chunk in missing.chunked(AUTHOR_CHUNK)) {
            val filter = JSONObject()
                .put("kinds", JSONArray().put(Nip01Event.KIND_RELAY_LIST))
                .put("authors", JSONArray().apply { chunk.forEach { put(it) } })
                .put("limit", chunk.size)
            val events = RelayQuery.rawQuery(urls, listOf(filter))
                .filter { it.kind == Nip01Event.KIND_RELAY_LIST }
            EventCache.putAll(events)
        }
    }
}
