package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteSizeTest {
    @Test
    fun formatsHumanReadableSizes() {
        assertEquals("0 B", ByteSize.format(0))
        assertEquals("512 B", ByteSize.format(512))
        assertEquals("1 KB", ByteSize.format(1024))
        assertEquals("12 KB", ByteSize.format(12 * 1024L))
        assertEquals("1.5 MB", ByteSize.format((1.5 * 1024 * 1024).toLong()))
        assertEquals("48 MB", ByteSize.format(48L * 1024 * 1024))
        assertEquals("1.0 GB", ByteSize.format(1024L * 1024 * 1024))
    }
}
