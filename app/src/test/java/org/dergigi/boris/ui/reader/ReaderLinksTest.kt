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

    @Test
    fun nostrProfileLinksOpenProfiles() {
        val action = readerLinkAction(
            uri = "nostr:nprofile1qyv8wue69uhk6mmwv9jzu6nzx56jucm0d5arsvpcxqq3qamn8ghj7atdvfex2mp6xsurgwqqyzdkm9dhdgq3jxjvw7qc26q76lememf0l75wg9gka3uzgzepx2zl2ewxw6s",
            currentUrl = "nostr:naddr1qq",
            openInReader = true,
        )
        assertEquals(
            ReaderLinkAction.OpenProfile("9b6d95b76a01191a4c778185681ed7f3bced2fffa8e41516ec78240b213285f5"),
            action,
        )
    }
}
