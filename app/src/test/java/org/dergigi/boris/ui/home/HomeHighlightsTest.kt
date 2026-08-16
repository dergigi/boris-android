package org.dergigi.boris.ui.home

import org.dergigi.boris.data.OgPreview
import org.junit.Assert.assertEquals
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

    @Test
    fun mergePreviewKeepsCachedCoverWhenFetchHasNoImage() {
        val cached = OgPreview("Cached", "https://cdn.example/cover.png", "Example")
        val fetched = OgPreview("Fresh", null, "Example")
        val merged = mergePreview(cached, fetched)
        assertEquals("Fresh", merged?.title)
        assertEquals("https://cdn.example/cover.png", merged?.imageUrl)
    }
}
