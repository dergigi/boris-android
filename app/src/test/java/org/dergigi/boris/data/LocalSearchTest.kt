package org.dergigi.boris.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSearchTest {
    @Test
    fun normalizeCollapsesWhitespaceAndCase() {
        assertTrue(LocalSearch.normalize("  Hello   World ") == "hello world")
    }

    @Test
    fun matchesFindsNeedleInAnyHaystack() {
        assertTrue(LocalSearch.matches("boris", "Read with Boris", null))
        assertTrue(LocalSearch.matches("quote", null, "a longer quote here"))
        assertFalse(LocalSearch.matches("zzz", "hello", "world"))
        assertFalse(LocalSearch.matches("", "anything"))
        assertFalse(LocalSearch.matches("a", null, "  "))
    }

    @Test
    fun hitMatchesKeepsRefinementsAndDropsUnrelatedQueries() {
        val bookmark = LocalSearch.Hit.Bookmark(
            id = "bm:1",
            title = "The Bitcoin Standard",
            subtitle = "saifedean.com",
            sortAt = 1,
            url = "https://saifedean.com/tbs",
        )
        assertTrue(LocalSearch.hitMatches(bookmark, "bit"))
        assertTrue(LocalSearch.hitMatches(bookmark, "bitcoin"))
        assertFalse(LocalSearch.hitMatches(bookmark, "nostr"))
        assertFalse(LocalSearch.hitMatches(bookmark, "b"))
    }
}
