package org.dergigi.boris.nostr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NwcClientTest {
    private fun info(vararg tags: List<String>) =
        Nip01Event("id", "pub", 0, Nip01Event.KIND_NWC_INFO, tags.toList(), "pay_invoice get_balance", "sig")

    @Test
    fun readsTheEncryptionTag() {
        assertTrue(NwcClient.supportsNip44(info(listOf("encryption", "nip44_v2 nip04"))))
        assertTrue(NwcClient.supportsNip44(info(listOf("encryption", "nip44_v2"))))
        assertFalse(NwcClient.supportsNip44(info(listOf("encryption", "nip04"))))
        assertFalse(NwcClient.supportsNip44(info(listOf("notifications", "payment_received"))))
        assertFalse(NwcClient.supportsNip44(info()))
    }
}
