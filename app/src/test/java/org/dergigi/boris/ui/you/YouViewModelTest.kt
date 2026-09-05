package org.dergigi.boris.ui.you

import org.dergigi.boris.nostr.Nip01Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouViewModelTest {
    @Test
    fun writingFromBuildsANostrArticleUrl() {
        val event = article(
            d = "my-article",
            title = "Hello",
            summary = "A short note.",
        )
        val writing = YouViewModel.writingFrom(event)!!
        assertEquals("Hello", writing.title)
        assertEquals("A short note.", writing.summary)
        assertTrue(writing.url.startsWith("nostr:naddr1"))
        assertEquals(1_610_582_400L, writing.publishedAt)
    }

    @Test
    fun highlightMatchesQuoteContextAndHost() {
        val item = YouHighlight(
            id = "1",
            quote = "Purple text, orange highlights",
            context = "A longer paragraph about reading.",
            url = "https://example.com/post",
            host = "example.com",
            createdAt = 1,
        )
        assertTrue(item.matchesQuery("orange"))
        assertTrue(item.matchesQuery("READING"))
        assertTrue(item.matchesQuery("example"))
        assertTrue(!item.matchesQuery("bitcoin"))
        assertTrue(item.matchesQuery("  "))
    }

    @Test
    fun writingMatchesTitleAndSummary() {
        val item = YouWriting(
            id = "1",
            title = "I Left the Future",
            summary = "Arrived at home.",
            imageUrl = null,
            url = "nostr:naddr1qq",
            publishedAt = 1,
        )
        assertTrue(item.matchesQuery("future"))
        assertTrue(item.matchesQuery("HOME"))
        assertTrue(!item.matchesQuery("bitcoin"))
    }

    @Test
    fun bookmarkMatchesTitleHostAndUrl() {
        val item = org.dergigi.boris.data.BookmarkItem(
            id = "r:https://example.com/post",
            title = "A public bookmark",
            url = "https://example.com/post",
            host = "example.com",
            imageUrl = null,
            createdAt = 1,
            bucket = org.dergigi.boris.data.BookmarkBucket.Public,
        )
        assertTrue(item.matchesQuery("public"))
        assertTrue(item.matchesQuery("EXAMPLE"))
        assertTrue(item.matchesQuery("post"))
        assertTrue(!item.matchesQuery("bitcoin"))
    }

    @Test
    fun allMergesKindsNewestFirst() {
        val highlight = YouHighlight(
            id = "h1",
            quote = "quote",
            url = "https://example.com/a",
            host = "example.com",
            createdAt = 30,
        )
        val writing = YouWriting(
            id = "w1",
            title = "Essay",
            summary = null,
            imageUrl = null,
            url = "nostr:naddr1qq",
            publishedAt = 50,
        )
        val public = org.dergigi.boris.data.BookmarkItem(
            id = "p1",
            title = "Public",
            url = "https://example.com/p",
            host = "example.com",
            imageUrl = null,
            createdAt = 40,
            bucket = org.dergigi.boris.data.BookmarkBucket.Public,
        )
        val web = org.dergigi.boris.data.BookmarkItem(
            id = "w-b1",
            title = "Web",
            url = "https://example.com/w",
            host = "example.com",
            imageUrl = null,
            createdAt = 10,
            bucket = org.dergigi.boris.data.BookmarkBucket.Web,
        )
        val associated = org.dergigi.boris.data.BookmarkItem(
            id = "c1",
            title = "Associated",
            url = "https://example.com/a",
            host = "example.com",
            imageUrl = null,
            createdAt = 20,
            bucket = org.dergigi.boris.data.BookmarkBucket.Web,
        )
        val merged = mergeYouItems(
            highlights = listOf(highlight),
            writings = listOf(writing),
            publicBookmarks = listOf(public),
            webBookmarks = listOf(web),
            associatedArticles = listOf(associated),
        )
        assertEquals(
            listOf("w:w1", "b:p1", "h:h1", "b:c1", "b:w-b1"),
            merged.map { it.key },
        )
    }

    @Test
    fun allDropsDeletedHighlightsAndHonorsSearch() {
        val keep = YouHighlight(
            id = "keep",
            quote = "bitcoin brief",
            url = null,
            host = null,
            createdAt = 2,
        )
        val gone = YouHighlight(
            id = "gone",
            quote = "bitcoin brief",
            url = null,
            host = null,
            createdAt = 3,
        )
        val writing = YouWriting(
            id = "w1",
            title = "Other topic",
            summary = null,
            imageUrl = null,
            url = "nostr:naddr1qq",
            publishedAt = 4,
        )
        val merged = mergeYouItems(
            highlights = listOf(keep, gone),
            writings = listOf(writing),
            publicBookmarks = emptyList(),
            webBookmarks = emptyList(),
            associatedArticles = emptyList(),
            deletedIds = setOf("gone"),
            query = "bitcoin",
        )
        assertEquals(listOf("h:keep"), merged.map { it.key })
    }

    @Test
    fun writingFromSkipsEventsWithoutADtag() {
        val event = article(d = null, title = "Nope")
        assertNull(YouViewModel.writingFrom(event))
    }

    private fun article(
        d: String?,
        title: String?,
        summary: String? = null,
    ): Nip01Event {
        val tags = buildList {
            if (d != null) add(listOf("d", d))
            if (title != null) add(listOf("title", title))
            if (summary != null) add(listOf("summary", summary))
            add(listOf("published_at", "1610582400"))
        }
        return Nip01Event(
            id = "d7a92714f81d0f712e715556aee69ea6da6bfb287e6baf794a095d301d603ec7",
            pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            createdAt = 42L,
            kind = Nip01Event.KIND_LONG_FORM,
            tags = tags,
            content = "body",
            sig = "36d34e6448fe0223e9999361c39c492a208bc423d2fcdfc2a3404e04df7c22dc",
        )
    }
}
