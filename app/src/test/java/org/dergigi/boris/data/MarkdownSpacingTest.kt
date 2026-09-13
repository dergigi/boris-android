package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownSpacingTest {
    @Test
    fun movesTrailingSpaceOutsideClosingEmphasisMarker() {
        assertEquals(
            "Headlines like *“Bitcoin reaches a new all-time high”* are enough...",
            MarkdownSpacing.normalize("Headlines like *“Bitcoin reaches a new all-time high” *are enough..."),
        )
    }

    @Test
    fun separatesOpeningEmphasisMarkerFromPreviousWord() {
        assertEquals(
            "As more and more research happens *in silico*, AI can increasingly...",
            MarkdownSpacing.normalize("As more and more research happens* in silico*, AI can increasingly..."),
        )
    }

    @Test
    fun leavesCodeFencesAlone() {
        val markdown = "Before\n\n```\nhappens* in silico*\n```\n\nAfter"
        assertEquals(markdown, MarkdownSpacing.normalize(markdown))
    }
}
