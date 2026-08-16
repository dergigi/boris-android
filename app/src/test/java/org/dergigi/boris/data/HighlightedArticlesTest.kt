package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip01Event.Companion.KIND_HIGHLIGHT
import org.dergigi.boris.nostr.Nip01Event.Companion.KIND_LONG_FORM
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HighlightedArticlesTest {
    @Before
    fun setUp() {
        EventCache.clear()
        OgPreviewCache.clear()
    }

    @After
    fun tearDown() {
        EventCache.clear()
        OgPreviewCache.clear()
    }
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
    fun usesCachedArticleTitleAndCover() {
        val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val coordinate = "30023:$pubkey:i-left-the-future-and-arrived-at-home"
        EventCache.put(
            Nip01Event(
                id = "11".repeat(32),
                pubkey = pubkey,
                createdAt = 10,
                kind = KIND_LONG_FORM,
                tags = listOf(
                    listOf("d", "i-left-the-future-and-arrived-at-home"),
                    listOf("title", "I Left the Future and Arrived at Home"),
                    listOf("image", "https://cdn.example.com/cover.jpg"),
                ),
                content = "body",
                sig = "22".repeat(32),
            ),
        )
        val articles = HighlightedArticles.fromEvents(
            listOf(highlight("ignored", createdAt = 5, tags = listOf(listOf("a", coordinate)))),
            limit = 12,
        )
        assertEquals("I Left the Future and Arrived at Home", articles[0].title)
        assertEquals("https://cdn.example.com/cover.jpg", articles[0].imageUrl)
    }

    @Test
    fun usesReaderPreviewWhenTheArticleEventIsMissing() {
        val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val coordinate = "30023:$pubkey:i-left-the-future-and-arrived-at-home"
        val uri = NostrArticle.fromCoordinate(coordinate)!!.uri
        ArticlePreview.remember(
            ReadableContent(
                url = uri,
                title = "I Left the Future and Arrived at Home",
                imageUrl = "https://cdn.example.com/cover.jpg",
                articleCoordinate = coordinate,
            ),
        )
        val articles = HighlightedArticles.fromEvents(
            listOf(highlight("ignored", createdAt = 5, tags = listOf(listOf("a", coordinate)))),
            limit = 12,
        )
        assertEquals("I Left the Future and Arrived at Home", articles[0].title)
        assertEquals("https://cdn.example.com/cover.jpg", articles[0].imageUrl)
    }

    @Test
    fun includesNotesFromETags() {
        val eventId = "d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f"
        val articles = HighlightedArticles.fromEvents(
            listOf(highlight("ignored", createdAt = 5, tags = listOf(listOf("e", eventId)))),
            limit = 12,
        )
        assertEquals(1, articles.size)
        assertEquals("nostr", articles[0].host)
        assertEquals("nostr:${org.dergigi.boris.nostr.Nip19.noteEncode(eventId)}", articles[0].url)
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

    @Test
    fun mostHighlightedRanksByCountAndNeedsAtLeastTwo() {
        val events = listOf(
            highlight("https://example.com/popular", createdAt = 1),
            highlight("https://example.com/popular", createdAt = 2),
            highlight("https://example.com/popular", createdAt = 3),
            highlight("https://example.com/runner-up", createdAt = 4),
            highlight("https://example.com/runner-up", createdAt = 5),
            highlight("https://example.com/lonely", createdAt = 6),
        )
        val articles = HighlightedArticles.mostHighlighted(events, limit = 12)
        assertEquals(
            listOf(
                "https://example.com/popular",
                "https://example.com/runner-up",
            ),
            articles.map { it.url },
        )
    }

    @Test
    fun mostHighlightedDedupesEventIds() {
        val duplicate = highlight("https://example.com/one", createdAt = 1)
        val articles = HighlightedArticles.mostHighlighted(
            listOf(duplicate, duplicate, duplicate),
            limit = 12,
        )
        assertEquals(emptyList<String>(), articles.map { it.url })
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
