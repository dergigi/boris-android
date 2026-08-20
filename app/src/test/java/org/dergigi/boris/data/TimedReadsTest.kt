package org.dergigi.boris.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TimedReadsTest {
    @After
    fun tearDown() {
        ReadingTimeStore.clear()
    }

    private fun item(url: String) = BookmarkItem(
        id = "r:$url",
        title = url,
        url = url,
        host = "example.com",
        imageUrl = null,
        createdAt = 1L,
        bucket = BookmarkBucket.Public,
    )

    @Test
    fun shortKeepsFiveMinutesAndUnder() {
        val minutes = mapOf(
            "https://example.com/tiny" to 1,
            "https://example.com/short" to 5,
            "https://example.com/medium" to 8,
            "https://example.com/long" to 20,
        )
        val articles = TimedReads.articles(
            items = minutes.keys.map(::item),
            archivedKeys = emptySet(),
            kind = TimedReadKind.Short,
            minutes = minutes,
            limit = 12,
            random = Random(1),
        )
        assertEquals(
            setOf("https://example.com/tiny", "https://example.com/short"),
            articles.map { it.url }.toSet(),
        )
    }

    @Test
    fun longKeepsFifteenMinutesAndOver() {
        val minutes = mapOf(
            "https://example.com/short" to 5,
            "https://example.com/medium" to 14,
            "https://example.com/long" to 15,
            "https://example.com/book" to 40,
        )
        val articles = TimedReads.articles(
            items = minutes.keys.map(::item),
            archivedKeys = emptySet(),
            kind = TimedReadKind.Long,
            minutes = minutes,
            limit = 12,
            random = Random(1),
        )
        assertEquals(
            setOf("https://example.com/long", "https://example.com/book"),
            articles.map { it.url }.toSet(),
        )
    }

    @Test
    fun skipsArchivedAndUnknownLength() {
        val done = "https://example.com/done"
        val articles = TimedReads.articles(
            items = listOf(item(done), item("https://example.com/unknown"), item("https://example.com/short")),
            archivedKeys = setOf(ArchivedArticles.key(done)!!),
            kind = TimedReadKind.Short,
            minutes = mapOf("https://example.com/short" to 3),
            limit = 12,
        )
        assertEquals(listOf("https://example.com/short"), articles.map { it.url })
    }

    @Test
    fun emptyWhenNothingMatches() {
        val articles = TimedReads.articles(
            items = listOf(item("https://example.com/medium")),
            archivedKeys = emptySet(),
            kind = TimedReadKind.Long,
            minutes = mapOf("https://example.com/medium" to 8),
            limit = 12,
        )
        assertTrue(articles.isEmpty())
    }
}
