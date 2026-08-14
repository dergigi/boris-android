package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteMatchTest {
    @Test
    fun occurrencesFindsExactHit() {
        assertEquals(
            listOf(6 until 11),
            QuoteMatch.occurrences("hello world", "world"),
        )
    }

    @Test
    fun occurrencesReturnsEmptyOnMiss() {
        assertTrue(QuoteMatch.occurrences("hello world", "planet").isEmpty())
    }

    @Test
    fun occurrencesReturnsTwoRangesForTwoIdenticalQuotes() {
        assertEquals(
            listOf(0 until 3, 8 until 11),
            QuoteMatch.occurrences("the cat the hat", "the"),
        )
    }

    @Test
    fun occurrencesReturnsEmptyForBlankQuote() {
        assertTrue(QuoteMatch.occurrences("hello", "   ").isEmpty())
        assertTrue(QuoteMatch.occurrences("hello", "").isEmpty())
    }
}
