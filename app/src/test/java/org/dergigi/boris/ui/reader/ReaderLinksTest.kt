package org.dergigi.boris.ui.reader

import org.dergigi.boris.nostr.HintedRelays
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ReaderLinksTest {
    private lateinit var hintedFile: File

    @Before
    fun setUp() {
        hintedFile = File.createTempFile("hinted_relays", ".json")
        HintedRelays.clear()
        HintedRelays.init(hintedFile)
    }

    @After
    fun tearDown() {
        HintedRelays.clear()
        hintedFile.delete()
    }
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
    fun nprofileAndNpubOpenProfile() {
        val nprofile =
            "nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p"
        val hex = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        assertEquals(
            ReaderLinkAction.OpenProfile(hex),
            readerLinkAction("nostr:$nprofile", "https://example.com", openInReader = true),
        )
        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        assertEquals(
            ReaderLinkAction.OpenProfile(hex),
            readerLinkAction("nostr:$npub", "https://example.com", openInReader = false),
        )
    }

    @Test
    fun openProfileRemembersNprofileRelayHints() {
        val nprofile =
            "nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p"
        val hex = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val action = readerLinkAction("nostr:$nprofile", "https://example.com", openInReader = true)
        assertEquals(ReaderLinkAction.OpenProfile(hex), action)
        assertTrue(HintedRelays.forPubkey(hex).isNotEmpty())
        assertEquals(
            listOf("wss://r.x.com", "wss://djbas.sadkb.com"),
            HintedRelays.forPubkey(hex),
        )
    }

    @Test
    fun naddrStillOpensInReaderWhenEnabled() {
        val naddr =
            "naddr1qqwhwmmjw35xcetnwvkk6mmwv4uj6arfd4jkcetnwvkkzun595pzq634npfz8rwfq2hdr8am76s9t7dt7gwpe2y3t5wyufl4phe09yxeqvzqqqr4gu7cgak5"
        val action = readerLinkAction("nostr:$naddr", "https://example.com", openInReader = true)
        assertEquals(ReaderLinkAction.OpenInReader("nostr:$naddr"), action)
    }

    @Test
    fun secretKeyUriIsNotOpenProfile() {
        val action = readerLinkAction(
            uri = "nostr:nsec1vl029mgpspedva04g90vltkh6fvh240zqtv9k0t9af8935ke9laqsnlfe5",
            currentUrl = "https://example.com",
            openInReader = true,
        )
        assertEquals(
            ReaderLinkAction.OpenExternal(
                "nostr:nsec1vl029mgpspedva04g90vltkh6fvh240zqtv9k0t9af8935ke9laqsnlfe5",
            ),
            action,
        )
    }
}
