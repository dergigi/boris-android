package org.dergigi.boris.nostr

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class RelaySocket(
    private val url: String,
    private val client: OkHttpClient,
) {
    @Volatile
    var isOpen: Boolean = false
        private set

    private var socket: WebSocket? = null
    private val pending = ArrayDeque<String>()

    fun open(onOpen: () -> Unit, onMessage: (String) -> Unit) {
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isOpen = true
                    onOpen()
                    flushPending()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    onMessage(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    isOpen = false
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    isOpen = false
                }
            },
        )
    }

    fun send(text: String) {
        synchronized(pending) {
            if (isOpen) {
                socket?.send(text)
            } else {
                pending.addLast(text)
            }
        }
    }

    fun close() {
        isOpen = false
        synchronized(pending) { pending.clear() }
        socket?.close(1000, null)
        socket = null
    }

    private fun flushPending() {
        synchronized(pending) {
            while (pending.isNotEmpty()) {
                socket?.send(pending.removeFirst())
            }
        }
    }
}
