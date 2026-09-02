package org.dergigi.boris.nostr

import org.dergigi.boris.ui.feed.FeedLevel
import org.dergigi.boris.ui.feed.classifyFeedLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SocialGraphsTest {
    @Before
    fun setUp() {
        EventCache.clear()
    }

    @Test
    fun cachedWithoutPubkeyIsFallbackOnly() {
        val graph = SocialGraphs.cached(null)
        assertEquals(null, graph.pubkey)
        assertEquals(RelayList.FALLBACK, graph.relays)
        assertTrue(graph.friends.isEmpty())
        assertTrue(graph.foaf.isEmpty())
    }

    @Test
    fun cachedReadsFriendsAndFoafFromEventCache() {
        EventCache.put(
            contacts(
                id = "me-contacts",
                pubkey = ME,
                follows = listOf(FRIEND),
            ),
        )
        EventCache.put(
            contacts(
                id = "friend-contacts",
                pubkey = FRIEND,
                follows = listOf(FOAF, ME),
            ),
        )
        val graph = SocialGraphs.cached(ME)
        assertEquals(ME, graph.pubkey)
        assertEquals(RelayList.FALLBACK, graph.relays)
        assertEquals(setOf(FRIEND), graph.friends)
        assertEquals(setOf(FOAF), graph.foaf)
        assertEquals(FeedLevel.Friends, classifyFeedLevel(FRIEND, graph))
        assertEquals(FeedLevel.Foaf, classifyFeedLevel(FOAF, graph))
        assertEquals(FeedLevel.Mine, classifyFeedLevel(ME, graph))
        assertEquals(FeedLevel.Nostrverse, classifyFeedLevel(STRANGER, graph))
    }

    private fun contacts(id: String, pubkey: String, follows: List<String>): Nip01Event =
        Nip01Event(
            id = id,
            pubkey = pubkey,
            createdAt = 1_000,
            kind = Nip01Event.KIND_CONTACTS,
            tags = follows.map { listOf("p", it) },
            content = "",
            sig = "",
        )

    companion object {
        private val ME = "aa".repeat(32)
        private val FRIEND = "bb".repeat(32)
        private val FOAF = "cc".repeat(32)
        private val STRANGER = "dd".repeat(32)
    }
}
