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
                    "relay" -> if (value.startsWith("wss://", ignoreCase = true)) relays.add(value)
                    "secret" -> if (value.isNotEmpty()) secret = value
                }
            }
            if (relays.isEmpty()) return null
            return BunkerUri(remote, relays, secret)
        }
    }
}
