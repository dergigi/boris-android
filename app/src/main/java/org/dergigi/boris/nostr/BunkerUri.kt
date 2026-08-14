package org.dergigi.boris.nostr

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class BunkerUri(
    val remoteSignerPubkey: String,
    val relays: List<String>,
    val secret: String?,
) {
    companion object {
        fun parse(input: String): BunkerUri? {
            val trimmed = input.trim()
            if (trimmed.contains("nsec1", ignoreCase = true)) return null
            if (!trimmed.startsWith("bunker://", ignoreCase = true)) return null
            val after = trimmed.substring("bunker://".length)
            val parts = after.split("?", limit = 2)
            val remote = parts[0].lowercase()
            if (remote.length != 64 || remote.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
            if (parts.size < 2) return null
            val relays = mutableListOf<String>()
            var secret: String? = null
            for (param in parts[1].split("&")) {
                val eq = param.indexOf('=')
                if (eq <= 0) continue
                val key = param.substring(0, eq)
                val value = URLDecoder.decode(param.substring(eq + 1), StandardCharsets.UTF_8.name())
                when (key) {
                    "relay" -> if (isAllowedRelay(value) && value !in relays) relays.add(value)
                    "secret" -> if (value.isNotEmpty()) secret = value
                }
            }
            if (relays.none { it.startsWith("wss://", ignoreCase = true) }) return null
            return BunkerUri(remote, relays, secret)
        }

        internal fun isAllowedRelay(value: String): Boolean {
            if (value.startsWith("wss://", ignoreCase = true)) return true
            if (!value.startsWith("ws://", ignoreCase = true)) return false
            return isLoopbackHost(wsHost(value))
        }

        private fun wsHost(value: String): String {
            val rest = value.substring("ws://".length)
            return if (rest.startsWith("[")) {
                rest.substringAfter("[").substringBefore("]").lowercase()
            } else {
                rest.substringBefore("/").substringBefore(":").lowercase()
            }
        }

        private fun isLoopbackHost(host: String): Boolean =
            host == "127.0.0.1" || host == "localhost" || host == "::1"
    }
}
