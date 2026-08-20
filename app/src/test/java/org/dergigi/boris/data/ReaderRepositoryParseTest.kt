package org.dergigi.boris.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals(listOf(HttpUserAgents.BORIS_UA, HttpUserAgents.BROWSER_UA), agents)
    }

    @Test
    fun fetchMapsLiveFailWithoutCacheToUnreachable() {
        val client = stubClient { throw IOException("connect timed out") }
        val error = fetchError(client, "https://example.com/gone")
        assertEquals("Could not reach this page.", error?.message)
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

    private fun stubResponse(request: Request, code: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("stub")
            .body(body.toResponseBody("text/html; charset=utf-8".toMediaType()))
            .build()
}
