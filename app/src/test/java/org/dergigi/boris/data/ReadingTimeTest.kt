package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingTimeTest {
    @Test
    fun minutesFromWordCount() {
        val minute = List(200) { "word" }.joinToString(" ")
        assertEquals(1, ReadingTime.minutes(minute))
        assertEquals(5, ReadingTime.minutes(List(1000) { "word" }.joinToString(" ")))
        assertEquals(15, ReadingTime.minutes(List(3000) { "word" }.joinToString(" ")))
    }

    @Test
    fun blankTextHasNoMinutes() {
        assertNull(ReadingTime.minutes(""))
        assertNull(ReadingTime.minutes("   "))
        assertNull(ReadingTime.labelFor(""))
    }

    @Test
    fun labelMatchesReader() {
        assertEquals("1 min read", ReadingTime.label(1))
        assertEquals("5 min read", ReadingTime.label(5))
    }
}
