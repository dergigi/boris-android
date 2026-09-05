package org.dergigi.boris.ui.reader

import androidx.compose.ui.unit.dp
import org.dergigi.boris.data.ReaderMargin
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderMarginsTest {
    @Test
    fun presetsMapToHorizontalPadding() {
        assertEquals(8.dp, readerHorizontalPadding(ReaderMargin.Compact))
        assertEquals(20.dp, readerHorizontalPadding(ReaderMargin.Default))
        assertEquals(32.dp, readerHorizontalPadding(ReaderMargin.Comfortable))
    }
}
