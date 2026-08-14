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
}
