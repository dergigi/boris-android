package org.dergigi.boris.ui.reader

import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.feed.FeedLevel
import org.dergigi.boris.ui.highlightContextParts
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
    fun filterEnablesNostrverseWhenOnlyOthersExistAndItIsOff() {
        val settings = UserSettings.parse(
            """{"defaultHighlightVisibilityMine":true,"defaultHighlightVisibilityFriends":true,"defaultHighlightVisibilityNostrverse":false}""",
        )
        val filter = highlightFilter(settings, listOf(other))
        assertTrue(filter.nostrverse)
        assertTrue(filter.friends)
        assertTrue(filter.mine)
        assertTrue(filter.shows(other))
    }

    @Test
    fun filterEnablesFriendsWhenOnlyFriendsExistAndTheyAreOff() {
        val settings = UserSettings.parse(
            """{"defaultHighlightVisibilityMine":true,"defaultHighlightVisibilityFriends":false,"defaultHighlightVisibilityNostrverse":false}""",
        )
        val filter = highlightFilter(settings, listOf(friend, other))
        assertTrue(filter.friends)
        assertFalse(filter.nostrverse)
        assertTrue(filter.shows(friend))
        assertFalse(filter.shows(other))
    }

    @Test
    fun filterKeepsSettingsWhenSomethingAlreadyMatches() {
        val settings = UserSettings.parse(
            """{"defaultHighlightVisibilityMine":true,"defaultHighlightVisibilityFriends":false,"defaultHighlightVisibilityNostrverse":false}""",
        )
        val filter = highlightFilter(settings, listOf(mine, other))
        assertTrue(filter.mine)
        assertFalse(filter.friends)
        assertFalse(filter.nostrverse)
        assertTrue(filter.shows(mine))
        assertFalse(filter.shows(other))
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
