package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RssDatesTest {
    @Test
    fun parsesRfc822PubDate() {
        assertEquals(
            1755302400L,
            RssDates.parseSeconds("Sat, 16 Aug 2025 00:00:00 GMT"),
        )
        assertEquals(
            1755295200L,
            RssDates.parseSeconds("Sat, 16 Aug 2025 00:00:00 +0200"),
        )
    }

    @Test
    fun parsesIsoDates() {
        assertEquals(1755302400L, RssDates.parseSeconds("2025-08-16T00:00:00Z"))
        assertEquals(1755295200L, RssDates.parseSeconds("2025-08-16T00:00:00+02:00"))
    }

    @Test
    fun rejectsGarbage() {
        assertNull(RssDates.parseSeconds(null))
        assertNull(RssDates.parseSeconds(""))
        assertNull(RssDates.parseSeconds("yesterday"))
    }
}
