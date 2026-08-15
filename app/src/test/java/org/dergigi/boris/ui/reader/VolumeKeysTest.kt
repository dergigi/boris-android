package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeKeysTest {
    @Test
    fun volumeDownMovesTowardTheEnd() {
        assertEquals(900, VolumeKeys.nextOffset(current = 0, max = 2000, page = 900, up = false))
        assertEquals(2000, VolumeKeys.nextOffset(current = 1800, max = 2000, page = 900, up = false))
    }

    @Test
    fun volumeUpMovesTowardTheStart() {
        assertEquals(100, VolumeKeys.nextOffset(current = 1000, max = 2000, page = 900, up = true))
        assertEquals(0, VolumeKeys.nextOffset(current = 400, max = 2000, page = 900, up = true))
    }

    @Test
    fun pageSizeUsesTheChosenPercentOfTheViewport() {
        assertEquals(900, VolumeKeys.pageSize(viewportHeight = 1000, percent = 90))
        assertEquals(250, VolumeKeys.pageSize(viewportHeight = 1000, percent = 25))
        assertEquals(1000, VolumeKeys.pageSize(viewportHeight = 1000, percent = 100))
        assertEquals(1, VolumeKeys.pageSize(viewportHeight = 0, percent = 90))
    }
}
