package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NostrArticleTest {
    private val naddr =
        "naddr1qvzqqqr4gupzqwlsccluhy6xxsr6l9a9uhhxf75g85g8a709tprjcn4e42h053vaqq9x67fdv9e8g6trd3jsrnn0q2"

    @Test
    fun parsesBareNaddr() {
        val article = NostrArticle.parse(naddr)!!
        assertEquals(naddr, article.naddr)
        assertEquals("nostr:$naddr", article.uri)
        assertEquals("my-article", article.pointer.identifier)
    }

    @Test
    fun parsesNostrUriAndWebGateways() {
        assertEquals(naddr, NostrArticle.parse("nostr:$naddr")?.naddr)
        assertEquals(naddr, NostrArticle.parse("https://njump.to/$naddr")?.naddr)
        assertEquals(naddr, NostrArticle.parse("https://readwithboris.com/a/$naddr")?.naddr)
        assertEquals(
            naddr,
            NostrArticle.parse("Check this: https://njump.to/$naddr extra")?.naddr,
        )
    }

    @Test
    fun ignoresNonArticleNaddrs() {
        val pointer = org.dergigi.boris.nostr.NaddrPointer(
            identifier = "draft",
            pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            kind = 30024,
        )
        val encoded = org.dergigi.boris.nostr.Nip19.naddrEncode(pointer)
        assertNull(NostrArticle.parse(encoded))
    }

    @Test
    fun fromCoordinateRoundTrips() {
        val coordinate =
            "30023:3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d:my-article"
        val article = NostrArticle.fromCoordinate(coordinate)!!
        assertEquals("my-article", article.pointer.identifier)
        assertEquals(coordinate, article.coordinate)
    }
}
