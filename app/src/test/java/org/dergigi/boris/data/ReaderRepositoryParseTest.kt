package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderRepositoryParseTest {
    private val repository = ReaderRepository()

    @Test
    fun parsesHtmlFallback() {
        val raw = "<html><head><title>Page Title</title></head><body><p>Hi</p></body></html>"
        val content = repository.parse("https://example.com", raw)
        assertEquals("Page Title", content.title)
        assertEquals("Hi", content.markdown)
        assertNull(content.html)
    }

    @Test
    fun htmlFallbackKeepsImagesInMarkdown() {
        val raw = """
            <html><head><title>Page Title</title></head>
            <body>
              <p>Hi</p>
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
}
