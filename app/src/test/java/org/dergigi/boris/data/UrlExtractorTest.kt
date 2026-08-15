package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {
    @Test
    fun extractsBareHttpsUrl() {
        assertEquals(
            "https://example.com/article",
            UrlExtractor.extract("https://example.com/article"),
        )
    }

    @Test
    fun extractsUrlFromShareText() {
        assertEquals(
            "https://example.com/hello",
            UrlExtractor.extract("A great read\nhttps://example.com/hello"),
        )
    }

    @Test
    fun normalizesProtocolLessUrls() {
        assertEquals("https://www.example.com", UrlExtractor.normalize("www.example.com"))
    }

    @Test
    fun returnsNullWhenEmpty() {
        assertNull(UrlExtractor.extract("   "))
        assertNull(UrlExtractor.extract(null))
    }

    @Test
    fun resolvesRelativeArticleLinks() {
        assertEquals(
            "https://www.citadel21.com/other",
            UrlExtractor.articleUrl(
                "/other",
                "https://www.citadel21.com/the-paranoid-wallet",
            ),
        )
    }

    @Test
    fun keepsAbsoluteHttpLinks() {
        assertEquals(
            "https://wizardsardine.com/blog/zero-entropy",
            UrlExtractor.articleUrl("https://wizardsardine.com/blog/zero-entropy"),
        )
    }

    @Test
    fun ignoresMailtoAndAnchors() {
        assertNull(UrlExtractor.articleUrl("mailto:hi@example.com"))
        assertNull(UrlExtractor.articleUrl("#section"))
    }

    @Test
    fun extractsMarkdownImageUrls() {
        val markdown = """
            Intro
            ![one](https://example.com/a.jpg)
            ![two](/images/b.png "caption")
            ![skip](mailto:x@example.com)
        """.trimIndent()
        assertEquals(
            listOf(
                "https://example.com/a.jpg",
                "https://www.citadel21.com/images/b.png",
            ),
            UrlExtractor.imageUrls(markdown, "https://www.citadel21.com/the-paranoid-wallet"),
        )
    }

    @Test
    fun extractsHtmlImageUrls() {
        assertEquals(
            listOf("https://cdn.example.com/photo.webp"),
            UrlExtractor.imageUrls(
                """<img src="https://cdn.example.com/photo.webp" alt="photo">""",
                "https://example.com/article",
            ),
        )
    }

    @Test
    fun extractsNaddrFromShareTextAndGateways() {
        val naddr =
            "naddr1qvzqqqr4gupzqwlsccluhy6xxsr6l9a9uhhxf75g85g8a709tprjcn4e42h053vaqq9x67fdv9e8g6trd3jsrnn0q2"
        assertEquals("nostr:$naddr", UrlExtractor.extract(naddr))
        assertEquals("nostr:$naddr", UrlExtractor.extract("nostr:$naddr"))
        assertEquals("nostr:$naddr", UrlExtractor.extract("https://njump.to/$naddr"))
        assertEquals(
            "nostr:$naddr",
            UrlExtractor.articleUrl("/a/$naddr", "https://readwithboris.com/article"),
        )
    }
}
