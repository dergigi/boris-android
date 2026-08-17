package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextFragmentTest {
    @Test
    fun appendsExactQuote() {
        assertEquals(
            "https://example.com/post#:~:text=hello%20world",
            TextFragment.apply("https://example.com/post", "hello world"),
        )
    }

    @Test
    fun encodesCommaAmpersandAndHyphen() {
        val url = TextFragment.apply("https://example.com/p", "red, white & blue-green")
        assertTrue(url.contains("red%2C%20white%20%26%20blue%2Dgreen"))
    }

    @Test
    fun keepsExistingHashAndReplacesOldDirective() {
        assertEquals(
            "https://example.com/p#intro:~:text=quote",
            TextFragment.apply("https://example.com/p#intro", "quote"),
        )
        assertEquals(
            "https://example.com/p#intro:~:text=new",
            TextFragment.apply("https://example.com/p#intro:~:text=old", "new"),
        )
    }

    @Test
    fun emptyQuoteLeavesUrlAlone() {
        assertEquals("https://example.com/p", TextFragment.apply("https://example.com/p", "   "))
    }

    @Test
    fun longQuoteUsesStartAndEnd() {
        val words = (1..80).joinToString(" ") { "word$it" }
        val value = TextFragment.fragmentValue(words)!!
        assertTrue(value.contains(","))
        assertTrue(value.startsWith("word1%20"))
        assertTrue(value.endsWith("%20word80"))
        assertFalse(value.contains("word40"))
    }
}
