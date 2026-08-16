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
}
