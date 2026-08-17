package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RandomArticlesTest {
    private fun item(url: String, title: String = url) = BookmarkItem(
        id = "r:$url",
        title = title,
        url = url,
        host = "example.com",
        imageUrl = null,
        createdAt = 1L,
        bucket = BookmarkBucket.Public,
    )

    @Test
    fun skipsArchivedAndDedupesUrls() {
        val archived = setOf(ArchivedArticles.key("https://example.com/done")!!)
        val articles = RandomArticles.articles(
            items = listOf(
                item("https://example.com/a"),
                item("https://example.com/a"),
                item("https://example.com/done"),
                item("https://example.com/b"),
            ),
            archivedKeys = archived,
            limit = 12,
            random = Random(1),
        )
        assertEquals(
            setOf("https://example.com/a", "https://example.com/b"),
            articles.map { it.url }.toSet(),
        )
    }

    @Test
    fun respectsLimitAndUsesSeededOrder() {
        val items = (1..5).map { item("https://example.com/$it") }
        val articles = RandomArticles.articles(items, emptySet(), limit = 3, random = Random(42))
        assertEquals(3, articles.size)
        assertEquals(
            RandomArticles.articles(items, emptySet(), limit = 3, random = Random(42)).map { it.url },
            articles.map { it.url },
        )
    }

    @Test
    fun emptyWhenNothingUnread() {
        val url = "https://example.com/done"
        val articles = RandomArticles.articles(
            items = listOf(item(url)),
            archivedKeys = setOf(ArchivedArticles.key(url)!!),
            limit = 12,
        )
        assertTrue(articles.isEmpty())
    }
}
