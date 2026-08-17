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
        val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val naddr = Nip19.naddrEncode(
            org.dergigi.boris.nostr.NaddrPointer(
                identifier = "my-article",
                pubkey = pubkey,
                kind = 30023,
            ),
        )
        val url = HighlightShare.url("nostr:$naddr", "a chosen line")
        assertEquals(NostrLink.gatewayUrl(naddr), url)
        assertTrue(!url.contains(":~:text="))
    }
}
