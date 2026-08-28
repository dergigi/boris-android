package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip19
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightShareTest {
    @Test
    fun webUrlGetsTextFragment() {
        val url = HighlightShare.url("https://example.com/essay", "a chosen line")
        assertEquals("https://example.com/essay#:~:text=a%20chosen%20line", url)
    }

    @Test
    fun stripsMarkdownBeforeEncoding() {
        val url = HighlightShare.url(
            "https://example.com/essay",
            "[a chosen line](https://ignored.example)",
        )
        assertEquals("https://example.com/essay#:~:text=a%20chosen%20line", url)
    }

    @Test
    fun nostrArticleSharesPublicUrlWithoutFragment() {
        val naddr = sampleNaddr()
        val url = HighlightShare.url("nostr:$naddr", "a chosen line")
        assertEquals(NostrLink.gatewayUrl(naddr), url)
        assertTrue(!url.contains(":~:text="))
    }

    @Test
    fun webArticleShareIsCleanUrl() {
        assertEquals(
            "https://example.com/essay",
            HighlightShare.articleUrl("https://example.com/essay", "a chosen line"),
        )
    }

    @Test
    fun nostrArticleShareUsesNjumpAndTextFragment() {
        val naddr = sampleNaddr()
        val url = HighlightShare.articleUrl("nostr:$naddr", "a chosen line")
        assertEquals("${NostrLink.gatewayUrl(naddr)}#:~:text=a%20chosen%20line", url)
    }

    @Test
    fun nostrNoteShareUsesNjumpAndTextFragment() {
        val note = Nip19.noteEncode("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        val url = HighlightShare.articleUrl("nostr:$note", "[a chosen line](https://ignored.example)")
        assertEquals("${NostrLink.gatewayUrl(note)}#:~:text=a%20chosen%20line", url)
    }

    @Test
    fun profileHasNoArticleShare() {
        val npub = Nip19.npubEncode(
            "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
        )
        assertEquals(null, HighlightShare.articleUrl("nostr:$npub", "a chosen line"))
    }

    private fun sampleNaddr(): String = Nip19.naddrEncode(
        org.dergigi.boris.nostr.NaddrPointer(
            identifier = "my-article",
            pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            kind = 30023,
        ),
    )
}
