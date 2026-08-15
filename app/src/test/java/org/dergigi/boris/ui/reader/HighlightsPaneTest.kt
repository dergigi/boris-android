package org.dergigi.boris.ui.reader

import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.feed.FeedLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightsPaneTest {
    private val mine = PaintedHighlight("1", "mine", mine = true)
    private val friend = PaintedHighlight("2", "friend", mine = false, friend = true)
    private val other = PaintedHighlight("3", "other", mine = false)

    @Test
    fun filterFollowsHighlightVisibilitySettings() {
        val settings = UserSettings.parse(
            """{"defaultHighlightVisibilityMine":true,"defaultHighlightVisibilityFriends":false,"defaultHighlightVisibilityNostrverse":true}""",
        )
        val filter = highlightFilter(settings)
        assertTrue(filter.shows(mine))
        assertFalse(filter.shows(friend))
        assertTrue(filter.shows(other))
    }

    @Test
    fun filterKeepsAtLeastOneLevelOn() {
        val onlyMine = highlightFilter(
            UserSettings.parse(
                """{"defaultHighlightVisibilityMine":true,"defaultHighlightVisibilityFriends":false,"defaultHighlightVisibilityNostrverse":false}""",
            ),
        )
        assertEquals(onlyMine, onlyMine.toggle(FeedLevel.Mine))
        val next = onlyMine.toggle(FeedLevel.Friends)
        assertTrue(next.mine)
        assertTrue(next.friends)
        assertFalse(next.nostrverse)
    }

    @Test
    fun contextSplitsAroundTheQuote() {
        assertEquals(
            Triple("before ", "quote", " after"),
            highlightContextParts("quote", "before quote after"),
        )
        assertEquals(Triple("", "quote", ""), highlightContextParts("quote", null))
        assertEquals(Triple("", "quote", ""), highlightContextParts("quote", "   "))
        assertEquals(
            Triple("nearby words", "quote", ""),
            highlightContextParts("quote", "nearby words"),
        )
    }
}
