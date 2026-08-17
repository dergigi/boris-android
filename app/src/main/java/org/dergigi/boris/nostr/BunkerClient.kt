package org.dergigi.boris.nostr

import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

sealed class BunkerSignResult {
    data class Signed(val event: Nip01Event) : BunkerSignResult()
    data object RelayTimeout : BunkerSignResult()
    data object Rejected : BunkerSignResult()
}

sealed class BunkerDecryptResult {
    data class Plaintext(val value: String) : BunkerDecryptResult()
    data object RelayTimeout : BunkerDecryptResult()
    data object Rejected : BunkerDecryptResult()
}

sealed class BunkerEncryptResult {
    data class Ciphertext(val value: String) : BunkerEncryptResult()
    data object RelayTimeout : BunkerEncryptResult()
    data object Rejected : BunkerEncryptResult()
}

sealed class BunkerResult {
    data class Success(val userHex: String, val clientPrivkey: ByteArray) : BunkerResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return userHex == other.userHex && clientPrivkey.contentEquals(other.clientPrivkey)
        }

        override fun hashCode(): Int = 31 * userHex.hashCode() + clientPrivkey.contentHashCode()
    }

    data object BadUri : BunkerResult()
    data object RelayTimeout : BunkerResult()
    data object Rejected : BunkerResult()
    data object MissingPubkey : BunkerResult()
}

