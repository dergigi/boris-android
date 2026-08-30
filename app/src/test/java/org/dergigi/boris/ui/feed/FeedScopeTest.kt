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
        assertFalse(scope.foaf)
        assertTrue(scope.visible(FeedLevel.Nostrverse))
        assertFalse(scope.visible(FeedLevel.Friends))
        assertFalse(scope.visible(FeedLevel.Mine))
        assertFalse(scope.visible(FeedLevel.Foaf))
    }

    @Test
    fun settingsDefaultsMatchWebappExplore() {
        val scope = FeedScope.fromSettings(UserSettings.defaults())
        assertFalse(scope.nostrverse)
        assertTrue(scope.friends)
        assertFalse(scope.mine)
        assertFalse(scope.foaf)
    }

    @Test
    fun withExploreScopeWritesTheSameKeysAsTheWebapp() {
        val next = UserSettings.defaults().withExploreScope(
            FeedScope(nostrverse = true, friends = false, mine = true),
        )
        assertTrue(next.defaultExploreScopeNostrverse)
        assertFalse(next.defaultExploreScopeFriends)
        assertTrue(next.defaultExploreScopeMine)
        assertEquals(
            FeedScope(nostrverse = true, friends = false, mine = true),
            FeedScope.fromSettings(next),
        )
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

    @Test
    fun classifyPrefersFriendsOverFoaf() {
        val me = "aa".repeat(32)
        val friend = "bb".repeat(32)
        val foaf = "cc".repeat(32)
        val stranger = "dd".repeat(32)
        val friends = setOf(friend)
        val foafs = setOf(friend, foaf)
        assertEquals(FeedLevel.Friends, classifyFeedLevel(friend, me, friends, foafs))
        assertEquals(FeedLevel.Foaf, classifyFeedLevel(foaf.uppercase(), me, friends, foafs))
        assertEquals(FeedLevel.Nostrverse, classifyFeedLevel(stranger, me, friends, foafs))
    }

    @Test
    fun foafPubkeysDropsSelfAndFriends() {
        val me = "aa".repeat(32)
        val friend = "bb".repeat(32)
        val hop = "cc".repeat(32)
        val contacts = mapOf(
            friend to setOf(me, hop, friend.uppercase()),
        )
        assertEquals(
            setOf(hop),
            foafPubkeys(me, setOf(friend)) { hex -> contacts[hex].orEmpty() },
        )
    }

    @Test
    fun foafFetchAuthorsCapsDeterministically() {
        val keys = (1..100).map { it.toString().padStart(64, '0') }.toSet()
        val capped = foafFetchAuthors(keys, cap = 80)
        assertEquals(80, capped.size)
        assertEquals(keys.sorted().take(80).toSet(), capped)
    }
}
