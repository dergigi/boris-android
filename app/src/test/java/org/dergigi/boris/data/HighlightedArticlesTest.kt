package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip01Event.Companion.KIND_HIGHLIGHT
import org.dergigi.boris.nostr.Nip01Event.Companion.KIND_LONG_FORM
import org.dergigi.boris.nostr.Profile
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
        assertEquals(
            Profile.displayName(
                "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
                null,
            ),
            articles[0].host,
        )
        assertEquals("nostr:", articles[0].url.take(6))
    }

    @Test
    fun usesAuthorNameAsHostForNostrArticles() {
        val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        EventCache.put(
            Nip01Event(
                id = "33".repeat(32),
                pubkey = pubkey,
                createdAt = 1,
                kind = Nip01Event.KIND_METADATA,
                tags = emptyList(),
                content = """{"name":"Gigi","picture":"https://cdn.example.com/gigi.png"}""",
                sig = "44".repeat(32),
            ),
        )
        val articles = HighlightedArticles.fromEvents(
            listOf(
                highlight(
                    "ignored",
                    createdAt = 5,
                    tags = listOf(listOf("a", "30023:$pubkey:my-article")),
                ),
            ),
            limit = 12,
        )
        assertEquals("Gigi", articles[0].host)
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
    fun mostHighlightedRanksByUniquePeopleNotHighlightCount() {
        val now = 1_700_000_000L
        val events = listOf(
            highlight("https://example.com/popular", createdAt = now - 10, pubkey = person("aa")),
            highlight("https://example.com/popular", createdAt = now - 9, pubkey = person("bb")),
            highlight("https://example.com/popular", createdAt = now - 8, pubkey = person("cc")),
            highlight("https://example.com/runner-up", createdAt = now - 7, pubkey = person("aa")),
            highlight("https://example.com/runner-up", createdAt = now - 6, pubkey = person("bb")),
            highlight("https://example.com/lonely", createdAt = now - 5, pubkey = person("aa")),
            highlight("https://example.com/one-guy", createdAt = now - 4, pubkey = person("dd")),
            highlight("https://example.com/one-guy", createdAt = now - 3, pubkey = person("dd")),
            highlight("https://example.com/one-guy", createdAt = now - 2, pubkey = person("dd")),
            highlight("https://example.com/one-guy", createdAt = now - 1, pubkey = person("dd")),
        )
        val articles = HighlightedArticles.mostHighlighted(events, limit = 12, since = now - 7 * 24 * 60 * 60)
        assertEquals(
            listOf(
                "https://example.com/popular",
                "https://example.com/runner-up",
            ),
            articles.map { it.url },
        )
    }

    @Test
    fun mostHighlightedIgnoresOlderThanAWeek() {
        val now = 1_700_000_000L
        val week = 7L * 24 * 60 * 60
        val events = listOf(
            highlight("https://example.com/old", createdAt = now - week - 1, pubkey = person("aa")),
            highlight("https://example.com/old", createdAt = now - week - 2, pubkey = person("bb")),
            highlight("https://example.com/old", createdAt = now - week - 3, pubkey = person("cc")),
            highlight("https://example.com/fresh", createdAt = now - 10, pubkey = person("aa")),
            highlight("https://example.com/fresh", createdAt = now - 9, pubkey = person("bb")),
        )
        val articles = HighlightedArticles.mostHighlighted(events, limit = 12, since = now - week)
        assertEquals(listOf("https://example.com/fresh"), articles.map { it.url })
    }

    @Test
    fun mostHighlightedHonorsDayAndMonthWindows() {
        val now = 1_700_000_000L
        val events = listOf(
            highlight("https://example.com/today", createdAt = now - 3_600, pubkey = person("aa")),
            highlight("https://example.com/today", createdAt = now - 7_200, pubkey = person("bb")),
            highlight("https://example.com/month", createdAt = now - 10 * 24 * 60 * 60, pubkey = person("aa")),
            highlight("https://example.com/month", createdAt = now - 11 * 24 * 60 * 60, pubkey = person("bb")),
        )
        assertEquals(
            listOf("https://example.com/today"),
            HighlightedArticles.mostHighlighted(
                events,
                limit = 12,
                since = MostHighlightedWindow.Day.since(now),
            ).map { it.url },
        )
        assertEquals(
            listOf("https://example.com/today", "https://example.com/month"),
            HighlightedArticles.mostHighlighted(
                events,
                limit = 12,
                since = MostHighlightedWindow.Month.since(now),
            ).map { it.url },
        )
    }

    @Test
    fun mostHighlightedDedupesEventIds() {
        val duplicate = highlight("https://example.com/one", createdAt = 1_700_000_000L)
        val articles = HighlightedArticles.mostHighlighted(
            listOf(duplicate, duplicate, duplicate),
            limit = 12,
            since = 0,
        )
        assertEquals(emptyList<String>(), articles.map { it.url })
    }

    private fun person(byte: String): String = byte.repeat(32)

    private fun highlight(
        url: String,
        createdAt: Long,
        pubkey: String = person("aa"),
        tags: List<List<String>> = listOf(listOf("r", url)),
    ): Nip01Event = Nip01Event(
        id = (createdAt.toString() + pubkey.take(8)).padStart(64, '0').takeLast(64),
        pubkey = pubkey,
        createdAt = createdAt,
        kind = KIND_HIGHLIGHT,
        tags = tags,
        content = "quote",
        sig = "bb".repeat(32),
    )
}
