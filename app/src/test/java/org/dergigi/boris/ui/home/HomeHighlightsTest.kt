package org.dergigi.boris.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeHighlightsTest {
    @Test
    fun othersExcludesYouAndFriends() {
        val me = "aa".repeat(32)
        val friend = "bb".repeat(32)
        val other = "cc".repeat(32)
        val friends = setOf(friend)
        assertFalse(isNetworkHighlight(me, me, friends))
        assertFalse(isNetworkHighlight(friend.uppercase(), me, friends))
        assertTrue(isNetworkHighlight(other, me, friends))
        assertTrue(isNetworkHighlight(friend, null, emptySet()))
    }
}
