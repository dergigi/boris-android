package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleFindTest {
    @Test
    fun hitsFindsIgnoreCase() {
        val hits = ArticleFind.hits("Hello World hello", "HELLO")
        assertEquals(2, hits.size)
        assertEquals("Hello", hits[0].match)
        assertEquals("hello", hits[1].match)
    }

    @Test
    fun hitsBuildsSnippet() {
        val hits = ArticleFind.hits("alpha beta gamma delta", "beta", contextChars = 6)
        assertEquals(1, hits.size)
        assertTrue(hits[0].snippet.contains("beta"))
    }

    @Test
    fun hitsEmptyOnBlankQuery() {
        assertTrue(ArticleFind.hits("hello", "  ").isEmpty())
        assertTrue(ArticleFind.hits("hello", "").isEmpty())
    }

    @Test
    fun paintedRequiresQuery() {
        assertEquals(null, ArticleFind.painted("  "))
        val mark = ArticleFind.painted("find me")!!
        assertEquals(ArticleFind.HIGHLIGHT_ID, mark.id)
        assertTrue(mark.find)
        assertTrue(mark.ignoreCase)
    }
}
