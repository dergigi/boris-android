package org.dergigi.boris.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ArticleCacheTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() {
        ArticleCache.init(folder.newFolder("article_cache"))
    }

    @After
    fun tearDown() {
        ArticleCache.reset()
    }

    @Test
    fun roundTripsEveryField() {
        val content = ReadableContent(
            url = "https://example.com/article",
            title = "A Title",
            markdown = "Some **markdown** body.",
            html = "<p>html</p>",
            publishedAt = 1_700_000_000L,
            articleCoordinate = "30023:abc:slug",
            eventId = "event123",
            authorPubkey = "a".repeat(64),
            imageUrl = "https://example.com/cover.jpg",
            summary = "A summary.",
            sourceZapTags = listOf(listOf("zap", "pubkey", "relay", "1")),
            tags = listOf(listOf("content-warning", "nudity"), listOf("t", "nsfw")),
        )
        assertEquals(content, ArticleCache.decode(ArticleCache.encode(content)))
    }

    @Test
    fun roundTripsNullFields() {
        val content = ReadableContent(
            url = "https://example.com/bare",
            markdown = "Body only.",
        )
        assertEquals(content, ArticleCache.decode(ArticleCache.encode(content)))
    }

    @Test
    fun cacheKeyNormalizesSchemes() {
        val https = ArticleCache.cacheKey("https://example.com/a")
        assertEquals(https, ArticleCache.cacheKey("http://example.com/a"))
        assertEquals(https, ArticleCache.cacheKey("example.com/a"))
        assertEquals(https, ArticleCache.cacheKey("  https://example.com/a  "))
        assertNotEquals(https, ArticleCache.cacheKey("https://example.com/b"))
    }

    @Test
    fun savedArticleLoadsBack() {
        val content = ReadableContent(
            url = "https://example.com/saved",
            title = "Saved",
            markdown = "Cached body.",
        )
        ArticleCache.save(content.url, content)
        assertEquals(content, ArticleCache.load(content.url))
        assertEquals(content, ArticleCache.load("http://example.com/saved"))
    }

    @Test
    fun loadReturnsNullForBlankBody() {
        val content = ReadableContent(url = "https://example.com/blank", markdown = " ")
        ArticleCache.save(content.url, content)
        assertNull(ArticleCache.load(content.url))
    }

    @Test
    fun loadReturnsNullWhenMissing() {
        assertNull(ArticleCache.load("https://example.com/never-saved"))
    }

    @Test
    fun saveOverwritesForRefresh() {
        val url = "https://example.com/refresh"
        ArticleCache.save(url, ReadableContent(url = url, markdown = "Old parse."))
        val updated = ReadableContent(url = url, title = "New", markdown = "New parse.")
        ArticleCache.save(url, updated)
        assertEquals(updated, ArticleCache.load(url))
    }

    @Test
    fun removeDeletesEntry() {
        val url = "https://example.com/removed"
        ArticleCache.save(url, ReadableContent(url = url, markdown = "Body."))
        ArticleCache.remove(url)
        assertNull(ArticleCache.load(url))
    }
}
