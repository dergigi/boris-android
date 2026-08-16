package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayHealthTest {
    @Test
    fun cooldownRequiresRepeatedConsecutiveFailures() {
        val url = "wss://flaky.example"
        val now = 1_000_000L
        RelayHealth.onConnectFail(url, now)
        RelayHealth.onConnectFail(url, now)
        assertFalse(RelayHealth.inCooldown(url, now))
        RelayHealth.onConnectFail(url, now)
        assertTrue(RelayHealth.inCooldown(url, now))
    }

    @Test
    fun cooldownExpiresAndSuccessResetsStreak() {
        val url = "wss://recovering.example"
        val now = 2_000_000L
        repeat(RelayHealth.COOLDOWN_FAILURES) { RelayHealth.onConnectFail(url, now) }
        assertTrue(RelayHealth.inCooldown(url, now))
        // Second chance after the cooldown window.
        assertFalse(RelayHealth.inCooldown(url, now + RelayHealth.COOLDOWN_MS))
        RelayHealth.onConnectOk(url, latencyMs = 42, now = now + RelayHealth.COOLDOWN_MS)
        assertFalse(RelayHealth.inCooldown(url, now + RelayHealth.COOLDOWN_MS + 1))
        assertEquals(0, RelayHealth.stats(url)?.consecutiveFailures)
    }

    @Test
    fun localRelaysNeverCoolDown() {
        val url = "ws://127.0.0.1:4869"
        val now = 3_000_000L
        repeat(10) { RelayHealth.onConnectFail(url, now) }
        assertFalse(RelayHealth.inCooldown(url, now))
    }

    @Test
    fun freshnessTracksRecentSuccessOnly() {
        val url = "wss://fresh.example"
        val now = 4_000_000L
        assertFalse(RelayHealth.isFresh(url, now))
        RelayHealth.onConnectOk(url, latencyMs = 10, now = now)
        assertTrue(RelayHealth.isFresh(url, now + 1))
        assertFalse(RelayHealth.isFresh(url, now + RelayHealth.FRESH_MS + 1))
        RelayHealth.onConnectFail(url, now + 2)
        assertFalse(RelayHealth.isFresh(url, now + 3))
    }
}
