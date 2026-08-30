package org.dergigi.boris.ui.reader

import org.dergigi.boris.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightVisibilityTest {
    @Test
    fun filtersMineFriendsAndOthersFromSettings() {
        val mine = PaintedHighlight("1", "mine", mine = true)
        val friend = PaintedHighlight("2", "friend", mine = false, friend = true)
        val foaf = PaintedHighlight("3", "foaf", mine = false, foaf = true)
        val other = PaintedHighlight("4", "other", mine = false)
        val all = listOf(mine, friend, foaf, other)
        val hidden = UserSettings.parse("""{"showHighlights":false}""")
        assertTrue(all.visibleFor(hidden).isEmpty())
        val mineOnly = UserSettings.parse(
            """{"showHighlights":true,"defaultHighlightVisibilityMine":true,"defaultHighlightVisibilityFriends":false,"defaultHighlightVisibilityFoaf":false,"defaultHighlightVisibilityNostrverse":false}""",
        )
        assertEquals(listOf(mine), all.visibleFor(mineOnly))
        val friendsOnly = UserSettings.parse(
            """{"showHighlights":true,"defaultHighlightVisibilityMine":false,"defaultHighlightVisibilityFriends":true,"defaultHighlightVisibilityFoaf":false,"defaultHighlightVisibilityNostrverse":false}""",
        )
        assertEquals(listOf(friend), all.visibleFor(friendsOnly))
        val foafOnly = UserSettings.parse(
            """{"showHighlights":true,"defaultHighlightVisibilityMine":false,"defaultHighlightVisibilityFriends":false,"defaultHighlightVisibilityFoaf":true,"defaultHighlightVisibilityNostrverse":false}""",
        )
        assertEquals(listOf(foaf), all.visibleFor(foafOnly))
        val othersOnly = UserSettings.parse(
            """{"showHighlights":true,"defaultHighlightVisibilityMine":false,"defaultHighlightVisibilityFriends":false,"defaultHighlightVisibilityFoaf":false,"defaultHighlightVisibilityNostrverse":true}""",
        )
        assertEquals(listOf(other), all.visibleFor(othersOnly))
    }
}
