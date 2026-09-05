package org.dergigi.boris.nostr

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** A parsed `nostr+walletconnect://` connection string (NIP-47). */
data class NwcUri(
    val walletPubkey: String,
    val relays: List<String>,
    val secretHex: String,
    val lud16: String?,
) {
    /** Fresh copy each call; zero it after use. */
    fun secretBytes(): ByteArray = secretHex.hexToByteArray()

    companion object {
        private val SCHEMES = listOf("nostr+walletconnect://", "nostr+walletconnect:", "nostrwalletconnect://")

        fun parse(input: String): NwcUri? {
            val trimmed = input.trim()
            val scheme = SCHEMES.firstOrNull { trimmed.startsWith(it, ignoreCase = true) } ?: return null
            val after = trimmed.substring(scheme.length)
            val parts = after.split("?", limit = 2)
            val wallet = parts[0].lowercase()
            if (!isHex(wallet, 64)) return null
            if (parts.size < 2) return null
            val relays = mutableListOf<String>()
            var secret: String? = null
            var lud16: String? = null
            for (param in parts[1].split("&")) {
                val eq = param.indexOf('=')
                if (eq <= 0) continue
                val key = param.substring(0, eq)
                val value = URLDecoder.decode(param.substring(eq + 1), StandardCharsets.UTF_8.name())
                when (key) {
                    "relay" -> if (BunkerUri.isAllowedRelay(value) && value !in relays) relays.add(value)
                    "secret" -> secret = value.lowercase().takeIf { isHex(it, 64) }
                    "lud16" -> lud16 = LightningAddress.parse(value)
                }
            }
            if (relays.isEmpty()) return null
            return NwcUri(wallet, relays, secret ?: return null, lud16)
        }

        private fun isHex(value: String, length: Int): Boolean =
            value.length == length && value.all { it in '0'..'9' || it in 'a'..'f' }
    }
}
