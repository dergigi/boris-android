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
}
