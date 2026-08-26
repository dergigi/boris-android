package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderSwipeTest {
    @Test
    fun leftSwipeOpensHighlights() {
        assertEquals(
            ReaderSwipeTarget.Highlights,
            ReaderSwipe.target(totalX = -120f, totalY = 12f, thresholdPx = 72f),
        )
    }

    @Test
    fun rightSwipeOpensContents() {
        assertEquals(
            ReaderSwipeTarget.Contents,
            ReaderSwipe.target(totalX = 120f, totalY = 12f, thresholdPx = 72f),
        )
    }

    @Test
    fun shortSwipeIsIgnored() {
        assertNull(ReaderSwipe.target(totalX = 60f, totalY = 0f, thresholdPx = 72f))
    }

    @Test
    fun diagonalScrollIsIgnored() {
        assertNull(ReaderSwipe.target(totalX = 90f, totalY = 80f, thresholdPx = 72f))
    }
}
