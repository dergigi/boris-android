package org.dergigi.boris.ui.feed

import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedViewModelTest {
    @Test
    fun writingFromBuildsANostrArticleUrl() {
        val me = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val writing = FeedViewModel.writingFrom(
            event = article(d = "my-article", title = "Hello", summary = "A short note."),
            profile = Profile(name = "Gigi", picture = "https://example.com/p.png"),
            sessionHex = me,
            friends = emptySet(),
            nowSeconds = 1_610_582_400L,
        )!!
        assertEquals("Hello", writing.title)
        assertEquals("A short note.", writing.summary)
        assertTrue(writing.url.startsWith("nostr:naddr1"))
        assertEquals("Gigi", writing.authorName)
        assertEquals(FeedLevel.Mine, writing.level)
        assertEquals(1_610_582_400L, writing.publishedAt)
    }

    @Test
    fun writingFromClassifiesFriendsAndNostrverse() {
        val me = "aa".repeat(32)
        val friend = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val friends = FeedViewModel.writingFrom(
            event = article(d = "from-a-friend", title = "Hi"),
            profile = null,
            sessionHex = me,
            friends = setOf(friend),
            nowSeconds = 1_610_582_400L,
        )!!
        assertEquals(FeedLevel.Friends, friends.level)
        val other = FeedViewModel.writingFrom(
            event = article(d = "from-afar", title = "Yo"),
            profile = null,
            sessionHex = me,
            friends = emptySet(),
            nowSeconds = 1_610_582_400L,
        )!!
        assertEquals(FeedLevel.Nostrverse, other.level)
        val hop = FeedViewModel.writingFrom(
            event = article(d = "from-foaf", title = "Hey"),
            profile = null,
            sessionHex = me,
            friends = emptySet(),
            foaf = setOf(friend),
            nowSeconds = 1_610_582_400L,
        )!!
        assertEquals(FeedLevel.Foaf, hop.level)
    }

    @Test
    fun writingFromSkipsEventsWithoutADtag() {
        assertNull(
            FeedViewModel.writingFrom(
                event = article(d = null, title = "Nope"),
                profile = null,
                sessionHex = null,
                friends = emptySet(),
                nowSeconds = 1_610_582_400L,
            ),
        )
    }

    @Test
    fun writingFromSkipsFarFuturePublishDates() {
        assertNull(
            FeedViewModel.writingFrom(
                event = article(d = "soon", title = "Later", publishedAt = "2000000000"),
                profile = null,
                sessionHex = null,
                friends = emptySet(),
                nowSeconds = 1_610_582_400L,
            ),
        )
    }

    private fun article(
        d: String?,
        title: String?,
        summary: String? = null,
        publishedAt: String = "1610582400",
    ): Nip01Event {
        val tags = buildList {
            if (d != null) add(listOf("d", d))
            if (title != null) add(listOf("title", title))
            if (summary != null) add(listOf("summary", summary))
            add(listOf("published_at", publishedAt))
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
