package org.dergigi.boris.ui.browser

import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.ui.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class InAppBrowserTest {
    @Test
    fun httpPagesStayHttp() {
        assertEquals(
            "https://example.com/article",
            InAppBrowser.targetUrl("https://example.com/article"),
        )
        assertTrue(InAppBrowser.isHttp("http://example.com"))
        assertTrue(InAppBrowser.isHttp("https://example.com"))
    }

    @Test
    fun notesOpenOnThePublicGateway() {
        val id = "d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f"
        val note = Nip19.noteEncode(id)
        assertEquals("https://njump.to/$note", InAppBrowser.targetUrl("nostr:$note"))
    }

    @Test
    fun fileAndScriptUrlsAreRejected() {
        assertNull(InAppBrowser.targetUrl("file:///sdcard/page.html"))
        assertNull(InAppBrowser.targetUrl("javascript:alert(1)"))
        assertFalse(InAppBrowser.isHttp("javascript:alert(1)"))
    }

    @Test
    fun browserRouteKeepsTheUrlAfterOneDecode() {
        val url = "https://example.com/foo?q=a%26b"
        val encoded = Routes.browser(url).substringAfter("url=")
        val decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
        assertEquals(url, decoded)
    }
}
