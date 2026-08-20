package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun convertsTablesToGfm() {
        val markdown = HtmlToMarkdown.convert(
            "<table><thead><tr><th>Name</th><th>Age</th></tr></thead>" +
                "<tbody><tr><td>Ada</td><td>36</td></tr></tbody></table>",
        )
        assertEquals("| Name | Age |\n| --- | --- |\n| Ada | 36 |", markdown)
    }

    @Test
    fun numbersOrderedListItems() {
        val markdown = HtmlToMarkdown.convert(
            "<ol><li>first</li><li>second</li></ol><ul><li>bullet</li></ul>",
        )
        assertTrue(markdown.contains("1. first"))
        assertTrue(markdown.contains("2. second"))
        assertTrue(markdown.contains("- bullet"))
    }

    @Test
    fun convertsFootnotePairsThatFootnotesExpands() {
        val markdown = HtmlToMarkdown.convert(
            """<p>Claim<sup id="fnref1"><a href="#fn1">1</a></sup>.</p>""" +
                """<div class="footnotes"><ol><li id="fn1">The source text.</li></ol></div>""",
        )
        assertTrue(markdown.contains("Claim[^1]."))
        assertTrue(markdown.contains("[^1]: The source text."))
        val expanded = Footnotes.expand(markdown)
        assertTrue(expanded.contains("Claim¹."))
        assertTrue(expanded.contains("1. The source text."))
    }

    @Test
    fun prependsObviousBylineFromAuthorMeta() {
        val markdown = HtmlToMarkdown.convert(
            """<html><head><meta name="author" content="Jane Doe"></head>""" +
                "<body><p>Body text.</p></body></html>",
        )
        assertEquals("*Jane Doe*\n\nBody text.", markdown)
    }

    @Test
    fun prettyPrintedHtmlDoesNotBecomeIndentedCode() {
        val markdown = HtmlToMarkdown.convert(
            """
            <article class="post">
              <div class="post-content" itemprop="articleBody">
                <p>I'm coming back to America for the summer.</p>
                <p><strong>1 - Back the money by Gold.</strong></p>
                <p>You work in a Ponzi scheme. The best time was 50 years ago.</p>
              </div>
            </article>
            """.trimIndent(),
        )
        assertFalse(
            "leftover HTML indent must not become a CommonMark code block",
            markdown.lines().any { it.startsWith("    ") || it.startsWith("\t") },
        )
        assertTrue(markdown.contains("I'm coming back to America for the summer."))
        assertTrue(markdown.contains("**1 - Back the money by Gold.**"))
        assertFalse(markdown.contains("```"))
    }

    @Test
    fun realCodeBlocksKeepInternalIndent() {
        val markdown = HtmlToMarkdown.convert(
            "<p>Intro</p><pre><code>def hi():\n    return 1\n</code></pre>",
        )
        assertTrue(markdown.contains("```\ndef hi():\n    return 1\n```"))
    }

    @Test
    fun prependsObviousBylineFromRelAuthor() {
        val markdown = HtmlToMarkdown.convert(
            """<p>By <a rel="author" href="/jane">Jane Doe</a></p><p>Body text.</p>""",
        )
        assertTrue(markdown.startsWith("*Jane Doe*"))
        assertTrue(markdown.contains("Body text."))
    }
}
