package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OgMetaTest {
    @Test
    fun parsePrefersOgTitleAndImage() {
        val html = """
            <html><head>
            <title>Tab title</title>
            <meta property="og:title" content="Understanding is the new bottleneck">
            <meta property="og:image" content="https://cdn.example/cover.jpg">
            <meta property="og:description" content="A lede from the page">
            <meta property="og:site_name" content="geoffreylitt.com">
            </head></html>
        """.trimIndent()
        val preview = OgMeta.parse(html, "https://www.geoffreylitt.com/post")
        assertEquals("Understanding is the new bottleneck", preview.title)
        assertEquals("https://cdn.example/cover.jpg", preview.imageUrl)
        assertEquals("geoffreylitt.com", preview.siteName)
        assertEquals("A lede from the page", preview.description)
    }

    @Test
    fun parseReadsReversedMetaAndRelativeImage() {
        val html = """
            <meta content="The Paranoid Wallet" property="og:title">
            <meta content="/cover.png" name="twitter:image">
        """.trimIndent()
        val preview = OgMeta.parse(html, "https://www.citadel21.com/the-paranoid-wallet")
        assertEquals("The Paranoid Wallet", preview.title)
        assertEquals("https://www.citadel21.com/cover.png", preview.imageUrl)
    }

    @Test
    fun parseFallsBackToHtmlTitle() {
        val html = "<html><head><title>  Page Title  </title></head></html>"
        val preview = OgMeta.parse(html, "https://example.com")
        assertEquals("Page Title", preview.title)
        assertNull(preview.imageUrl)
        assertNull(preview.siteName)
    }

    @Test
    fun parseUnescapesEntitiesAndProtocolRelativeImage() {
        val html = """
            <meta property="og:title" content="Foo &amp; Bar">
            <meta property="og:image" content="//cdn.example/a.png">
        """.trimIndent()
        val preview = OgMeta.parse(html, "https://example.com/post")
        assertEquals("Foo & Bar", preview.title)
        assertEquals("https://cdn.example/a.png", preview.imageUrl)
    }
}
