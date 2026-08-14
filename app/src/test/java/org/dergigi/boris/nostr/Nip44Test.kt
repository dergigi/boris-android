package org.dergigi.boris.nostr

import fr.acinq.secp256k1.Secp256k1
import org.junit.Assert.assertEquals
import org.junit.Test

class Nip44Test {
    @Test
    fun conversationKeyMatchesOfficialVector() {
        val sec1 = "0000000000000000000000000000000000000000000000000000000000000001"
            .hexToByteArray()
        val sec2 = "0000000000000000000000000000000000000000000000000000000000000002"
            .hexToByteArray()
        val pub2 = Secp256k1.pubkeyCreate(sec2).copyOfRange(1, 33).toHex()
        assertEquals(
            "c41c775356fd92eadc63ff5a0dc1da211b268cbea22316767095b2871ea1412d",
            Nip44.conversationKey(sec1, pub2).toHex(),
        )
    }

    @Test
    fun encryptDecryptRoundTripsOfficialVector() {
        val conversationKey =
            "c41c775356fd92eadc63ff5a0dc1da211b268cbea22316767095b2871ea1412d"
                .hexToByteArray()
        val nonce =
            "0000000000000000000000000000000000000000000000000000000000000001"
                .hexToByteArray()
        val payload = Nip44.encrypt("a", conversationKey, nonce)
        assertEquals(
            "AgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABee0G5VSK0/9YypIObAtDKfYEAjD35uVkHyB0F4DwrcNaCXlCWZKaArsGrY6M9wnuTMxWfp1RTN9Xga8no+kF5Vsb",
            payload,
        )
        assertEquals("a", Nip44.decrypt(payload, conversationKey))
    }
}
