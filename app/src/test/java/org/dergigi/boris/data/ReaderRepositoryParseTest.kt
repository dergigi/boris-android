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
    }

    @Test
    fun parsesHtmlFallback() {
        val raw = "<html><head><title>Page Title</title></head><body><p>Hi</p></body></html>"
        val content = repository.parse("https://example.com", raw)
        assertEquals("Page Title", content.title)
        assertEquals(raw, content.html)
        assertNull(content.markdown)
    }
}
