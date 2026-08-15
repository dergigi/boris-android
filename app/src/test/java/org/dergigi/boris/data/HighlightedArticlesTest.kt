package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip01Event.Companion.KIND_HIGHLIGHT
import org.junit.Assert.assertEquals
import org.junit.Test

class HighlightedArticlesTest {
    @Test
    fun uniqueRecentKeepsNewestUrlAndDropsDuplicates() {
        val events = listOf(
            highlight("https://citadel21.com/wallet", createdAt = 30),
            highlight("https://www.citadel21.com/wallet/", createdAt = 10),
            highlight("https://geoffreylitt.com/bottleneck", createdAt = 20),
            highlight("https://example.com/no-host", createdAt = 40, tags = listOf(listOf("r", "not-a-url"))),
        )
        val articles = HighlightedArticles.fromEvents(events, limit = 12)
        assertEquals(
            listOf(
                "https://citadel21.com/wallet",
                "https://geoffreylitt.com/bottleneck",
            ),
            articles.map { it.url },
        )
        assertEquals("citadel21.com", articles[0].host)
        assertEquals(30L, articles[0].highlightedAt)
    }

    @Test
    fun includesNostrArticlesFromATags() {
        val coordinate =
            "30023:3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d:my-article"
        val articles = HighlightedArticles.fromEvents(
            listOf(highlight("ignored", createdAt = 5, tags = listOf(listOf("a", coordinate)))),
            limit = 12,
        )
        assertEquals(1, articles.size)
        assertEquals("my-article", articles[0].host)
        assertEquals("nostr:", articles[0].url.take(6))
    }

    @Test
    fun uniqueRecentHonorsLimit() {
        val events = (1..5).map { i ->
            highlight("https://example.com/p$i", createdAt = i.toLong())
        }
        val articles = HighlightedArticles.fromEvents(events, limit = 3)
        assertEquals(
            listOf(
                "https://example.com/p5",
                "https://example.com/p4",
                "https://example.com/p3",
            ),
            articles.map { it.url },
        )
    }

    private fun highlight(
        url: String,
        createdAt: Long,
        tags: List<List<String>> = listOf(listOf("r", url)),
    ): Nip01Event = Nip01Event(
        id = createdAt.toString().padStart(64, '0'),
        pubkey = "aa".repeat(32),
        createdAt = createdAt,
        kind = KIND_HIGHLIGHT,
        tags = tags,
        content = "quote",
        sig = "bb".repeat(32),
    )
}
