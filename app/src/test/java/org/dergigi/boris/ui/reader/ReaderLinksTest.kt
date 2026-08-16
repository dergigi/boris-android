package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderLinksTest {
    @Test
    fun openInReaderSendsHttpLinksToTheReader() {
        val action = readerLinkAction(
            uri = "https://example.com/next",
            currentUrl = "https://example.com/now",
            openInReader = true,
        )
        assertEquals(ReaderLinkAction.OpenInReader("https://example.com/next"), action)
    }

    @Test
    fun closedSettingSendsHttpLinksOutside() {
        val action = readerLinkAction(
            uri = "https://example.com/next",
            currentUrl = "https://example.com/now",
            openInReader = false,
        )
        assertEquals(ReaderLinkAction.OpenExternal("https://example.com/next"), action)
    }

    @Test
    fun sameArticleLinkIsIgnored() {
        val url = "https://example.com/now"
        assertEquals(ReaderLinkAction.Ignore, readerLinkAction(url, url, openInReader = true))
        assertEquals(ReaderLinkAction.Ignore, readerLinkAction(url, url, openInReader = false))
    }

    @Test
    fun settingsWeblinksFollowTheSameSwitch() {
        assertEquals(
            ReaderLinkAction.OpenInReader("https://nostr.how/en/relays"),
            readerLinkAction("https://nostr.how/en/relays", "", openInReader = true),
        )
        assertEquals(
            ReaderLinkAction.OpenExternal("https://nostr.how/en/relays"),
            readerLinkAction("https://nostr.how/en/relays", "", openInReader = false),
        )
    }

    @Test
    fun mailtoAlwaysGoesOutside() {
        val action = readerLinkAction(
            uri = "mailto:hi@example.com",
            currentUrl = "https://example.com/now",
            openInReader = true,
        )
        assertEquals(ReaderLinkAction.OpenExternal("mailto:hi@example.com"), action)
    }
}
