package org.dergigi.boris.nostr

import fr.acinq.secp256k1.Secp256k1
import java.security.SecureRandom

data class ClientKeypair(
    val privkey: ByteArray,
    val pubkeyHex: String,
) {
    companion object {
        fun generate(): ClientKeypair {
            val priv = ByteArray(32)
            val rng = SecureRandom()
            do {
                rng.nextBytes(priv)
            } while (!Secp256k1.secKeyVerify(priv))
            val pub = Secp256k1.pubkeyCreate(priv)
            val xOnly = pub.copyOfRange(1, 33)
            return ClientKeypair(priv, xOnly.toHex())
        }

        fun fromPrivkey(priv: ByteArray): ClientKeypair? {
            if (priv.size != 32 || !Secp256k1.secKeyVerify(priv)) return null
            val pub = Secp256k1.pubkeyCreate(priv)
            return ClientKeypair(priv.copyOf(), pub.copyOfRange(1, 33).toHex())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClientKeypair) return false
        return privkey.contentEquals(other.privkey) && pubkeyHex == other.pubkeyHex
    }

    override fun hashCode(): Int = 31 * privkey.contentHashCode() + pubkeyHex.hashCode()
}
