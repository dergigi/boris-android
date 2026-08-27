package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeFiltersTest {
    private fun article(name: String, title: String = name) = HighlightedArticle(
        url = "https://example.com/$name-${System.nanoTime()}",
        host = "example.com",
        title = title,
        imageUrl = null,
        highlightedAt = 1,
    )

    @Test
    fun hidesCompletedAtGreenThreshold() {
        val unread = article("unread")
        val done = article("done")
        ReadingPositionStore.save(done.url, 0.95f)
        val visible = HomeFilters.visible(
            listOf(unread, done),
            archivedKeys = emptySet(),
            hideArchived = false,
            hideCompleted = true,
            hideNsfw = false,
        )
        assertEquals(listOf(unread), visible)
    }

    @Test
    fun hidesNsfwByTitleKeyword() {
        val clean = article("clean", "On time preference")
        val nsfw = article("nsfw", "An NSFW photo essay")
        val visible = HomeFilters.visible(
            listOf(clean, nsfw),
            archivedKeys = emptySet(),
            hideArchived = false,
            hideCompleted = false,
            hideNsfw = true,
        )
        assertEquals(listOf(clean), visible)
    }

    @Test
    fun leavesAllWhenFiltersOff() {
        val items = listOf(article("a"), article("b", "NSFW notes"))
        assertEquals(
            items,
            HomeFilters.visible(
                items,
                archivedKeys = emptySet(),
                hideArchived = false,
                hideCompleted = false,
                hideNsfw = false,
            ),
        )
    }

    @Test
    fun sharedVisibilityHidesNsfwFromPlainItems() {
        assertEquals(
            false,
            HomeFilters.visible(
                url = "https://example.com/photo",
                title = "NSFW notes",
                summary = null,
                archivedKeys = emptySet(),
                hideArchived = false,
                hideCompleted = false,
                hideNsfw = true,
            ),
        )
    }
}
