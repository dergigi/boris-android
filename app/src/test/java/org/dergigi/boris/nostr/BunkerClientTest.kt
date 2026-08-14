package org.dergigi.boris.nostr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BunkerClientTest {
    @Test
    fun alreadyConnectedIsNotAReject() {
        assertFalse(BunkerClient.rpcIsRejected("", "already connected"))
        assertFalse(BunkerClient.rpcIsRejected("", "Already Connected"))
        assertFalse(BunkerClient.rpcIsRejected("ack", ""))
    }

    @Test
    fun unauthorizedIsAReject() {
        assertTrue(BunkerClient.rpcIsRejected("", "unauthorized"))
        assertTrue(BunkerClient.rpcIsRejected("", "secret already used"))
    }
}
