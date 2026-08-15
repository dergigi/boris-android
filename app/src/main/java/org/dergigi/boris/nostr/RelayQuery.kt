package org.dergigi.boris.nostr

import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.dergigi.boris.data.ArticleUrl

object RelayQuery {
    fun fetchRelayList(pubkeyHex: String): RelayList {
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_RELAY_LIST))
            .put("authors", JSONArray().put(pubkeyHex))
            .put("limit", 10)
        val events = query(RelayList.FALLBACK, listOf(filter))
        return RelayList.parse(events)
    }

    fun fetchAppData(pubkeyHex: String): Nip01Event? {
        val relays = buildList {
            addAll(RelayList.FALLBACK)
            addAll(fetchRelayList(pubkeyHex).read)
        }.distinct()
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_APP_DATA))
            .put("authors", JSONArray().put(pubkeyHex))
            .put("#d", JSONArray().put(Nip78.SETTINGS_D))
            .put("limit", 5)
        return query(relays, listOf(filter))
            .filter { event ->
                event.kind == Nip01Event.KIND_APP_DATA &&
                    event.pubkey.equals(pubkeyHex, ignoreCase = true) &&
                    Nip78.hasSettingsD(event)
            }
            .maxByOrNull { it.createdAt }
    }

    fun fetchProfile(pubkeyHex: String): Profile? {
        val relays = fetchRelayList(pubkeyHex).read
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_METADATA))
            .put("authors", JSONArray().put(pubkeyHex))
            .put("limit", 5)
        val newest = query(relays, listOf(filter))
            .filter { event ->
                event.kind == Nip01Event.KIND_METADATA &&
                    event.pubkey.equals(pubkeyHex, ignoreCase = true)
            }
            .maxByOrNull { it.createdAt } ?: return null
        return Profile.parse(newest.content)
    }

    fun fetchProfilePicture(pubkeyHex: String): String? = fetchProfile(pubkeyHex)?.picture

    fun discoverContentRelays(
        seed: List<String> = RelayList.FALLBACK,
        limit: Int = 12,
    ): List<String> {
        val since = System.currentTimeMillis() / 1000 - DISCOVERY_WINDOW_SECONDS
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_RELAY_DISCOVERY))
            .put("since", since)
            .put("limit", 200)
        val bootstrap = (Nip66.MONITOR_RELAYS + seed).mapNotNull { Nip66.normalize(it) }.distinct()
        val events = query(bootstrap, listOf(filter))
        return Nip66.select(events, seed, limit)
    }

    fun fetchContactPubkeys(pubkeyHex: String): Set<String> {
        val relays = buildList {
            addAll(RelayList.FALLBACK)
            addAll(fetchRelayList(pubkeyHex).read)
        }.distinct()
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_CONTACTS))
            .put("authors", JSONArray().put(pubkeyHex))
            .put("limit", 5)
        val newest = query(relays, listOf(filter))
            .filter { event ->
                event.kind == Nip01Event.KIND_CONTACTS &&
                    event.pubkey.equals(pubkeyHex, ignoreCase = true)
            }
            .maxByOrNull { it.createdAt } ?: return emptySet()
        return newest.pPubkeys()
    }

    fun fetchRecentHighlights(
        readRelays: List<String>,
        limit: Int = 80,
        pubkeyHex: String? = null,
        authors: Collection<String> = emptyList(),
    ): List<Nip01Event> {
        val urls = readRelays.mapNotNull { Nip66.normalize(it) }.distinct()
        if (urls.isEmpty()) return emptyList()
        val keys = buildList {
            if (!pubkeyHex.isNullOrBlank()) add(pubkeyHex.lowercase())
            authors.forEach { key ->
                if (key.isNotBlank()) add(key.lowercase())
            }
        }.distinct()
        val filters = if (keys.isEmpty()) {
            listOf(highlightFilter(limit, emptyList()))
        } else {
            keys.chunked(AUTHOR_CHUNK).map { chunk -> highlightFilter(limit, chunk) }
        }
        val allowed = keys.toSet()
        return query(urls, filters)
            .filter { event ->
                event.kind == Nip01Event.KIND_HIGHLIGHT &&
                    event.content.isNotBlank() &&
                    (allowed.isEmpty() || event.pubkey.lowercase() in allowed)
            }
            .sortedByDescending { it.createdAt }
    }

    fun fetchProfiles(
        readRelays: List<String>,
        pubkeys: List<String>,
    ): Map<String, Profile> {
        val urls = readRelays.mapNotNull { Nip66.normalize(it) }.distinct()
        val keys = pubkeys.map { it.lowercase() }.distinct()
        if (urls.isEmpty() || keys.isEmpty()) return emptyMap()
        val newest = mutableMapOf<String, Nip01Event>()
        for (chunk in keys.chunked(PROFILE_CHUNK)) {
            val filter = JSONObject()
                .put("kinds", JSONArray().put(Nip01Event.KIND_METADATA))
                .put("authors", JSONArray().apply { chunk.forEach { put(it) } })
                .put("limit", chunk.size)
            for (event in query(urls, listOf(filter))) {
                if (event.kind != Nip01Event.KIND_METADATA) continue
                val key = event.pubkey.lowercase()
                val existing = newest[key]
                if (existing == null || event.createdAt > existing.createdAt) {
                    newest[key] = event
                }
            }
        }
        return newest.mapValues { Profile.parse(it.value.content) }
    }

    fun fetchHighlights(
        readRelays: List<String>,
        url: String,
        pubkeyHex: String? = null,
    ): List<Nip01Event> {
        val opened = url
        val normalized = ArticleUrl.normalize(url)
        val filters = listOf(opened, normalized).distinct().map { tagged ->
            JSONObject()
                .put("kinds", JSONArray().put(Nip01Event.KIND_HIGHLIGHT))
                .put("#r", JSONArray().put(tagged))
                .put("limit", 200)
                .also { filter ->
                    if (!pubkeyHex.isNullOrBlank()) {
                        filter.put("authors", JSONArray().put(pubkeyHex))
                    }
                }
        }
        val articleNorm = ArticleUrl.normalize(url)
        return query(readRelays, filters).filter { event ->
            event.kind == Nip01Event.KIND_HIGHLIGHT &&
                event.content.isNotBlank() &&
                (pubkeyHex.isNullOrBlank() || event.pubkey.equals(pubkeyHex, ignoreCase = true)) &&
                event.tags.any { tag ->
                    tag.size >= 2 &&
                        tag[0] == "r" &&
                        ArticleUrl.normalize(tag[1]) == articleNorm
                }
        }
    }

    fun publish(writeRelays: List<String>, event: Nip01Event): Boolean {
        if (writeRelays.isEmpty()) return false
        val sockets = mutableListOf<RelaySocket>()
        val okTrue = AtomicBoolean(false)
        val responses = CountDownLatch(writeRelays.size)
        try {
            for (url in writeRelays) {
                val socket = RelaySocket(url, client)
                sockets.add(socket)
                val signaled = AtomicBoolean(false)
                fun signal() {
                    if (signaled.compareAndSet(false, true)) responses.countDown()
                }
                try {
                    socket.open(
                        onOpen = {
                            val message = JSONArray()
                                .put("EVENT")
                                .put(JSONObject(event.toJsonString()))
                                .toString()
                            socket.send(message)
                        },
                        onMessage = { text ->
                            try {
                                val arr = JSONArray(text)
                                if (arr.optString(0) == "OK") {
                                    if (arr.optBoolean(2)) okTrue.set(true)
                                    signal()
                                }
                            } catch (_: Exception) {
                            }
                        },
                        onFailure = { signal() },
                    )
                } catch (_: Exception) {
                    signal()
                }
            }
            responses.await(PUBLISH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            return okTrue.get()
        } catch (_: Exception) {
            return okTrue.get()
        } finally {
            sockets.forEach { it.close() }
        }
    }

    private fun query(urls: List<String>, filters: List<JSONObject>): List<Nip01Event> {
        if (urls.isEmpty()) return emptyList()
        val sockets = mutableListOf<RelaySocket>()
        val events = ConcurrentHashMap<String, Nip01Event>()
        val eose = CountDownLatch(urls.size)
        try {
            for (url in urls) {
                val socket = RelaySocket(url, client)
                sockets.add(socket)
                val eoseSignaled = AtomicBoolean(false)
                fun signalEose() {
                    if (eoseSignaled.compareAndSet(false, true)) eose.countDown()
                }
                try {
                    socket.open(
                        onOpen = {
                            val req = JSONArray().put("REQ").put(newId())
                            filters.forEach { req.put(it) }
                            socket.send(req.toString())
                        },
                        onMessage = { text ->
                            try {
                                val arr = JSONArray(text)
                                when (arr.optString(0)) {
                                    "EVENT" -> {
                                        val event = Nip01Event.parse(arr.getJSONObject(2)) ?: return@open
                                        if (event.verify()) events[event.id] = event
                                    }
                                    "EOSE" -> signalEose()
                                }
                            } catch (_: Exception) {
                            }
                        },
                        onFailure = { signalEose() },
                    )
                } catch (_: Exception) {
                    signalEose()
                }
            }
            eose.await(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            return events.values.toList()
        } catch (_: Exception) {
            return events.values.toList()
        } finally {
            sockets.forEach { it.close() }
        }
    }

    private fun highlightFilter(limit: Int, authors: List<String>): JSONObject {
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_HIGHLIGHT))
            .put("limit", limit)
        if (authors.isNotEmpty()) {
            filter.put("authors", JSONArray().apply { authors.forEach { put(it) } })
        }
        return filter
    }

    private fun newId(): String = UUID.randomUUID().toString().take(12)

    private const val QUERY_TIMEOUT_MS = 8_000L
    private const val PUBLISH_TIMEOUT_MS = 8_000L
    private const val DISCOVERY_WINDOW_SECONDS = 48L * 60L * 60L
    private const val PROFILE_CHUNK = 25
    private const val AUTHOR_CHUNK = 50

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()
}
