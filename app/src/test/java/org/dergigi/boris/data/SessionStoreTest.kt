package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionStoreTest {
    @Test
    fun fromStoredAcceptsValidPair() {
        val session = Session.fromStored(
            "3BF0C63FCB93463407AF97A5E5EE64FA883D107EF9E558472C4EB9AAAEFA459D",
            "com.greenart7c3.nostrsigner",
        )
        assertEquals(
            "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            session?.pubkeyHex,
        )
        assertEquals("com.greenart7c3.nostrsigner", session?.signerPackage)
    }

    @Test
    fun fromStoredRejectsBlankHex() {
        assertNull(Session.fromStored("", "com.greenart7c3.nostrsigner"))
        assertNull(Session.fromStored(null, "com.greenart7c3.nostrsigner"))
        assertNull(Session.fromStored("not-hex", "com.greenart7c3.nostrsigner"))
    }

    @Test
    fun fromStoredRejectsBlankPackage() {
        assertNull(
            Session.fromStored(
                "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
                "  ",
            ),
        )
    }

    @Test
    fun fromStoredBunkerAcceptsHexRelaysAndWrappedClientCiphertext() {
        val session = Session.fromStoredBunker(
            "3BF0C63FCB93463407AF97A5E5EE64FA883D107EF9E558472C4EB9AAAEFA459D",
            "7E7E9C42A91BFEF19FA929E5FDA1B72E0EBC1A4C1141673E2794234D86ADDF4E",
            "wss://relay.one,wss://relay.two",
            "iv.ciphertext",
            null,
        )
        assertEquals(
            "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            session?.pubkeyHex,
        )
        assertEquals(
            "7e7e9c42a91bfef19fa929e5fda1b72e0ebc1a4c1141673e2794234d86addf4e",
            session?.remoteSignerPubkey,
        )
        assertEquals(listOf("wss://relay.one", "wss://relay.two"), session?.relays)
        assertEquals("iv.ciphertext", session?.clientPrivkeyCiphertext)
        assertNull(session?.bunkerSecretCiphertext)
    }

    @Test
    fun fromStoredBunkerRejectsBlankClientCiphertext() {
        assertNull(
            Session.fromStoredBunker(
                "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
                "7e7e9c42a91bfef19fa929e5fda1b72e0ebc1a4c1141673e2794234d86addf4e",
                "wss://relay.one",
                "  ",
                null,
            ),
        )
    }

    @Test
    fun fromStoredLoadsAmberWhenKindAbsentAndSignerPackagePresent() {
        val session = Session.fromStored(
            "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            "com.greenart7c3.nostrsigner",
        )
        assertEquals("com.greenart7c3.nostrsigner", session?.signerPackage)
    }

    @Test
    fun saveOfOneKindClearsTheOtherKeys() {
        assertEquals(
            setOf(
                SessionStore.KEY_REMOTE_SIGNER_PUBKEY,
                SessionStore.KEY_RELAYS,
                SessionStore.KEY_CLIENT_PRIVKEY,
                SessionStore.KEY_BUNKER_SECRET,
            ),
            SessionStore.keysClearedWhenSaving(SessionStore.KIND_AMBER),
        )
        assertEquals(
            setOf(SessionStore.KEY_SIGNER_PACKAGE),
            SessionStore.keysClearedWhenSaving(SessionStore.KIND_BUNKER),
        )
    }
}
