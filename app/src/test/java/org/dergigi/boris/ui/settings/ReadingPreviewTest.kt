package org.dergigi.boris.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingPreviewTest {
    @Test
    fun sampleQuotesLiveInsideTheirParagraphs() {
        assertTrue(PreviewCopy.P1.contains(PreviewCopy.MINE))
        assertTrue(PreviewCopy.P2.contains(PreviewCopy.FRIENDS))
        assertTrue(PreviewCopy.P3.contains(PreviewCopy.NOSTRVERSE))
        assertTrue(PreviewCopy.P3.contains(PreviewCopy.LINK))
        assertTrue(PreviewCopy.P3.indexOf(PreviewCopy.LINK) < PreviewCopy.P3.indexOf(PreviewCopy.NOSTRVERSE))
    }
}
