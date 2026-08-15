package org.dergigi.boris.ui.reader

import org.dergigi.boris.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightVisibilityTest {
    @Test
    fun filtersMineAndOthersFromSettings() {
        val mine = PaintedHighlight("1", "mine", mine = true)
        val other = PaintedHighlight("2", "other", mine = false)
        val all = listOf(mine, other)
        val hidden = UserSettings.parse("""{"showHighlights":false}""")
        assertTrue(all.visibleFor(hidden).isEmpty())
        val mineOnly = UserSettings.parse(
            """{"showHighlights":true,"defaultHighlightVisibilityMine":true,"defaultHighlightVisibilityNostrverse":false}""",
        )
        assertEquals(listOf(mine), all.visibleFor(mineOnly))
        val othersOnly = UserSettings.parse(
            """{"showHighlights":true,"defaultHighlightVisibilityMine":false,"defaultHighlightVisibilityNostrverse":true}""",
        )
        assertEquals(listOf(other), all.visibleFor(othersOnly))
    }
}
