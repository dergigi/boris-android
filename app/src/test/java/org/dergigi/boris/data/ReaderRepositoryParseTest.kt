package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderRepositoryParseTest {
    private val repository = ReaderRepository()

    @Test
    fun parsesJinaMarkdownPayload() {
        val raw = """
            Title: Hello World
            URL Source: https://example.com/hello
            Markdown Content:
            # Hello

            Body text.
        """.trimIndent()

        val content = repository.parse("https://example.com/hello", raw)
        assertEquals("Hello World", content.title)
        assertEquals("# Hello\n\nBody text.", content.markdown)
        assertNull(content.html)
        assertNull(content.publishedAt)
        assertNull(content.imageUrl)
    }

    @Test
    fun parsesPublishedTimeFromJinaHeader() {
        val raw = """
            Title: Hello World
            URL Source: https://example.com/hello
            Published Time: 2024-01-15T10:00:00Z
            Markdown Content:
            # Hello
        """.trimIndent()
        val content = repository.parse("https://example.com/hello", raw)
        assertEquals(1_705_312_800L, content.publishedAt)
    }

    @Test
    fun parsesHtmlFallback() {
        val raw = "<html><head><title>Page Title</title></head><body><p>Hi</p></body></html>"
        val content = repository.parse("https://example.com", raw)
        assertEquals("Page Title", content.title)
        assertEquals(raw, content.html)
        assertNull(content.markdown)
        assertNull(content.imageUrl)
    }

    @Test
    fun parsesCoverFromJinaHeaderAndStripsTheLeadImage() {
        val raw = """
            Title: Hello World
            URL Source: https://example.com/hello
            Image URL: https://cdn.example.com/cover.jpg
            Description: A short lede
            Markdown Content:
            ![](https://cdn.example.com/cover.jpg)

            Body text.
        """.trimIndent()
        val content = repository.parse("https://example.com/hello", raw)
        assertEquals("https://cdn.example.com/cover.jpg", content.imageUrl)
        assertEquals("A short lede", content.summary)
        assertEquals("Body text.", content.markdown)
    }

    @Test
    fun noteMarkdownEmbedsImageLinksAndKeepsLineBreaks() {
        val out = repository.noteMarkdown("hello\nhttps://cdn.example.com/shot.jpg")
        assertEquals("hello  \n![](https://cdn.example.com/shot.jpg)", out)
    }
}
