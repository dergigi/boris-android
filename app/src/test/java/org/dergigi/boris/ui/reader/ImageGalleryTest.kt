package org.dergigi.boris.ui.reader

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageGalleryTest {
    @Test
    fun doubleTapZoomOffsetKeepsTapUnderFinger() {
        val tap = Offset(100f, 80f)
        val scale = 2.5f
        val offset = doubleTapZoomOffset(tap, width = 400f, height = 800f, scale = scale)
        val center = Offset(200f, 400f)
        val after = center + (tap - center) * scale + offset
        assertEquals(tap.x, after.x, 0.01f)
        assertEquals(tap.y, after.y, 0.01f)
    }

    @Test
    fun doubleTapOnCenterStaysCentered() {
        val offset = doubleTapZoomOffset(Offset(200f, 400f), 400f, 800f, 2.5f)
        assertEquals(0f, offset.x, 0.01f)
        assertEquals(0f, offset.y, 0.01f)
    }
}