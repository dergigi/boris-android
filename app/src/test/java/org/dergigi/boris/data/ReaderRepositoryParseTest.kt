package org.dergigi.boris.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ReaderRepositoryParseTest {
    private val repository = ReaderRepository()

    private val longBody = "The article paragraph carries the real story. ".repeat(15)

    @Test
    fun parsesHtmlFallback() {
        val raw = "<html><head><title>Page Title</title></head><body><p>$longBody</p></body></html>"
        val content = repository.parse("https://example.com", raw)
        assertEquals("Page Title", content.title)
        assertTrue(content.markdown!!.contains("The article paragraph carries the real story."))
        assertNull(content.html)
    }

    @Test
    fun htmlFallbackKeepsImagesInMarkdown() {
        val raw = """
            <html><head><title>Page Title</title></head>
            <body>
              <p>$longBody</p>
              <img src="/shots/a.jpg" alt="shot">
              <img src="http://cdn.example.com/b.png">
            </body></html>
        """.trimIndent()
        val content = repository.parse("https://example.com/post", raw)
        assertEquals("Page Title", content.title)
        assertEquals(true, content.markdown!!.contains("![shot](https://example.com/shots/a.jpg)"))
        assertEquals(true, content.markdown!!.contains("![](https://cdn.example.com/b.png)"))
        assertEquals(
            listOf(
                "https://example.com/shots/a.jpg",
                "https://cdn.example.com/b.png",
            ),
            UrlExtractor.imageUrls(content.body, content.url),
        )
    }

    @Test
    fun thinHtmlBodyYieldsNullMarkdown() {
        val raw = "<html><head><title>Page Title</title></head><body><p>Hi</p></body></html>"
        val content = repository.parse("https://example.com", raw)
        assertEquals("Page Title", content.title)
        assertNull(content.markdown)
    }

    @Test
    fun paywallTeaserYieldsNullMarkdown() {
        val teaser = "Subscribe now to keep reading this exclusive story from our newsroom."
        val raw = "<html><body><article><p>$teaser</p></article></body></html>"
        assertNull(repository.parse("https://example.com/post", raw).markdown)
    }

    @Test
    fun parseSurvivesInvalidNumericEntities() {
        val raw = """
            <html><body>
            <article><p>$longBody &#1114112; leftover &#xD800;</p></article>
            </body></html>
        """.trimIndent()
        val markdown = repository.parse("https://example.com/post", raw).markdown
        assertNotNull(markdown)
        assertTrue(markdown!!.contains("The article paragraph carries the real story."))
    }

    @Test
    fun parseStripsChromeFromWrappedArticle() {
        val body = "The article paragraph carries the real story. ".repeat(15)
        val raw = """
            <html><head><title>Page Title</title></head><body>
            <nav>Home</nav>
            <article><p>$body</p></article>
            <div id="comments">nope</div>
            </body></html>
        """.trimIndent()
        val content = repository.parse("https://example.com/post", raw)
        val markdown = content.markdown!!
        assertTrue(markdown.contains("The article paragraph carries the real story."))
        assertFalse(markdown.contains("Home"))
        assertFalse(markdown.contains("nope"))
    }

    @Test
    fun parseReadsNip21AuthorAndAlternate() {
        val npub = "npub1dergggklka99wwrs92yz8wdjs952h2ux2ha2ed598ngwu9w7a6fsh9xzpc"
        val naddr =
            "naddr1qvzqqqr4gupzqwlsccluhy6xxsr6l9a9uhhxf75g85g8a709tprjcn4e42h053vaqq9x67fdv9e8g6trd3jsrnn0q2"
        val raw = """
            <html><head>
            <title>Page Title</title>
            <link rel="author" href="nostr:$npub" />
            <link rel="alternate" href="nostr:$naddr" />
            </head><body><article><p>$longBody</p></article></body></html>
        """.trimIndent()
        val content = repository.parse("https://dergigi.com/post", raw)
        val hex = org.dergigi.boris.nostr.Nip19.npubDecode(npub)
        assertEquals(hex, content.authorPubkey)
        assertEquals(NostrArticle.parse(naddr)!!.coordinate, content.articleCoordinate)
    }

    @Test
    fun parseIgnoresPagesWithoutNip21Tags() {
        val raw = "<html><head><title>Page Title</title></head><body><article><p>$longBody</p></article></body></html>"
        val content = repository.parse("https://example.com", raw)
        assertNull(content.authorPubkey)
        assertNull(content.articleCoordinate)
    }

    @Test
    fun noteMarkdownEmbedsImageLinksAndKeepsLineBreaks() {
        val out = repository.noteMarkdown("hello\nhttps://cdn.example.com/shot.jpg")
        assertEquals("hello  \n![](https://cdn.example.com/shot.jpg)", out)
    }

    @Test
    fun fetchMapsThinExtractToNoArticleAfterUaRetry() {
        val agents = mutableListOf<String>()
        val client = stubClient { request ->
            agents += request.header("User-Agent").orEmpty()
            stubResponse(request, 200, "<html><body><p>Hi</p></body></html>")
        }
        val error = fetchError(client, "https://example.com/thin")
        assertEquals("Could not find an article on this page.", error?.message)
        assertEquals("No readable article in the page", (error as? ReaderFetchException)?.detail)
        assertEquals(listOf(HttpUserAgents.BORIS_UA, HttpUserAgents.BROWSER_UA), agents)
    }

    @Test
    fun fetchMapsLiveFailWithoutCacheToUnreachable() {
        val client = stubClient { throw IOException("connect timed out") }
        val error = fetchError(client, "https://example.com/gone")
        assertEquals("Could not reach this page.", error?.message)
        assertTrue((error as? ReaderFetchException)?.detail.orEmpty().contains("connect timed out"))
    }

    @Test
    fun fetchKeepsHttpStatusInUnreachableDetail() {
        val client = stubClient { request -> stubResponse(request, 502, "") }
        val error = fetchError(client, "https://example.com/bad-gateway")
        assertEquals("Could not reach this page.", error?.message)
        assertEquals("HTTP 502", (error as? ReaderFetchException)?.detail)
    }

    @Test
    fun fetchRetriesWithBrowserUaWhenBlocked() {
        val agents = mutableListOf<String>()
        val page = """
            <html><head><title>Page Title</title>
            <meta property="og:image" content="https://example.com/hero.png">
            <meta property="og:description" content="A short lede">
            </head><body><article><p>$longBody</p></article></body></html>
        """.trimIndent()
        val client = stubClient { request ->
            agents += request.header("User-Agent").orEmpty()
            if (agents.size == 1) stubResponse(request, 403, "") else stubResponse(request, 200, page)
        }
        val content = ReaderRepository(client).fetch("https://example.com/blocked")
        assertTrue(content.markdown!!.contains("The article paragraph carries the real story."))
        assertEquals(listOf(HttpUserAgents.BORIS_UA, HttpUserAgents.BROWSER_UA), agents)
    }

    @Test
    fun fetchFollowsHtmlForwardBeforeParsing() {
        val requested = mutableListOf<String>()
        val target = "https://dergigi.com/2022/04/03/inalienable-property-rights/"
        val page = """
            <html><head><title>Target Article</title></head>
            <body><article><p>$longBody</p></article></body></html>
        """.trimIndent()
        val client = stubClient { request ->
            requested += request.url.toString()
            if (request.url.toString() == target) {
                stubResponse(request, 200, page)
            } else {
                stubResponse(request, 200, forwardPage(meta = target, script = target, canonical = target))
            }
        }
        val content = ReaderRepository(client).fetch("https://dergigi.com/speech")
        assertEquals(target, content.url)
        assertEquals("Target Article", content.title)
        assertTrue(content.markdown!!.contains("The article paragraph carries the real story."))
        assertEquals(listOf("https://dergigi.com/speech", target), requested)
    }

    @Test
    fun htmlForwardTargetSupportsCanonicalOnlyRedirectPages() {
        assertEquals(
            "https://example.com/target",
            repository.htmlForwardTarget(
                "https://example.com/start",
                forwardPage(canonical = "https://example.com/target"),
            ),
        )
    }

    @Test
    fun htmlForwardTargetSupportsJavascriptOnlyRedirectPages() {
        assertEquals(
            "https://example.com/target",
            repository.htmlForwardTarget(
                "https://example.com/start",
                forwardPage(script = "https://example.com/target"),
            ),
        )
    }

    @Test
    fun htmlForwardTargetResolvesRelativeMetaRefreshTargets() {
        assertEquals(
            "https://example.com/target",
            repository.htmlForwardTarget(
                "https://example.com/start",
                forwardPage(meta = "/target"),
            ),
        )
    }

    @Test
    fun htmlForwardTargetRejectsNonHttpTargets() {
        assertNull(
            repository.htmlForwardTarget(
                "https://example.com/start",
                forwardPage(meta = "javascript:alert(1)"),
            ),
        )
    }

    @Test
    fun htmlForwardTargetKeepsCaseSensitivePathsDistinct() {
        assertEquals(
            "https://example.com/post?Ref=A",
            repository.htmlForwardTarget(
                "https://example.com/Post?Ref=A",
                forwardPage(meta = "https://example.com/post?Ref=A"),
            ),
        )
    }

    @Test
    fun htmlForwardTargetKeepsTrailingSlashPathsDistinct() {
        assertEquals(
            "https://example.com/post/",
            repository.htmlForwardTarget(
                "https://example.com/post",
                forwardPage(meta = "https://example.com/post/"),
            ),
        )
    }

    @Test
    fun htmlForwardTargetRejectsDelayedMetaRefreshes() {
        assertNull(
            repository.htmlForwardTarget(
                "https://example.com/post",
                forwardPage(meta = "https://example.com/next", delaySeconds = 300),
            ),
        )
    }

    @Test
    fun fetchStopsHtmlForwardLoops() {
        val client = stubClient { request ->
            if (request.cacheControl.onlyIfCached) return@stubClient stubResponse(request, 504, "")
            val target = if (request.url.encodedPath == "/one") {
                "https://example.com/two"
            } else {
                "https://example.com/one"
            }
            stubResponse(request, 200, forwardPage(meta = target))
        }
        val error = fetchError(client, "https://example.com/one")
        assertEquals("Could not reach this page.", error?.message)
        assertEquals("Redirect loop", (error as? ReaderFetchException)?.detail)
    }

    @Test
    fun fetchStopsAfterFiveHtmlForwards() {
        val requested = mutableListOf<String>()
        val client = stubClient { request ->
            if (request.cacheControl.onlyIfCached) return@stubClient stubResponse(request, 504, "")
            requested += request.url.encodedPath
            val step = request.url.encodedPath.removePrefix("/").toInt()
            stubResponse(request, 200, forwardPage(meta = "https://example.com/${step + 1}"))
        }
        val error = fetchError(client, "https://example.com/0")
        assertEquals("Could not reach this page.", error?.message)
        assertEquals("Too many redirects", (error as? ReaderFetchException)?.detail)
        assertEquals(listOf("/0", "/1", "/2", "/3", "/4", "/5"), requested)
    }

    @Test
    fun fetchResolvesRelativeHtmlForwardsFromFinalResponseUrl() {
        val target = "https://example.com/redirected/article"
        val page = """
            <html><head><title>Redirect Target</title></head>
            <body><article><p>$longBody</p></article></body></html>
        """.trimIndent()
        val client = stubClient { request ->
            when (request.url.toString()) {
                "https://example.com/short" -> stubResponse(
                    request,
                    200,
                    forwardPage(meta = "article"),
                    finalUrl = "https://example.com/redirected/page",
                )
                target -> stubResponse(request, 200, page)
                else -> stubResponse(request, 404, "")
            }
        }
        val content = ReaderRepository(client).fetch("https://example.com/short")
        assertEquals(target, content.url)
        assertTrue(content.markdown!!.contains("The article paragraph carries the real story."))
    }

    @Test
    fun fetchParsesFinalResponseUrlAfterHttpRedirects() {
        val finalUrl = "https://example.com/final/page"
        val page = """
            <html><head><title>Final Article</title></head>
            <body>
              <article>
                <p>$longBody</p>
                <img src="images/cover.jpg" alt="cover">
              </article>
            </body></html>
        """.trimIndent()
        val client = stubClient { request ->
            stubResponse(request, 200, page, finalUrl = finalUrl)
        }
        val content = ReaderRepository(client).fetch("https://example.com/short")
        assertEquals(finalUrl, content.url)
        assertEquals(
            listOf("https://example.com/final/images/cover.jpg"),
            UrlExtractor.imageUrls(content.body, content.url),
        )
    }

    private fun fetchError(client: OkHttpClient, url: String): IOException? = try {
        ReaderRepository(client).fetch(url)
        null
    } catch (e: IOException) {
        e
    }

    private fun stubClient(handler: (Request) -> Response): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain -> handler(chain.request()) }
            .build()

    private fun forwardPage(
        meta: String? = null,
        script: String? = null,
        canonical: String? = null,
        delaySeconds: Int = 0,
    ): String = """
        <!DOCTYPE html>
        <html lang="en-US">
          <title>Redirecting&hellip;</title>
          ${canonical?.let { "<link rel=\"canonical\" href=\"$it\">" }.orEmpty()}
          ${script?.let { "<script>location=\"$it\"</script>" }.orEmpty()}
          ${meta?.let { "<meta http-equiv=\"refresh\" content=\"$delaySeconds; url=$it\">" }.orEmpty()}
          <h1>Redirecting&hellip;</h1>
          <a href="${meta ?: script ?: canonical.orEmpty()}">Click here if you are not redirected.</a>
        </html>
    """.trimIndent()

    private fun stubResponse(
        request: Request,
        code: Int,
        body: String,
        finalUrl: String? = null,
    ): Response =
        Response.Builder()
            .request(finalUrl?.let { request.newBuilder().url(it).build() } ?: request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("stub")
            .body(body.toResponseBody("text/html; charset=utf-8".toMediaType()))
            .build()
}
