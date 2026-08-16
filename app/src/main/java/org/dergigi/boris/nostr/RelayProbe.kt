package org.dergigi.boris.nostr

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Checks whether a relay accepts websocket connections, without keeping the socket open. */
object RelayProbe {
    const val DEFAULT_TIMEOUT_MS = 3_000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    fun isReachable(url: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        val latch = CountDownLatch(1)
        val opened = AtomicBoolean(false)
        val socket = try {
            client.newWebSocket(
                Request.Builder().url(url).build(),
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        opened.set(true)
                        latch.countDown()
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        latch.countDown()
                    }
                },
            )
        } catch (_: Exception) {
            return false
        }
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        socket.cancel()
        return opened.get()
    }
}
