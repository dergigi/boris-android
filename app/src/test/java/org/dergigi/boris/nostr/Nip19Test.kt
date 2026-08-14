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
}
