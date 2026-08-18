package org.dergigi.boris.nostr

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class HintedRelaysTest {
    private val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
    private lateinit var file: File

    @Before
    fun setUp() {
        file = File.createTempFile("hinted_relays", ".json")
        HintedRelays.clear()
        HintedRelays.init(file)
    }

    @After
    fun tearDown() {
        HintedRelays.clear()
        file.delete()
    }

    @Test
    fun rememberThenForPubkey() {
        HintedRelays.remember(pubkey, listOf("wss://r.x.com"))
        assertEquals(listOf("wss://r.x.com"), HintedRelays.forPubkey(pubkey))
    }

    @Test
    fun unionMergesDistinctHints() {
        HintedRelays.remember(pubkey, listOf("wss://r.x.com"))
        HintedRelays.remember(pubkey, listOf("wss://djbas.sadkb.com"))
        assertEquals(
            listOf("wss://r.x.com", "wss://djbas.sadkb.com"),
            HintedRelays.forPubkey(pubkey),
        )
    }

    @Test
    fun rejectedUrlDroppedWhileGoodHintRemains() {
        HintedRelays.remember(
            pubkey,
            listOf("https://evil.example", "wss://good.example", "not a relay"),
        )
        assertEquals(listOf("wss://good.example"), HintedRelays.forPubkey(pubkey))
    }

    @Test
    fun capsHintsAtMaxPerPubkey() {
        val extras = (0 until HintedRelays.MAX_HINTS + 1).map { "wss://r$it.example" }
        HintedRelays.remember(pubkey, extras)
        val stored = HintedRelays.forPubkey(pubkey)
        assertEquals(HintedRelays.MAX_HINTS, stored.size)
        assertEquals(extras.takeLast(HintedRelays.MAX_HINTS), stored)
        assertTrue("wss://r0.example" !in stored)
    }

    @Test
    fun reloadsFromSameFileAfterClear() {
        HintedRelays.remember(pubkey, listOf("wss://r.x.com", "wss://djbas.sadkb.com"))
        HintedRelays.clear()
        assertEquals(emptyList<String>(), HintedRelays.forPubkey(pubkey))
        HintedRelays.init(file)
        assertEquals(
            listOf("wss://r.x.com", "wss://djbas.sadkb.com"),
            HintedRelays.forPubkey(pubkey),
        )
    }
}
