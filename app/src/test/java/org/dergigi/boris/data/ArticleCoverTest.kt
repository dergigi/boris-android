package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleCoverTest {
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
}
