package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Test

class Nip19Test {
    @Test
    fun roundTripsOfficialNpubVector() {
        val hex = "7e7e9c42a91bfef19fa929e5fda1b72e0ebc1a4c1141673e2794234d86addf4e"
        val npub = "npub10elfcs4fr0l0r8af98jlmgdh9c8tcxjvz9qkw038js35mp4dma8qzvjptg"
        assertEquals(npub, Nip19.npubEncode(hex))
        assertEquals(hex, Nip19.npubDecode(npub))
    }

    @Test
    fun roundTripsSecondOfficialNpub() {
        val hex = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        assertEquals(npub, Nip19.npubEncode(hex))
        assertEquals(hex, Nip19.npubDecode(npub))
    }

    @Test
    fun normalizeAcceptsHexAndNpub() {
        val hex = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        assertEquals(hex, Nip19.normalizePubkey(hex.uppercase()))
        assertEquals(hex, Nip19.normalizePubkey(npub))
    }

    @Test
    fun decodesBareNaddrFromNostrTools() {
        val naddr = "naddr1qvzqqqr4gupzqwlsccluhy6xxsr6l9a9uhhxf75g85g8a709tprjcn4e42h053vaqq9x67fdv9e8g6trd3jsrnn0q2"
        val pointer = Nip19.naddrDecode(naddr)
        assertEquals("my-article", pointer.identifier)
        assertEquals("3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d", pointer.pubkey)
        assertEquals(30023, pointer.kind)
        assertEquals(emptyList<String>(), pointer.relays)
        assertEquals(
            "30023:3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d:my-article",
            pointer.coordinate,
        )
    }

    @Test
    fun decodesNaddrWithRelayHint() {
        val naddr = "naddr1qvzqqqr4gupzqwlsccluhy6xxsr6l9a9uhhxf75g85g8a709tprjcn4e42h053vaqy28wumn8ghj7un9d3shjtnyv9kh2uewd9hsqznd0ykkzun5d93kceg3p9se6"
        val pointer = Nip19.naddrDecode(naddr)
        assertEquals("my-article", pointer.identifier)
        assertEquals(listOf("wss://relay.damus.io"), pointer.relays)
    }

    @Test
    fun naddrRoundTrips() {
        val original = NaddrPointer(
            identifier = "my-article",
            pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            kind = 30023,
        )
        val decoded = Nip19.naddrDecode(Nip19.naddrEncode(original))
        assertEquals(original.identifier, decoded.identifier)
        assertEquals(original.pubkey, decoded.pubkey)
        assertEquals(original.kind, decoded.kind)
    }
}
