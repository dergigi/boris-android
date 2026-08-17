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
    fun viewStyleDataRejectsNonArticleStrings() {
        // VIEW used to fall back to raw dataString; share already used extract.
        assertNull(UrlExtractor.extract("not a url at all"))
        assertNull(UrlExtractor.extract("javascript:alert(1)"))
        assertNull(UrlExtractor.extract("ftp://files.example.com/a"))
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
    fun detectsImageUrlsByExtension() {
        assertEquals(true, UrlExtractor.isImageUrl("https://cdn.example.com/photo.JPG?w=800"))
        assertEquals(true, UrlExtractor.isImageUrl("https://cdn.example.com/a.webp"))
        assertEquals(false, UrlExtractor.isImageUrl("https://example.com/article"))
        assertEquals(false, UrlExtractor.isImageUrl("https://example.com/file.pdf"))
    }

    @Test
    fun embedImageLinksTurnsBareAndLinkedImagesIntoMarkdown() {
        val src = """
            photo https://cdn.example.com/a.jpg.
            already ![keep](https://cdn.example.com/b.png)
            [caption](https://cdn.example.com/c.webp)
            see https://example.com/post
        """.trimIndent()
        val out = UrlExtractor.embedImageLinks(src)
        assertEquals(true, out.contains("![](https://cdn.example.com/a.jpg)."))
        assertEquals(true, out.contains("![keep](https://cdn.example.com/b.png)"))
        assertEquals(true, out.contains("![caption](https://cdn.example.com/c.webp)"))
        assertEquals(true, out.contains("see https://example.com/post"))
        assertEquals(false, out.contains("![](https://example.com/post)"))
    }

    @Test
    fun imageUrlsIncludeBareImageLinks() {
        assertEquals(
            listOf(
                "https://cdn.example.com/a.jpg",
                "https://cdn.example.com/b.png",
            ),
            UrlExtractor.imageUrls(
                "shot https://cdn.example.com/a.jpg and ![x](https://cdn.example.com/b.png)",
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

    @Test
    fun extractsNostrUriWithSlashesAndNotes() {
        val naddr =
            "naddr1qvzqqqr4gupzqwlsccluhy6xxsr6l9a9uhhxf75g85g8a709tprjcn4e42h053vaqq9x67fdv9e8g6trd3jsrnn0q2"
        assertEquals("nostr:$naddr", UrlExtractor.extract("nostr://$naddr"))
        val id = "d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f"
        val note = org.dergigi.boris.nostr.Nip19.noteEncode(id)
        assertEquals("nostr:$note", UrlExtractor.extract("Share this nostr:$note please"))
    }
}
