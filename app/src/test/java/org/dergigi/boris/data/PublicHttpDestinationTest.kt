package org.dergigi.boris.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.InetAddress

class PublicHttpDestinationTest {
    @Test
    fun rejectsLocalPrivateAndReservedAddresses() {
        val blocked = listOf(
            "0.0.0.0",
            "10.0.0.1",
            "100.64.0.1",
            "127.0.0.1",
            "169.254.1.1",
            "172.16.0.1",
            "192.0.2.1",
            "192.168.0.1",
            "198.18.0.1",
            "198.51.100.1",
            "203.0.113.1",
            "224.0.0.1",
            "::1",
            "fc00::1",
            "fd00::1",
            "fe80::1",
            "2001:db8::1",
        )

        blocked.forEach { host ->
            assertFalse(host, PublicHttpDestination.isPublicAddress(InetAddress.getByName(host)))
        }
    }

    @Test
    fun parsesOnlyPublicHttpUrls() {
        assertNotNull(PublicHttpDestination.toHttpUrl("https://93.184.216.34/articles/one"))
        assertNull(PublicHttpDestination.toHttpUrl("ftp://93.184.216.34/articles/one"))
        assertNull(PublicHttpDestination.toHttpUrl("https://127.0.0.1/articles/one"))
        assertNull(PublicHttpDestination.toHttpUrl("https://10.0.0.1/articles/one"))
    }
}
