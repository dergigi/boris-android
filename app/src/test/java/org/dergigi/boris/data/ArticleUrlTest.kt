package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleUrlTest {
    @Test
    fun normalizeDropsWww() {
        assertEquals(
            "https://example.com/path",
            ArticleUrl.normalize("https://www.example.com/path"),
        )
    }

    @Test
    fun normalizeForcesHttps() {
        assertEquals(
            "https://example.com/path",
            ArticleUrl.normalize("http://example.com/path"),
        )
    }

    @Test
    fun normalizeDropsTrailingSlash() {
        assertEquals(
            "https://example.com/path",
            ArticleUrl.normalize("https://example.com/path/"),
        )
    }

    @Test
    fun normalizeDropsUtmQuery() {
        assertEquals(
            "https://example.com/path",
            ArticleUrl.normalize("https://example.com/path?utm_source=x&utm_medium=y"),
        )
    }

    @Test
    fun normalizeDropsFragment() {
        assertEquals(
            "https://example.com/path",
            ArticleUrl.normalize("https://example.com/path#section"),
        )
    }

    @Test
    fun normalizeKeepsCleanHttps() {
        assertEquals(
            "https://example.com/path",
            ArticleUrl.normalize("https://example.com/path"),
        )
    }

    @Test
    fun normalizeCollapsesAllDecorations() {
        assertEquals(
            "https://example.com/path",
            ArticleUrl.normalize("http://www.example.com/path/?utm=1#frag"),
        )
    }
}
