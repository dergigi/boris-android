package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XcancelLinkTest {
    @Test
    fun mapsXAndTwitterHosts() {
        assertEquals(
            "https://xcancel.com/jack/status/20",
            XcancelLink.copyUrl("https://x.com/jack/status/20"),
        )
        assertEquals(
            "https://xcancel.com/jack/status/20?s=20",
            XcancelLink.copyUrl("https://www.x.com/jack/status/20?s=20"),
        )
        assertEquals(
            "https://xcancel.com/jack/status/20",
            XcancelLink.copyUrl("https://twitter.com/jack/status/20"),
        )
        assertEquals(
            "https://xcancel.com/jack/status/20",
            XcancelLink.copyUrl("https://mobile.twitter.com/jack/status/20"),
        )
    }

    @Test
    fun ignoresUnrelatedUrls() {
        assertNull(XcancelLink.copyUrl("https://example.com/x.com/status/20"))
        assertNull(XcancelLink.copyUrl("https://nitter.net/jack/status/20"))
        assertNull(XcancelLink.copyUrl("https://xcancel.com/jack/status/20"))
        assertNull(XcancelLink.copyUrl("nostr:note1qqqq"))
    }
}
