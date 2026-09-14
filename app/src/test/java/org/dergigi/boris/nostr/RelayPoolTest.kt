package org.dergigi.boris.nostr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayPoolTest {
    @Test
    fun dropsOversizedIncomingRelayMessages() {
        assertFalse(shouldDropIncomingMessage("x".repeat(RelayPool.MAX_INCOMING_MESSAGE_CHARS)))
        assertTrue(shouldDropIncomingMessage("x".repeat(RelayPool.MAX_INCOMING_MESSAGE_CHARS + 1)))
    }

    @Test
    fun detectsLowHeapHeadroom() {
        assertFalse(
            heapHeadroomLow(
                maxMemory = 256L * 1024L * 1024L,
                totalMemory = 128L * 1024L * 1024L,
                freeMemory = 64L * 1024L * 1024L,
            ),
        )
        assertTrue(
            heapHeadroomLow(
                maxMemory = 256L * 1024L * 1024L,
                totalMemory = 252L * 1024L * 1024L,
                freeMemory = 1L * 1024L * 1024L,
            ),
        )
    }
}
