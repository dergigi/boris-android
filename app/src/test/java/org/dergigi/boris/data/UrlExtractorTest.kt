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
    fun normalizeDropsTextFragment() {
        assertEquals(
            "https://example.com/post",
            UrlExtractor.normalize("https://example.com/post#:~:text=hello world"),
        )
    }

    @Test
    fun preferHttpsUpgradesCleartextImageHosts() {
        assertEquals(
            "https://cdn.example.com/a.jpg",
            UrlExtractor.preferHttps("http://cdn.example.com/a.jpg"),
        )
        assertEquals(
            "https://cdn.example.com/a.jpg",
            UrlExtractor.preferHttps("HTTP://cdn.example.com/a.jpg"),
        )
        assertEquals(
            "https://cdn.example.com/a.jpg",
            UrlExtractor.preferHttps("https://cdn.example.com/a.jpg"),
        )
    }

    @Test
    fun imageUrlsUpgradeHttpToHttps() {
        assertEquals(
            listOf("https://cdn.example.com/a.jpg"),
            UrlExtractor.imageUrls("![x](http://cdn.example.com/a.jpg)"),
        )
    }

    @Test
    fun upgradeImageHttpUrlsRewritesMarkdownAndHtml() {
        assertEquals(
            "![x](https://cdn.example.com/a.jpg)",
            UrlExtractor.upgradeImageHttpUrls("![x](http://cdn.example.com/a.jpg)"),
        )
        assertEquals(
            """<img src="https://cdn.example.com/a.jpg">""",
            UrlExtractor.upgradeImageHttpUrls("""<img src="http://cdn.example.com/a.jpg">"""),
        )
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
        assertEquals(true, UrlExtractor.isImageUrl("https://image.nostr.build/abc.jpg"))
        assertEquals(true, UrlExtractor.isImageUrl("https://image.nostr.build/abc.gif"))
        assertEquals(false, UrlExtractor.isImageUrl("https://example.com/article"))
        assertEquals(false, UrlExtractor.isImageUrl("https://example.com/file.pdf"))
        assertEquals(
            true,
            UrlExtractor.isImageUrl(
                "https://relay.dergigi.com/03e174145e1f410772bb8c3e79b153ac0077fe482d7006b1f0ed67a81d475bb9.png",
            ),
        )
    }

    @Test
    fun detectsImageContentTypes() {
        assertEquals(true, UrlExtractor.isImageContentType("image/png"))
        assertEquals(true, UrlExtractor.isImageContentType("image/jpeg; charset=binary"))
        assertEquals(false, UrlExtractor.isImageContentType("text/html"))
        assertEquals(false, UrlExtractor.isImageContentType(null))
    }

    @Test
    fun embedImageLinksTurnsBareUrlsIntoMarkdownImages() {
        val src = """
            photo https://cdn.example.com/a.jpg.
            already ![keep](https://cdn.example.com/b.png)
            [caption](https://cdn.example.com/c.webp)
            see https://example.com/post
        """.trimIndent()
        val out = UrlExtractor.embedImageLinks(src)
        assertEquals(true, out.contains("![](https://cdn.example.com/a.jpg)."))
        assertEquals(true, out.contains("![keep](https://cdn.example.com/b.png)"))
        assertEquals(true, out.contains("[caption](https://cdn.example.com/c.webp)"))
        assertEquals(false, out.contains("![caption](https://cdn.example.com/c.webp)"))
        assertEquals(true, out.contains("see https://example.com/post"))
        assertEquals(false, out.contains("![](https://example.com/post)"))
    }

    @Test
    fun embedImageLinksLeavesMarkdownLinksToImagesAsLinks() {
        val src = """
            I am the Grug-brained developer. [The Grug Brained Developer](https://image.nostr.build/grug.png)

            [chat](https://cdn.example.com/shot.webp)
        """.trimIndent()
        val out = UrlExtractor.embedImageLinks(src)
        assertEquals(true, out.contains("[The Grug Brained Developer](https://image.nostr.build/grug.png)"))
        assertEquals(true, out.contains("[chat](https://cdn.example.com/shot.webp)"))
        assertEquals(false, out.contains("![The Grug Brained Developer]"))
        assertEquals(false, out.contains("![chat]"))
    }

    @Test
    fun embedImageLinksTurnsBareNostrBuildUrlsIntoImages() {
        val src = """
            intro

            https://image.nostr.build/3109eed06d708cfad1ec0c8f4a823b8d6c58b73d3a7f73a94cdcafe22554656f.jpg

            outro
        """.trimIndent()
        val out = UrlExtractor.embedImageLinks(src)
        assertEquals(
            true,
            out.contains("![](https://image.nostr.build/3109eed06d708cfad1ec0c8f4a823b8d6c58b73d3a7f73a94cdcafe22554656f.jpg)"),
        )
        assertEquals(false, out.contains("\nhttps://image.nostr.build/"))
    }

    @Test
    fun embedImageLinksLeavesImageUrlsInsideCodeFences() {
        val src = """
            ```
            https://image.nostr.build/abc.jpg
            ```
            `https://cdn.example.com/a.jpg`
        """.trimIndent()
        val out = UrlExtractor.embedImageLinks(src)
        assertEquals(true, out.contains("```\nhttps://image.nostr.build/abc.jpg\n```"))
        assertEquals(true, out.contains("`https://cdn.example.com/a.jpg`"))
        assertEquals(false, out.contains("![]("))
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

    @Test
    fun profileUrisAreNotArticles() {
        val nprofile =
            "nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p"
        assertNull(UrlExtractor.articleUrl("nostr:$nprofile"))
        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        assertNull(UrlExtractor.articleUrl("nostr:$npub"))
        assertNull(UrlExtractor.articleUrl("https://njump.to/$npub"))
    }

    @Test
    fun extractsProfileFromShareTextAndGateways() {
        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        assertEquals("nostr:$npub", UrlExtractor.extract("nostr:$npub"))
        assertEquals("nostr:$npub", UrlExtractor.extract(npub))
        assertEquals("nostr:$npub", UrlExtractor.extract("https://njump.to/$npub"))
        val nprofile =
            "nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p"
        assertEquals("nostr:$nprofile", UrlExtractor.extract("nostr:$nprofile"))
        assertEquals("nostr:$nprofile", UrlExtractor.extract(nprofile))
        assertNull(
            UrlExtractor.extract("nostr:nsec1vl029mgpspedva04g90vltkh6fvh240zqtv9k0t9af8935ke9laqsnlfe5"),
        )
    }

    @Test
    fun httpUrlWinsOverBareNpubInShareText() {
        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        assertEquals(
            "https://example.com/article",
            UrlExtractor.extract("see $npub and https://example.com/article"),
        )
    }

    @Test
    fun issue5ReproNaddrStillExtracts() {
        val naddr =
            "naddr1qqwhwmmjw35xcetnwvkk6mmwv4uj6arfd4jkcetnwvkkzun595pzq634npfz8rwfq2hdr8am76s9t7dt7gwpe2y3t5wyufl4phe09yxeqvzqqqr4gu7cgak5"
        assertEquals("nostr:$naddr", UrlExtractor.extract("nostr:$naddr"))
    }
}