class BunkerClient(
    private val client: OkHttpClient = defaultClient,
    private val onAuthUrl: (String) -> Unit,
) {
    fun pair(uri: String): BunkerResult {
        val parsed = BunkerUri.parse(uri) ?: return BunkerResult.BadUri
        val keypair = ClientKeypair.generate()
        val sockets = mutableListOf<RelaySocket>()
        val inbox = LinkedBlockingQueue<Nip01Event>()
        val lastEvent = ConcurrentHashMap<RelaySocket, String>()
        val seenAuthUrls = mutableSetOf<String>()
        val subId = newId()
        try {
            if (!openSockets(parsed.relays, keypair, parsed.remoteSignerPubkey, subId, sockets, inbox, lastEvent)) {
                return BunkerResult.RelayTimeout
            }
            val secret = parsed.secret?.takeIf { it.isNotEmpty() }
            val connectId = newId()
            publishConnect(sockets, keypair, parsed.remoteSignerPubkey, connectId, secret, lastEvent)
            val connectOutcome = awaitRpc(
                inbox,
                connectId,
                keypair,
                parsed.remoteSignerPubkey,
                seenAuthUrls,
                firstTimeoutMs = RPC_TIMEOUT_MS,
            )
            if (connectOutcome == null) return BunkerResult.RelayTimeout
            if (connectOutcome.rejected) return BunkerResult.Rejected

            val gpId = newId()
            publish(
                sockets,
                keypair,
                parsed.remoteSignerPubkey,
                rpcJson(gpId, "get_public_key", JSONArray()),
                lastEvent,
            )
            val gpOutcome = awaitRpc(
                inbox,
                gpId,
                keypair,
                parsed.remoteSignerPubkey,
                seenAuthUrls,
                firstTimeoutMs = RPC_TIMEOUT_MS,
            ) ?: return BunkerResult.RelayTimeout
            if (gpOutcome.rejected) return BunkerResult.Rejected
            val userHex = Nip19.normalizePubkey(gpOutcome.result.orEmpty())
                ?: return BunkerResult.MissingPubkey
            return BunkerResult.Success(userHex, keypair.privkey)
        } catch (_: Exception) {
            return BunkerResult.Rejected
        } finally {
            sockets.forEach { it.close() }
        }
    }

    fun logout(relays: List<String>, remoteSignerPubkey: String, clientPrivkey: ByteArray) {
        val keypair = ClientKeypair.fromPrivkey(clientPrivkey) ?: return
        val sockets = mutableListOf<RelaySocket>()
        val inbox = LinkedBlockingQueue<Nip01Event>()
        val lastEvent = ConcurrentHashMap<RelaySocket, String>()
        val subId = newId()
        try {
            if (!openSockets(relays, keypair, remoteSignerPubkey, subId, sockets, inbox, lastEvent)) return
            val id = newId()
            publish(sockets, keypair, remoteSignerPubkey, rpcJson(id, "logout", JSONArray()), lastEvent)
            awaitRpc(
                inbox,
                id,
                keypair,
                remoteSignerPubkey,
                seenAuthUrls = mutableSetOf(),
                firstTimeoutMs = LOGOUT_TIMEOUT_MS,
            )
        } catch (_: Exception) {
        } finally {
            sockets.forEach { it.close() }
        }
    }

    fun signEvent(
        relays: List<String>,
        remoteSignerPubkey: String,
        clientPrivkey: ByteArray,
        unsignedJson: String,
    ): BunkerSignResult {
        val keypair = ClientKeypair.fromPrivkey(clientPrivkey) ?: return BunkerSignResult.Rejected
        val sockets = mutableListOf<RelaySocket>()
        val inbox = LinkedBlockingQueue<Nip01Event>()
        val lastEvent = ConcurrentHashMap<RelaySocket, String>()
        val seenAuthUrls = mutableSetOf<String>()
        val subId = newId()
        try {
            if (!openSockets(relays, keypair, remoteSignerPubkey, subId, sockets, inbox, lastEvent)) {
                return BunkerSignResult.RelayTimeout
            }
            val id = newId()
            publish(
                sockets,
                keypair,
                remoteSignerPubkey,
                rpcJson(id, "sign_event", JSONArray().put(unsignedJson)),
                lastEvent,
            )
            val outcome = awaitRpc(
                inbox,
                id,
                keypair,
                remoteSignerPubkey,
                seenAuthUrls,
                firstTimeoutMs = RPC_TIMEOUT_MS,
            ) ?: return BunkerSignResult.RelayTimeout
            if (outcome.rejected) return BunkerSignResult.Rejected
            val event = try {
                Nip01Event.parse(JSONObject(outcome.result.orEmpty()))
            } catch (_: Exception) {
                null
            } ?: return BunkerSignResult.Rejected
            return BunkerSignResult.Signed(event)
        } catch (_: Exception) {
            return BunkerSignResult.Rejected
        } finally {
            sockets.forEach { it.close() }
        }
    }

    fun decrypt(
        relays: List<String>,
        remoteSignerPubkey: String,
        clientPrivkey: ByteArray,
        peerPubkeyHex: String,
        ciphertext: String,
        nip44: Boolean,
    ): BunkerDecryptResult {
        val keypair = ClientKeypair.fromPrivkey(clientPrivkey) ?: return BunkerDecryptResult.Rejected
        val sockets = mutableListOf<RelaySocket>()
        val inbox = LinkedBlockingQueue<Nip01Event>()
        val lastEvent = ConcurrentHashMap<RelaySocket, String>()
        val seenAuthUrls = mutableSetOf<String>()
        val subId = newId()
        try {
            if (!openSockets(relays, keypair, remoteSignerPubkey, subId, sockets, inbox, lastEvent)) {
                return BunkerDecryptResult.RelayTimeout
            }
            val id = newId()
            val method = if (nip44) "nip44_decrypt" else "nip04_decrypt"
            publish(
                sockets,
                keypair,
                remoteSignerPubkey,
                rpcJson(id, method, JSONArray().put(peerPubkeyHex).put(ciphertext)),
                lastEvent,
            )
            val outcome = awaitRpc(
                inbox,
                id,
                keypair,
                remoteSignerPubkey,
                seenAuthUrls,
                firstTimeoutMs = RPC_TIMEOUT_MS,
            ) ?: return BunkerDecryptResult.RelayTimeout
            if (outcome.rejected) return BunkerDecryptResult.Rejected
            val plaintext = outcome.result?.takeIf { it.isNotEmpty() }
                ?: return BunkerDecryptResult.Rejected
            return BunkerDecryptResult.Plaintext(plaintext)
        } catch (_: Exception) {
            return BunkerDecryptResult.Rejected
        } finally {
            sockets.forEach { it.close() }
        }
    }

    fun encrypt(
        relays: List<String>,
        remoteSignerPubkey: String,
        clientPrivkey: ByteArray,
        peerPubkeyHex: String,
        plaintext: String,
    ): BunkerEncryptResult {
        val keypair = ClientKeypair.fromPrivkey(clientPrivkey) ?: return BunkerEncryptResult.Rejected
        val sockets = mutableListOf<RelaySocket>()
        val inbox = LinkedBlockingQueue<Nip01Event>()
        val lastEvent = ConcurrentHashMap<RelaySocket, String>()
        val seenAuthUrls = mutableSetOf<String>()
        val subId = newId()
        try {
            if (!openSockets(relays, keypair, remoteSignerPubkey, subId, sockets, inbox, lastEvent)) {
                return BunkerEncryptResult.RelayTimeout
            }
            val id = newId()
            publish(
                sockets,
                keypair,
                remoteSignerPubkey,
                rpcJson(id, "nip44_encrypt", JSONArray().put(peerPubkeyHex).put(plaintext)),
                lastEvent,
            )
            val outcome = awaitRpc(
                inbox,
                id,
                keypair,
                remoteSignerPubkey,
                seenAuthUrls,
                firstTimeoutMs = RPC_TIMEOUT_MS,
            ) ?: return BunkerEncryptResult.RelayTimeout
            if (outcome.rejected) return BunkerEncryptResult.Rejected
            val ciphertext = outcome.result?.takeIf { it.isNotEmpty() }
                ?: return BunkerEncryptResult.Rejected
            return BunkerEncryptResult.Ciphertext(ciphertext)
        } catch (_: Exception) {
            return BunkerEncryptResult.Rejected
        } finally {
            sockets.forEach { it.close() }
        }
    }

    private fun openSockets(
        relays: List<String>,
        keypair: ClientKeypair,
        remoteSigner: String,
        subId: String,
        sockets: MutableList<RelaySocket>,
        inbox: LinkedBlockingQueue<Nip01Event>,
        lastEvent: ConcurrentHashMap<RelaySocket, String>,
    ): Boolean {
        val wssCount = relays.count { it.startsWith("wss://", ignoreCase = true) }
        val opened = CountDownLatch(wssCount.coerceAtLeast(1))
        for (relay in relays) {
            val socket = RelaySocket(relay, client)
            sockets.add(socket)
            val signaled = AtomicBoolean(false)
            val isWss = relay.startsWith("wss://", ignoreCase = true)
            fun signal() {
                if (isWss && signaled.compareAndSet(false, true)) opened.countDown()
            }
            try {
                socket.open(
                    onOpen = { signal() },
                    onMessage = { text ->
                        handleRelayMessage(
                            text = text,
                            socket = socket,
                            keypair = keypair,
                            remoteSigner = remoteSigner,
                            subId = subId,
                            inbox = inbox,
                            lastEvent = lastEvent,
                        )
                    },
                    onFailure = { signal() },
                )
            } catch (_: Exception) {
                signal()
            }
        }
        opened.await(RELAY_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        if (sockets.none { it.isOpen }) return false
        // Amethyst/nak: let NIP-42 AUTH arrive and complete before the first REQ.
        Thread.sleep(AUTH_SETTLE_MS)
        for (socket in sockets) {
            if (socket.isOpen) socket.send(reqMessage(keypair.pubkeyHex, subId))
        }
        // Brief pause so REQ is accepted after any late AUTH.
        Thread.sleep(AUTH_SETTLE_MS)
        return sockets.any { it.isOpen }
    }

    private fun handleRelayMessage(
        text: String,
        socket: RelaySocket,
        keypair: ClientKeypair,
        remoteSigner: String,
        subId: String,
        inbox: LinkedBlockingQueue<Nip01Event>,
        lastEvent: ConcurrentHashMap<RelaySocket, String>,
    ) {
        try {
            val arr = JSONArray(text)
            when (arr.optString(0)) {
                "AUTH" -> {
                    if (answerAuth(arr.optString(1), socket, keypair)) {
                        // Subscriptions issued before AUTH are dropped by auth relays.
                        socket.send(reqMessage(keypair.pubkeyHex, subId))
                        lastEvent[socket]?.let { socket.send(it) }
                    }
                }
                "OK" -> {
                    val ok = arr.optBoolean(2)
                    val message = arr.optString(3)
                    if (!ok && message.contains("auth-required", ignoreCase = true)) {
                        // Challenge may already have been answered; re-REQ + republish.
                        socket.send(reqMessage(keypair.pubkeyHex, subId))
                        lastEvent[socket]?.let { socket.send(it) }
                    }
                }
                "CLOSED" -> {
                    val message = arr.optString(2)
                    if (message.contains("auth-required", ignoreCase = true)) {
                        socket.send(reqMessage(keypair.pubkeyHex, subId))
                    }
                }
                "EVENT" -> {
                    incomingEvent(text, keypair.pubkeyHex, remoteSigner)?.let { inbox.offer(it) }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun answerAuth(challenge: String, socket: RelaySocket, keypair: ClientKeypair): Boolean {
        if (challenge.isEmpty()) return false
        return try {
            val event = Nip01Event.sign(
                privkey = keypair.privkey,
                pubkeyHex = keypair.pubkeyHex,
                kind = Nip01Event.KIND_AUTH,
                tags = listOf(
                    listOf("relay", socket.url),
                    listOf("challenge", challenge),
                ),
                content = "",
            )
            socket.send(JSONArray().put("AUTH").put(JSONObject(event.toJsonString())).toString())
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun publishConnect(
        sockets: List<RelaySocket>,
        keypair: ClientKeypair,
        remoteSignerPubkey: String,
        id: String,
        secret: String?,
        lastEvent: ConcurrentHashMap<RelaySocket, String>,
    ) {
        val params = JSONArray()
            .put(remoteSignerPubkey)
            .put(secret.orEmpty())
            .put("")
            .put(CLIENT_METADATA)
        publish(sockets, keypair, remoteSignerPubkey, rpcJson(id, "connect", params), lastEvent)
    }

    private fun publish(
        sockets: List<RelaySocket>,
        keypair: ClientKeypair,
        remoteSignerPubkey: String,
        plaintext: String,
        lastEvent: ConcurrentHashMap<RelaySocket, String>,
    ) {
        val event = Nip01Event.sign(
            privkey = keypair.privkey,
            pubkeyHex = keypair.pubkeyHex,
            kind = Nip01Event.KIND_RPC,
            tags = listOf(listOf("p", remoteSignerPubkey)),
            content = Nip44.encrypt(plaintext, keypair.privkey, remoteSignerPubkey),
        )
        val message = JSONArray().put("EVENT").put(JSONObject(event.toJsonString())).toString()
        sockets.forEach { socket ->
            lastEvent[socket] = message
            socket.send(message)
        }
    }

    private fun awaitRpc(
        inbox: LinkedBlockingQueue<Nip01Event>,
        requestId: String,
        keypair: ClientKeypair,
        remoteSignerPubkey: String,
        seenAuthUrls: MutableSet<String>,
        firstTimeoutMs: Long,
    ): RpcOutcome? {
        var deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(firstTimeoutMs)
        while (true) {
            val remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
            if (remaining <= 0) return null
            val event = inbox.poll(remaining, TimeUnit.MILLISECONDS) ?: return null
            val plain = try {
                Nip44.decrypt(event.content, keypair.privkey, remoteSignerPubkey)
            } catch (_: Exception) {
                continue
            }
            val json = try {
                JSONObject(plain)
            } catch (_: Exception) {
                continue
            }
            if (json.optString("id") != requestId) continue
            val result = json.optString("result")
            val error = json.optString("error")
            if (result == "auth_url") {
                if (error.isNotEmpty() && seenAuthUrls.add(error)) {
                    onAuthUrl(error)
                }
                deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(RPC_TIMEOUT_MS)
                continue
            }
            if (rpcIsRejected(result, error)) {
                return RpcOutcome(rejected = true)
            }
            return RpcOutcome(result = result, rejected = false)
        }
    }

    private fun incomingEvent(text: String, clientPub: String, remoteSigner: String): Nip01Event? {
        return try {
            val arr = JSONArray(text)
            if (arr.optString(0) != "EVENT") return null
            val event = Nip01Event.parse(arr.getJSONObject(2)) ?: return null
            if (event.kind != Nip01Event.KIND_RPC) return null
            if (!event.pubkey.equals(remoteSigner, ignoreCase = true)) return null
            if (!event.hasPTag(clientPub)) return null
            if (!event.verify()) return null
            event
        } catch (_: Exception) {
            null
        }
    }

    private fun reqMessage(clientPub: String, subId: String): String {
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_RPC))
            .put("#p", JSONArray().put(clientPub))
        return JSONArray().put("REQ").put(subId).put(filter).toString()
    }

    private fun rpcJson(id: String, method: String, params: JSONArray): String =
        JSONObject()
            .put("id", id)
            .put("method", method)
            .put("params", params)
            .toString()

    private fun newId(): String = UUID.randomUUID().toString()

    private data class RpcOutcome(
        val result: String? = null,
        val rejected: Boolean,
    )

    companion object {
        internal fun rpcIsRejected(result: String, error: String): Boolean {
            if (error.contains("already connected", ignoreCase = true)) return false
            return error.isNotEmpty() && result.isEmpty()
        }

        private const val RELAY_CONNECT_TIMEOUT_MS = 15_000L
        private const val RPC_TIMEOUT_MS = 65_000L
        private const val AUTH_SETTLE_MS = 750L
        private const val LOGOUT_TIMEOUT_MS = 8_000L
        private const val CLIENT_METADATA =
            """{"name":"Boris","url":"https://github.com/dergigi/boris-android"}"""

        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }
}
