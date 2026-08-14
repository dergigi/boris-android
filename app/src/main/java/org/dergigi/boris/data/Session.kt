package org.dergigi.boris.data

sealed interface Session {
    val pubkeyHex: String

    data class Amber(
        override val pubkeyHex: String,
        val signerPackage: String,
    ) : Session

    data class Bunker(
        override val pubkeyHex: String,
        val remoteSignerPubkey: String,
        val relays: List<String>,
        val clientPrivkeyCiphertext: String,
        val bunkerSecretCiphertext: String?,
    ) : Session

    companion object {
        fun fromStored(hex: String?, pkg: String?): Amber? {
            val h = hex?.trim()?.lowercase().orEmpty()
            val p = pkg?.trim().orEmpty()
            if (!isHex64(h)) return null
            if (p.isEmpty()) return null
            return Amber(h, p)
        }

        fun fromStoredBunker(
            hex: String?,
            remote: String?,
            relaysCsv: String?,
            clientCipher: String?,
            secretCipher: String?,
        ): Bunker? {
            val h = hex?.trim()?.lowercase().orEmpty()
            val r = remote?.trim()?.lowercase().orEmpty()
            if (!isHex64(h) || !isHex64(r)) return null
            val relays = relaysCsv.orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it.startsWith("wss://", ignoreCase = true) }
            if (relays.isEmpty()) return null
            val cipher = clientCipher?.trim().orEmpty()
            if (cipher.isEmpty()) return null
            val secret = secretCipher?.trim()?.takeIf { it.isNotEmpty() }
            return Bunker(h, r, relays, cipher, secret)
        }

        private fun isHex64(value: String): Boolean =
            value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' }
    }
}
