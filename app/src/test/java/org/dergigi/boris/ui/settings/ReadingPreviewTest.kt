package org.dergigi.boris.ui.settings

import androidx.compose.ui.unit.dp
import org.dergigi.boris.data.ReaderMargin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingPreviewTest {
    @Test
    fun sampleQuotesLiveInsideTheirParagraphs() {
        assertTrue(PreviewCopy.P1.contains(PreviewCopy.MINE))
        assertTrue(PreviewCopy.P2.contains(PreviewCopy.FRIENDS))
        assertTrue(PreviewCopy.P_FOAF.contains(PreviewCopy.FOAF))
        assertTrue(PreviewCopy.P3.contains(PreviewCopy.NOSTRVERSE))
        assertTrue(PreviewCopy.P3.contains(PreviewCopy.LINK))
        assertTrue(PreviewCopy.P3.indexOf(PreviewCopy.LINK) < PreviewCopy.P3.indexOf(PreviewCopy.NOSTRVERSE))
    }

    @Test
    fun previewMarginsMatchReaderMargins() {
        assertEquals(8.dp, readingPreviewHorizontalPadding(ReaderMargin.Compact))
        assertEquals(20.dp, readingPreviewHorizontalPadding(ReaderMargin.Default))
        assertEquals(32.dp, readingPreviewHorizontalPadding(ReaderMargin.Comfortable))
    }
}
