package org.dergigi.boris.nostr

import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

sealed class NwcResult {
    data class Ok(val result: JSONObject) : NwcResult()
    data class Error(val code: String, val message: String) : NwcResult()
    data object Timeout : NwcResult()
}

/**
 * Minimal NIP-47 client: one relay round trip per request. Reads the wallet's
 * kind 13194 info event to pick NIP-44 v2 when advertised, NIP-04 otherwise.
 * Blocking; call from an IO dispatcher.
 */
class NwcClient(
    private val walletPubkey: String,
    private val relays: List<String>,
    secret: ByteArray,
    private val client: OkHttpClient = defaultClient,
) {
    private val keypair = ClientKeypair.fromPrivkey(secret)

    fun getInfo(): NwcResult = request("get_info", JSONObject())

    fun getBalance(): NwcResult = request("get_balance", JSONObject())

    fun payInvoice(bolt11: String): NwcResult =
        request("pay_invoice", JSONObject().put("invoice", bolt11), timeoutMs = PAY_TIMEOUT_MS)

    private fun request(method: String, params: JSONObject, timeoutMs: Long = RPC_TIMEOUT_MS): NwcResult {
        val keypair = keypair ?: return NwcResult.Error("BAD_SECRET", "Invalid wallet secret")
        val sockets = mutableListOf<RelaySocket>()
        val inbox = LinkedBlockingQueue<Nip01Event>()
        val infoSub = newId()
        val responseSub = newId()
        val infoDone = CountDownLatch(1)
        val nip44 = AtomicBoolean(false)
        try {
            val opened = CountDownLatch(relays.size.coerceAtLeast(1))
            for (relay in relays) {
                val socket = RelaySocket(relay, client)
                sockets.add(socket)
                val signaled = AtomicBoolean(false)
                fun signal() {
                    if (signaled.compareAndSet(false, true)) opened.countDown()
                }
                try {
                    socket.open(
                        onOpen = { signal() },
                        onMessage = { text ->
                            handleMessage(text, socket, keypair, infoSub, responseSub, inbox, infoDone, nip44)
                        },
                        onFailure = { signal() },
                    )
                } catch (_: Exception) {
                    signal()
                }
            }
            opened.await(RELAY_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (sockets.none { it.isOpen }) return NwcResult.Timeout

            sockets.filter { it.isOpen }.forEach { socket ->
                socket.send(infoReq(infoSub))
                socket.send(responseReq(responseSub, keypair.pubkeyHex))
            }
            infoDone.await(INFO_TIMEOUT_MS, TimeUnit.MILLISECONDS)

            val plaintext = JSONObject().put("method", method).put("params", params).toString()
            val useNip44 = nip44.get()
            val event = Nip01Event.sign(
                privkey = keypair.privkey,
                pubkeyHex = keypair.pubkeyHex,
                kind = Nip01Event.KIND_NWC_REQUEST,
                tags = buildList {
                    add(listOf("p", walletPubkey))
                    if (useNip44) add(listOf("encryption", "nip44_v2"))
                },
                content = if (useNip44) {
                    Nip44.encrypt(plaintext, keypair.privkey, walletPubkey)
                } else {
                    Nip44.encryptLegacy(plaintext, keypair.privkey, walletPubkey)
                },
            )
            val message = JSONArray().put("EVENT").put(JSONObject(event.toJsonString())).toString()
            sockets.filter { it.isOpen }.forEach { it.send(message) }
            return awaitResponse(inbox, event.id, keypair, timeoutMs)
        } catch (e: Exception) {
            return NwcResult.Error("INTERNAL", e.message ?: "NWC request failed")
        } finally {
            sockets.forEach { it.close() }
        }
    }

    private fun awaitResponse(
        inbox: LinkedBlockingQueue<Nip01Event>,
        requestId: String,
        keypair: ClientKeypair,
        timeoutMs: Long,
    ): NwcResult {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (true) {
            val remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
            if (remaining <= 0) return NwcResult.Timeout
            val event = inbox.poll(remaining, TimeUnit.MILLISECONDS) ?: return NwcResult.Timeout
            if (!event.tagValue("e").equals(requestId, ignoreCase = true)) continue
            val json = try {
                JSONObject(Nip44.decrypt(event.content, keypair.privkey, walletPubkey))
            } catch (_: Exception) {
                continue
            }
            val error = json.optJSONObject("error")
            if (error != null) {
                return NwcResult.Error(
                    code = error.optString("code").ifEmpty { "OTHER" },
                    message = error.optString("message"),
                )
            }
            return NwcResult.Ok(json.optJSONObject("result") ?: JSONObject())
        }
    }

    private fun handleMessage(
        text: String,
        socket: RelaySocket,
        keypair: ClientKeypair,
        infoSub: String,
        responseSub: String,
        inbox: LinkedBlockingQueue<Nip01Event>,
        infoDone: CountDownLatch,
        nip44: AtomicBoolean,
    ) {
        try {
            val arr = JSONArray(text)
            when (arr.optString(0)) {
                "AUTH" -> answerAuth(arr.optString(1), socket, keypair)
                "EOSE" -> if (arr.optString(1) == infoSub) infoDone.countDown()
                "CLOSED" -> if (arr.optString(1) == infoSub) infoDone.countDown()
                "EVENT" -> {
                    val event = Nip01Event.parse(arr.getJSONObject(2)) ?: return
                    if (!event.pubkey.equals(walletPubkey, ignoreCase = true) || !event.verify()) return
                    when (event.kind) {
                        Nip01Event.KIND_NWC_INFO -> {
                            if (supportsNip44(event)) nip44.set(true)
                            infoDone.countDown()
                        }
                        Nip01Event.KIND_NWC_RESPONSE -> if (event.hasPTag(keypair.pubkeyHex)) inbox.offer(event)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun answerAuth(challenge: String, socket: RelaySocket, keypair: ClientKeypair) {
        if (challenge.isEmpty()) return
        val event = Nip01Event.sign(
            privkey = keypair.privkey,
            pubkeyHex = keypair.pubkeyHex,
            kind = Nip01Event.KIND_AUTH,
            tags = listOf(listOf("relay", socket.url), listOf("challenge", challenge)),
            content = "",
        )
        socket.send(JSONArray().put("AUTH").put(JSONObject(event.toJsonString())).toString())
    }

    private fun infoReq(subId: String): String {
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_NWC_INFO))
            .put("authors", JSONArray().put(walletPubkey))
            .put("limit", 1)
        return JSONArray().put("REQ").put(subId).put(filter).toString()
    }

    private fun responseReq(subId: String, clientPub: String): String {
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_NWC_RESPONSE))
            .put("authors", JSONArray().put(walletPubkey))
            .put("#p", JSONArray().put(clientPub))
        return JSONArray().put("REQ").put(subId).put(filter).toString()
    }

    private fun newId(): String = UUID.randomUUID().toString()

    companion object {
        /** NIP-47 info events list supported schemes in an `encryption` tag; absent means NIP-04 only. */
        internal fun supportsNip44(info: Nip01Event): Boolean =
            info.tags.any { tag ->
                tag.size >= 2 && tag[0] == "encryption" && tag.drop(1).any { it.split(" ").contains("nip44_v2") }
            }

        private const val RELAY_CONNECT_TIMEOUT_MS = 15_000L
        private const val INFO_TIMEOUT_MS = 4_000L
        private const val RPC_TIMEOUT_MS = 20_000L
        private const val PAY_TIMEOUT_MS = 90_000L

        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }
}
