package org.dergigi.boris.nostr

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * A long-lived relay connection multiplexing REQ subscriptions and publishes
 * over one websocket. Connects lazily on first use and reconnects on the next
 * use after a failure (with a short fail-fast window to avoid churn).
 */
class PooledRelay internal constructor(
    val url: String,
    private val client: OkHttpClient,
) {
    private class Sub(val onEvent: (Nip01Event) -> Unit, val onEose: () -> Unit)

    private val lock = Any()
    private var socket: WebSocket? = null
    private var connecting = false

    @Volatile
    private var open = false

    @Volatile
    private var lastFailureAt = 0L

    @Volatile
    internal var lastUsedAt = System.currentTimeMillis()

    private val pending = ArrayDeque<String>()
    private val subs = ConcurrentHashMap<String, Sub>()
    private val publishes = ConcurrentHashMap<String, (Boolean) -> Unit>()

    internal fun idle(): Boolean = subs.isEmpty() && publishes.isEmpty()

    /** [onEose] fires on end-of-stored-events, connection failure, or relay-side CLOSED. */
    fun subscribe(
        subId: String,
        filters: List<JSONObject>,
        onEvent: (Nip01Event) -> Unit,
        onEose: () -> Unit,
    ) {
        lastUsedAt = System.currentTimeMillis()
        subs[subId] = Sub(onEvent, onEose)
        val req = JSONArray().put("REQ").put(subId)
        filters.forEach { req.put(it) }
        send(req.toString())
    }

    fun unsubscribe(subId: String) {
        if (subs.remove(subId) != null && open) {
            runCatching { socket?.send(JSONArray().put("CLOSE").put(subId).toString()) }
        }
    }

    /** [onResult] fires exactly once unless the relay never answers; see [cancelPublish]. */
    fun publish(event: Nip01Event, onResult: (Boolean) -> Unit) {
        lastUsedAt = System.currentTimeMillis()
        publishes[event.id.lowercase()] = onResult
        send(
            JSONArray()
                .put("EVENT")
                .put(JSONObject(event.toJsonString()))
                .toString(),
        )
    }

    /** Drops the pending publish handler after a timeout so it cannot fire late. */
    fun cancelPublish(eventId: String) {
        publishes.remove(eventId.lowercase())
    }

    internal fun close() {
        synchronized(lock) {
            open = false
            connecting = false
            pending.clear()
            socket?.close(1000, null)
            socket = null
        }
        failAll()
    }

    private fun send(text: String) {
        val failFast: Boolean
        synchronized(lock) {
            if (open) {
                socket?.send(text)
                return
            }
            pending.addLast(text)
            failFast = !connecting &&
                System.currentTimeMillis() - lastFailureAt < RelayPool.RECONNECT_COOLDOWN_MS
            if (!failFast) connectLocked()
        }
        if (failFast) {
            synchronized(lock) { pending.clear() }
            failAll()
        }
    }

    private fun connectLocked() {
        if (connecting || open) return
        connecting = true
        val startedAt = System.currentTimeMillis()
        socket = client.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    RelayHealth.onConnectOk(url, System.currentTimeMillis() - startedAt)
                    synchronized(lock) {
                        open = true
                        connecting = false
                        while (pending.isNotEmpty()) {
                            webSocket.send(pending.removeFirst())
                        }
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    route(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    synchronized(lock) {
                        open = false
                        connecting = false
                        socket = null
                        pending.clear()
                    }
                    failAll()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    RelayHealth.onConnectFail(url)
                    synchronized(lock) {
                        open = false
                        connecting = false
                        socket = null
                        pending.clear()
                        lastFailureAt = System.currentTimeMillis()
                    }
                    failAll()
                }
            },
        )
    }

    private fun route(text: String) {
        try {
            val arr = JSONArray(text)
            when (arr.optString(0)) {
                "EVENT" -> {
                    val event = Nip01Event.parse(arr.getJSONObject(2)) ?: return
                    if (event.verify()) {
                        RelayHealth.onEvents(url, 1)
                        subs[arr.optString(1)]?.onEvent(event)
                    }
                }
                "EOSE" -> subs[arr.optString(1)]?.onEose()
                "CLOSED" -> subs.remove(arr.optString(1))?.onEose()
                "OK" -> publishes.remove(arr.optString(1).lowercase())?.invoke(arr.optBoolean(2))
            }
        } catch (_: Exception) {
        }
    }

    private fun failAll() {
        for (subId in subs.keys.toList()) {
            subs.remove(subId)?.onEose()
        }
        for (eventId in publishes.keys.toList()) {
            publishes.remove(eventId)?.invoke(false)
        }
    }
}

/**
 * Persistent websocket pool: hot relays (own relay set plus top outbox relays)
 * keep their sockets open across queries; one-off targets use ephemeral sockets
 * closed after an idle period.
 */
object RelayPool {
    internal const val RECONNECT_COOLDOWN_MS = 10_000L
    private const val IDLE_CLOSE_MS = 60_000L
    private const val MAX_PERSISTENT = 15
    private const val MAX_EPHEMERAL = 25

    private val relays = ConcurrentHashMap<String, PooledRelay>()

    @Volatile
    private var persistent: Set<String> = emptySet()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    /** Relays whose sockets should stay open across queries, capped at [MAX_PERSISTENT]. */
    fun markPersistent(urls: Collection<String>) {
        persistent = urls.mapNotNull(LocalRelays::resolve).distinct().take(MAX_PERSISTENT).toSet()
    }

    fun acquire(url: String): PooledRelay {
        sweep()
        return relays.computeIfAbsent(url) { PooledRelay(it, client) }
    }

    /** Close all sockets, e.g. when the app goes to background. */
    fun closeAll() {
        for (url in relays.keys.toList()) {
            relays.remove(url)?.close()
        }
    }

    /** Closes idle ephemeral sockets and enforces the ephemeral cap. */
    private fun sweep() {
        val now = System.currentTimeMillis()
        val hot = persistent
        for (relay in relays.values.toList()) {
            if (relay.url in hot) continue
            if (relay.idle() && now - relay.lastUsedAt > IDLE_CLOSE_MS) {
                relays.remove(relay.url)?.close()
            }
        }
        val ephemeral = relays.values.filter { it.url !in hot }
        val excess = ephemeral.size - MAX_EPHEMERAL
        if (excess > 0) {
            ephemeral.filter { it.idle() }
                .sortedBy { it.lastUsedAt }
                .take(excess)
                .forEach { relays.remove(it.url)?.close() }
        }
    }
}
