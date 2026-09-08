package org.dergigi.boris.ui.home

import org.dergigi.boris.data.HighlightedArticle
import org.junit.Assert.assertEquals
import org.junit.Test

class ExploreRowsTest {
    @Test
    fun fillGivesEarlierRowsFirstClaimThenRestocksLaterRows() {
        val friends = listOf(article("a"), article("b"), article("c"))
        val liked = listOf(
            article("a"),
            article("b"),
            article("c"),
            article("d"),
            article("e"),
            article("f"),
        )
        val filled = ExploreRows.fill(
            order = listOf(HomeSections.FRIENDS, HomeSections.LIKED_FRIENDS),
            rows = mapOf(
                HomeSections.FRIENDS to friends,
                HomeSections.LIKED_FRIENDS to liked,
            ),
            limit = 3,
        )
        assertEquals(listOf("a", "b", "c"), filled.getValue(HomeSections.FRIENDS).map { it.url })
        assertEquals(listOf("d", "e", "f"), filled.getValue(HomeSections.LIKED_FRIENDS).map { it.url })
    }

    @Test
    fun fillSkipsHiddenRowsSoTheyDoNotConsumeUrls() {
        val filled = ExploreRows.fill(
            order = listOf(HomeSections.LIKED_FRIENDS),
            rows = mapOf(
                HomeSections.FRIENDS to listOf(article("a"), article("b")),
                HomeSections.LIKED_FRIENDS to listOf(article("a"), article("c")),
            ),
            limit = 2,
        )
        assertEquals(listOf("a", "c"), filled.getValue(HomeSections.LIKED_FRIENDS).map { it.url })
        assertEquals(setOf(HomeSections.LIKED_FRIENDS), filled.keys)
    }

    @Test
    fun fillDoesNotStarveMostHighlighted() {
        val popular = listOf(article("a"), article("b"), article("c"), article("hot"))
        val filled = ExploreRows.fill(
            order = listOf(HomeSections.FRIENDS, HomeSections.MOST),
            rows = mapOf(
                HomeSections.FRIENDS to listOf(article("a"), article("b"), article("c")),
                HomeSections.MOST to popular,
            ),
            limit = 3,
        )
        assertEquals(listOf("a", "b", "c"), filled.getValue(HomeSections.FRIENDS).map { it.url })
        assertEquals(listOf("a", "b", "c"), filled.getValue(HomeSections.MOST).map { it.url })
    }

    private fun article(url: String) = HighlightedArticle(
        url = url,
        host = "example.com",
        title = url,
        imageUrl = null,
        highlightedAt = 1,
    )
}
