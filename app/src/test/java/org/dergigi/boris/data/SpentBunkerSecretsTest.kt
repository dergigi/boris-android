package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SpentBunkerSecretsTest {
    @Test
    fun fingerprintIsStableAndDoesNotEqualTheSecret() {
        val secret = "pairing-secret"
        assertEquals(SpentBunkerSecrets.fingerprint(secret), SpentBunkerSecrets.fingerprint(secret))
        assertNotEquals(secret, SpentBunkerSecrets.fingerprint(secret))
        assertEquals(64, SpentBunkerSecrets.fingerprint(secret).length)
    }
}
