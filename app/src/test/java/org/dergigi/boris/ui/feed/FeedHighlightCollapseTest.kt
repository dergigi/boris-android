package org.dergigi.boris.ui.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedHighlightCollapseTest {
    @Test
    fun singleHighlightStaysOpen() {
        val item = highlight("1")
        val rows = FeedHighlightCollapse.rows(listOf(item), emptySet(), emptySet())
        assertEquals(listOf(FeedHighlightRow.Open(item)), rows)
    }

    @Test
    fun collapsedArticleBecomesOneMutedRow() {
        val first = highlight("1", createdAt = 20)
        val second = highlight("2", createdAt = 10)
        val key = FeedHighlightCollapse.articleKey(first)!!
        val rows = FeedHighlightCollapse.rows(listOf(first, second), setOf(key), emptySet())
        assertEquals(1, rows.size)
        val collapsed = rows.single() as FeedHighlightRow.Collapsed
        assertEquals(2, collapsed.count)
        assertEquals(HighlightCollapseReason.Article, collapsed.reason)
        assertEquals("example.com", collapsed.source)
        assertEquals(20, collapsed.sortAt)
    }

    @Test
    fun collapsedAuthorGroupsRemainingHighlights() {
        val aliceA = highlight("1", author = "aa".repeat(32), name = "Alice")
        val bob = highlight("2", author = "bb".repeat(32), name = "Bob", url = "https://other.test/post")
        val aliceB = highlight("3", author = "aa".repeat(32), name = "Alice")
        val rows = FeedHighlightCollapse.rows(
            listOf(aliceA, bob, aliceB),
            emptySet(),
            setOf("aa".repeat(32)),
        )
        assertEquals(2, rows.size)
        val collapsed = rows[0] as FeedHighlightRow.Collapsed
        assertEquals(2, collapsed.count)
        assertEquals(HighlightCollapseReason.Author, collapsed.reason)
        assertEquals("Alice", collapsed.authorName)
        assertEquals(FeedHighlightRow.Open(bob), rows[1])
    }

    @Test
    fun articleCollapseWinsOverAuthor() {
        val alice = highlight("1", author = "aa".repeat(32))
        val key = FeedHighlightCollapse.articleKey(alice)!!
        val rows = FeedHighlightCollapse.rows(listOf(alice), setOf(key), setOf("aa".repeat(32)))
        val collapsed = rows.single() as FeedHighlightRow.Collapsed
        assertEquals(HighlightCollapseReason.Article, collapsed.reason)
    }

    @Test
    fun missingUrlCannotCollapseByArticle() {
        val item = highlight("1", url = null)
        assertEquals(null, FeedHighlightCollapse.articleKey(item))
        val rows = FeedHighlightCollapse.rows(listOf(item), setOf("anything"), emptySet())
        assertTrue(rows.single() is FeedHighlightRow.Open)
    }

    private fun highlight(
        id: String,
        url: String? = "https://example.com/essay",
        author: String = "11".repeat(32),
        name: String = "Ada",
        createdAt: Long = 1,
    ) = FeedItem(
        id = id,
        quote = "quote $id",
        url = url,
        host = url?.let { "example.com" },
        authorHex = author,
        authorName = name,
        authorPicture = null,
        createdAt = createdAt,
        level = FeedLevel.Friends,
    )
}
