package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleCoverTest {
    @Test
    fun readsImageAndDescriptionFromJinaHeader() {
        val raw = """
            Title: Hello
            URL Source: https://example.com/hello
            Image URL: https://cdn.example.com/cover.jpg
            Description: A short lede
            Markdown Content:
            # Hello
        """.trimIndent()
        assertEquals("https://cdn.example.com/cover.jpg", ArticleCover.imageFromJina(raw))
        assertEquals("A short lede", ArticleCover.descriptionFromJina(raw))
    }

    @Test
    fun firstMarkdownImageTakesTheLeadPicture() {
        val markdown = """
            ![cover](https://cdn.example.com/hero.png)

            Body text.
        """.trimIndent()
        assertEquals("https://cdn.example.com/hero.png", ArticleCover.firstMarkdownImage(markdown))
    }

    @Test
    fun stripLeadingImageRemovesMatchingHero() {
        val markdown = """
            ![](https://cdn.example.com/hero.png)

            Body text.
        """.trimIndent()
        assertEquals(
            "Body text.",
            ArticleCover.stripLeadingImage(markdown, "https://cdn.example.com/hero.png"),
        )
    }

    @Test
    fun stripLeadingImageLeavesADifferentPicture() {
        val markdown = "![other](https://cdn.example.com/other.png)\n\nBody"
        assertEquals(
            markdown,
            ArticleCover.stripLeadingImage(markdown, "https://cdn.example.com/hero.png"),
        )
    }

    @Test
    fun missingJinaFieldsAreNull() {
        val raw = "Title: Hello\nMarkdown Content:\n# Hello"
        assertNull(ArticleCover.imageFromJina(raw))
        assertNull(ArticleCover.descriptionFromJina(raw))
    }
}
