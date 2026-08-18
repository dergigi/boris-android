package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlToMarkdownTest {
    @Test
    fun convertsParagraphsAndFormatting() {
        val markdown = HtmlToMarkdown.convert(
            "<p>First <strong>bold</strong> and <em>italic</em>.</p><p>Second.</p>",
        )
        assertEquals("First **bold** and *italic*.\n\nSecond.", markdown)
    }

    @Test
    fun convertsLinksAndImages() {
        val markdown = HtmlToMarkdown.convert(
            """<p>See <a href="https://example.com/a">this post</a>.</p>""" +
                """<img src="https://example.com/pic.jpg" alt="A picture">""",
        )
        assertTrue(markdown.contains("[this post](https://example.com/a)"))
        assertTrue(markdown.contains("![A picture](https://example.com/pic.jpg)"))
    }

    @Test
    fun resolvesRelativeImagesAgainstBaseUrl() {
        val markdown = HtmlToMarkdown.convert(
            """<img src="/img/a.jpg" alt="a"><img src="http://cdn.example.com/b.png">""",
            "https://example.com/post",
        )
        assertTrue(markdown.contains("![a](https://example.com/img/a.jpg)"))
        assertTrue(markdown.contains("![](https://cdn.example.com/b.png)"))
    }

    @Test
    fun stripsDocumentHead() {
        val markdown = HtmlToMarkdown.convert(
            "<html><head><title>Page Title</title></head><body><p>Hi</p></body></html>",
        )
        assertEquals("Hi", markdown)
    }

    @Test
    fun convertsHeadingsAndLists() {
        val markdown = HtmlToMarkdown.convert(
            "<h2>Section</h2><ul><li>one</li><li>two</li></ul>",
        )
        assertTrue(markdown.contains("## Section"))
        assertTrue(markdown.contains("- one"))
        assertTrue(markdown.contains("- two"))
    }

    @Test
    fun convertsBlockquotes() {
        val markdown = HtmlToMarkdown.convert(
            "<blockquote><p>Wise words.</p></blockquote>",
        )
        assertEquals("> Wise words.", markdown)
    }

    @Test
    fun preservesCodeBlocksAndInlineCode() {
        val markdown = HtmlToMarkdown.convert(
            "<pre><code>val x = 1 &lt; 2</code></pre><p>Use <code>foo()</code>.</p>",
        )
        assertTrue(markdown.contains("```\nval x = 1 < 2\n```"))
        assertTrue(markdown.contains("`foo()`"))
    }

    @Test
    fun decodesEntitiesAndStripsScripts() {
        val markdown = HtmlToMarkdown.convert(
            "<script>alert(1)</script><p>Fish &amp; chips &#8211; caf&#xE9;</p>",
        )
        assertEquals("Fish & chips – café", markdown)
    }

    @Test
    fun anchorAroundImageKeepsImage() {
        val markdown = HtmlToMarkdown.convert(
            """<a href="https://example.com"><img src="https://example.com/i.png" alt="x"></a>""",
        )
        assertTrue(markdown.contains("![x](https://example.com/i.png)"))
    }
}
