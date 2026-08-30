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
    private val foaf = PaintedHighlight("3", "foaf", mine = false, foaf = true)
    private val other = PaintedHighlight("4", "other", mine = false)

    @Test
    fun filterFollowsHighlightVisibilitySettings() {
        val settings = UserSettings.parse(
            """{"defaultHighlightVisibilityMine":true,"defaultHighlightVisibilityFriends":false,"defaultHighlightVisibilityNostrverse":true}""",
        )
        val filter = highlightFilter(settings)
        assertTrue(filter.shows(mine))
        assertFalse(filter.shows(friend))
        assertTrue(filter.shows(foaf))
        assertTrue(filter.shows(other))
    }

    @Test
    fun filterEnablesFoafWhenOnlyFoafExistsAndItIsOff() {
        val settings = UserSettings.parse(
            """{"defaultHighlightVisibilityMine":true,"defaultHighlightVisibilityFriends":true,"defaultHighlightVisibilityFoaf":false,"defaultHighlightVisibilityNostrverse":false}""",
        )
        val filter = highlightFilter(settings, listOf(foaf, other))
        assertTrue(filter.foaf)
        assertFalse(filter.nostrverse)
        assertTrue(filter.shows(foaf))
        assertFalse(filter.shows(other))
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
                """{"defaultHighlightVisibilityMine":true,"defaultHighlightVisibilityFriends":false,"defaultHighlightVisibilityFoaf":false,"defaultHighlightVisibilityNostrverse":false}""",
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

    @Test
    fun contextKeepsSurroundingSentencesUnmarked() {
        val quote = "I carry my home with me not in luggage, but in myself."
        val context =
            "But the truth is deeper than that. I carry my home with me not in luggage, but in myself. In my body, my heart, my sense of presence."
        assertEquals(
            Triple(
                "But the truth is deeper than that. ",
                quote,
                " In my body, my heart, my sense of presence.",
            ),
            highlightContextParts(quote, context),
        )
    }

    @Test
    fun contextFindsQuoteAcrossNewlines() {
        val quote = "I carry my home with me not in luggage, but in myself."
        val context =
            "But the truth is deeper than that.\nI carry my home with me not in luggage, but in myself.\nIn my body, my heart, my sense of presence."
        val parts = highlightContextParts(quote, context)
        assertEquals(quote, parts.second.replace(Regex("\\s+"), " ").trim())
        assertTrue(parts.first.contains("truth is deeper"))
        assertTrue(parts.third.contains("sense of presence"))
    }

    @Test
    fun matchingContextStillMarksOnlyTheMiddleSentence() {
        val context =
            "But the truth is deeper than that. I carry my home with me not in luggage, but in myself. In my body, my heart, my sense of presence."
        val parts = highlightContextParts(context, context)
        assertEquals(
            "I carry my home with me not in luggage, but in myself.",
            parts.second,
        )
        assertTrue(parts.first.contains("truth is deeper"))
        assertTrue(parts.third.contains("sense of presence"))
    }
}
