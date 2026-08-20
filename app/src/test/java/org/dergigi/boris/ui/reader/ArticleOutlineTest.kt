package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleOutlineTest {
    @Test
    fun emptyBodyHasNoHeadings() {
        assertTrue(ArticleOutline.parse("").isEmpty())
        assertTrue(ArticleOutline.parse("just a paragraph").isEmpty())
    }

    @Test
    fun parsesAtxLevelsInOrder() {
        val items = ArticleOutline.parse(
            """
            # One
            intro
            ## Two
            ### Three
            """.trimIndent(),
        )
        assertEquals(listOf(1, 2, 3), items.map { it.level })
        assertEquals(listOf("One", "Two", "Three"), items.map { it.title })
        assertEquals(listOf("outline:0", "outline:1", "outline:2"), items.map { it.id })
        assertEquals("outline:0", ArticleOutline.idAt(items, items[0].startOffset))
        assertNull(ArticleOutline.idAt(items, 999))
    }

    @Test
    fun skipsHeadingsInsideFencedCode() {
        val items = ArticleOutline.parse(
            """
            ## Real
            ```
            # Fake
            ```
            ## Also real
            ~~~
            ## Still fake
            ~~~
            """.trimIndent(),
        )
        assertEquals(listOf("Real", "Also real"), items.map { it.title })
    }

    @Test
    fun stripsInlineMarkupAndTrailingHashes() {
        val items = ArticleOutline.parse("## **Bold** [link](https://x.test) title ##")
        assertEquals(listOf("Bold link title"), items.map { it.title })
    }

    @Test
    fun parsesSetextHeadings() {
        val items = ArticleOutline.parse(
            """
            First
            =====
            Second
            ------
            """.trimIndent(),
        )
        assertEquals(listOf(1, 2), items.map { it.level })
        assertEquals(listOf("First", "Second"), items.map { it.title })
    }

    @Test
    fun activeIdTracksLastHeadingAboveThreshold() {
        val items = ArticleOutline.parse("## A\n\n## B\n\n## C")
        val tops = mapOf(
            items[0].id to -40f,
            items[1].id to 8f,
            items[2].id to 400f,
        )
        assertEquals(
            items[1].id,
            ArticleOutline.activeId(items, { tops[it] }, threshold = 48f),
        )
    }

    @Test
    fun paintedIsAnchorOnly() {
        val mark = ArticleOutline.painted("outline:0", "Hello")!!
        assertTrue(mark.outline)
        assertTrue(ArticleOutline.isId(mark.id))
        assertNull(ArticleOutline.painted("outline:0", "  "))
    }
}
