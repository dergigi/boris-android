package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderMetaTest {
    @Test
    fun readingTimeUsesWordCount() {
        val text = List(200) { "word" }.joinToString(" ")
        assertEquals("1 min read", readingTimeLabel(text))
        assertEquals("2 min read", readingTimeLabel(text + " " + List(200) { "more" }.joinToString(" ")))
    }

    @Test
    fun readingTimeEmptyIsNull() {
        assertNull(readingTimeLabel(""))
        assertNull(readingTimeLabel("   "))
    }

    @Test
    fun highlightCountHidesZero() {
        assertNull(highlightCountLabel(0))
        assertEquals("1 highlight", highlightCountLabel(1))
        assertEquals("9 highlights", highlightCountLabel(9))
    }
}
