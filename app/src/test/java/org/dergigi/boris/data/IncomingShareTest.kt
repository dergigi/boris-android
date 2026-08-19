package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingShareTest {
    @Test
    fun urlOnlySelectionOpensTheArticle() {
        val share = IncomingShares.fromProcessText("https://example.com/article")
        assertEquals("https://example.com/article", share.url)
        assertNull(share.highlightQuote)
    }

    @Test
    fun quoteBecomesAHighlight() {
        val share = IncomingShares.fromProcessText("Taste is all that's left.")
        assertEquals("Taste is all that's left.", share.highlightQuote)
        assertNull(share.url)
    }

    @Test
    fun originatingPageUrlAttachesToTheQuote() {
        val share = IncomingShares.fromProcessText(
            "Taste is all that's left.",
            originatingUrl = "https://dergigi.com/taste",
        )
        assertEquals("Taste is all that's left.", share.highlightQuote)
        assertEquals("https://dergigi.com/taste", share.url)
    }

    @Test
    fun androidAppReferrerIsIgnored() {
        val share = IncomingShares.fromProcessText(
            "A quote",
            originatingUrl = "android-app://org.chromium.chrome",
        )
        assertEquals("A quote", share.highlightQuote)
        assertNull(share.url)
    }

    @Test
    fun firstPageUrlSkipsAppReferrers() {
        assertEquals(
            "https://example.com/article",
            IncomingShares.firstPageUrl(
                listOf(
                    "android-app://org.chromium.chrome",
                    "Selected text without a URL",
                    "https://example.com/article",
                ),
            ),
        )
    }

    @Test
    fun emptySelectionIsEmpty() {
        val share = IncomingShares.fromProcessText("   ")
        assertNull(share.url)
        assertNull(share.highlightQuote)
    }
}
