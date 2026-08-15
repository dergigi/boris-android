package org.dergigi.boris.ui.feed

import org.dergigi.boris.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedScopeTest {
    @Test
    fun toggleKeepsAtLeastOneScopeOn() {
        val onlyFriends = FeedScope(nostrverse = false, friends = true, mine = false)
        assertEquals(onlyFriends, onlyFriends.toggle(FeedLevel.Friends))
        val next = onlyFriends.toggle(FeedLevel.Mine)
        assertTrue(next.friends)
        assertTrue(next.mine)
        assertFalse(next.nostrverse)
    }

    @Test
    fun loggedOutDefaultsToNostrverseOnly() {
        val scope = FeedScope.LOGGED_OUT
        assertTrue(scope.nostrverse)
        assertFalse(scope.friends)
        assertFalse(scope.mine)
        assertTrue(scope.visible(FeedLevel.Nostrverse))
        assertFalse(scope.visible(FeedLevel.Friends))
        assertFalse(scope.visible(FeedLevel.Mine))
    }

    @Test
    fun settingsDefaultsMatchWebappExplore() {
        val scope = FeedScope.fromSettings(UserSettings.defaults())
        assertFalse(scope.nostrverse)
        assertTrue(scope.friends)
        assertFalse(scope.mine)
    }

    @Test
    fun classifyPrefersMineOverFriends() {
        val me = "aa".repeat(32)
        val friend = "bb".repeat(32)
        val other = "cc".repeat(32)
        val friends = setOf(me, friend)
        assertEquals(FeedLevel.Mine, classifyFeedLevel(me, me, friends))
        assertEquals(FeedLevel.Friends, classifyFeedLevel(friend.uppercase(), me, friends))
        assertEquals(FeedLevel.Nostrverse, classifyFeedLevel(other, me, friends))
        assertEquals(FeedLevel.Nostrverse, classifyFeedLevel(friend, null, emptySet()))
    }
}
