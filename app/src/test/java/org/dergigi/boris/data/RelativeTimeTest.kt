package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {
    @Test
    fun compactLabels() {
        val now = 1_700_000_000L
        assertEquals("now", RelativeTime.label(now, now))
        assertEquals("now", RelativeTime.label(now - 59, now))
        assertEquals("1m", RelativeTime.label(now - 60, now))
        assertEquals("59m", RelativeTime.label(now - 3_599, now))
        assertEquals("1h", RelativeTime.label(now - 3_600, now))
        assertEquals("23h", RelativeTime.label(now - 86_399, now))
        assertEquals("1d", RelativeTime.label(now - 86_400, now))
        assertEquals("29d", RelativeTime.label(now - 29 * 86_400, now))
        assertEquals("1mo", RelativeTime.label(now - 2_592_000, now))
        assertEquals("11mo", RelativeTime.label(now - 11 * 2_592_000, now))
        assertEquals("1y", RelativeTime.label(now - 31_536_000, now))
    }
}
