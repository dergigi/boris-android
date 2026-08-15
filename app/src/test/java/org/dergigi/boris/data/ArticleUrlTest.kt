package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleUrlTest {
    @Test
    fun hostDropsWww() {
        assertEquals("example.com", ArticleUrl.host("https://www.example.com/path"))
    }

    @Test
    fun rootIsSchemeAndHostOnly() {
        assertEquals("https://example.com", ArticleUrl.root("https://www.example.com/path?q=1#frag"))
        assertEquals("https://example.com", ArticleUrl.root("http://example.com/path"))
    }

    @Test
    fun rootNullForNostrContent() {
        assertNull(ArticleUrl.root("nostr:naddr1qqxnzd3exsurswpnxsmnwwpjqy"))
        assertNull(ArticleUrl.root("nostr:note1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq"))
    }

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
