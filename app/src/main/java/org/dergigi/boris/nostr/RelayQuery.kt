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

    fun fetchProfilePicture(pubkeyHex: String): String? {
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
        return pictureUrl(newest.content)
    }

    fun fetchHighlights(readRelays: List<String>, pubkeyHex: String, url: String): List<Nip01Event> {
        val opened = url
        val normalized = ArticleUrl.normalize(url)
        val filterOpened = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_HIGHLIGHT))
            .put("authors", JSONArray().put(pubkeyHex))
            .put("#r", JSONArray().put(opened))
        val filterNormalized = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_HIGHLIGHT))
            .put("authors", JSONArray().put(pubkeyHex))
            .put("#r", JSONArray().put(normalized))
        val events = query(readRelays, listOf(filterOpened, filterNormalized))
        val articleNorm = ArticleUrl.normalize(url)
        return events.filter { event ->
            event.kind == Nip01Event.KIND_HIGHLIGHT &&
                event.verify() &&
                event.pubkey.equals(pubkeyHex, ignoreCase = true) &&
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

    private fun pictureUrl(content: String): String? {
        return try {
            val url = JSONObject(content).optString("picture").trim()
            if (url.startsWith("http://") || url.startsWith("https://")) url else null
        } catch (_: Exception) {
            null
        }
    }

    private fun newId(): String = UUID.randomUUID().toString().take(12)

    private const val QUERY_TIMEOUT_MS = 8_000L
    private const val PUBLISH_TIMEOUT_MS = 8_000L

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()
}
