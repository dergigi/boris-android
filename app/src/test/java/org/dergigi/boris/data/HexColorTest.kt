package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HexColorTest {
    @Test
    fun parsesSixDigitHex() {
        assertEquals(0xFFFDE047.toInt(), HexColor.argb("#fde047"))
        assertEquals(0xFF9333EA.toInt(), HexColor.argb("9333ea"))
    }

    @Test
    fun rejectsBadHex() {
        assertNull(HexColor.argb("#fff"))
        assertNull(HexColor.argb("not-a-color"))
        assertEquals(0xFF000000.toInt(), HexColor.argb("nope", 0xFF000000.toInt()))
    }
}
