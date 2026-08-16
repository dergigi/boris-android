package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZapReceiptsTest {
    @Test
    fun bolt11SatsParsesCommonMultipliers() {
        assertEquals(2_100L, ZapReceipts.bolt11Sats("lnbc21u1p3xyz"))
        assertEquals(210L, ZapReceipts.bolt11Sats("lnbc2100n1p3xyz"))
        assertEquals(100_000L, ZapReceipts.bolt11Sats("lnbc1m1p3xyz"))
        assertEquals(100_000_000L, ZapReceipts.bolt11Sats("lnbc11p3xyz"))
    }

    @Test
    fun bolt11SatsIsCaseInsensitive() {
        assertEquals(2_100L, ZapReceipts.bolt11Sats("LNBC21U1P3XYZ"))
    }

    @Test
    fun bolt11SatsReturnsNullWithoutAmount() {
        assertNull(ZapReceipts.bolt11Sats("lnbc1p3xyzqqqq"))
        assertNull(ZapReceipts.bolt11Sats("not-an-invoice"))
    }

    @Test
    fun senderPrefersUppercasePTag() {
        val zapper = "ab".repeat(32)
        val event = receipt(
            id = "1",
            tags = listOf(
                listOf("P", zapper.uppercase()),
                listOf("description", requestJson("cd".repeat(32))),
            ),
        )
        assertEquals(zapper, ZapReceipts.sender(event))
    }

    @Test
    fun senderFallsBackToZapRequestPubkey() {
        val zapper = "cd".repeat(32)
        val event = receipt(
            id = "2",
            tags = listOf(listOf("description", requestJson(zapper))),
        )
        assertEquals(zapper, ZapReceipts.sender(event))
    }

    @Test
    fun amountFallsBackToRequestAmountTag() {
        val event = receipt(
            id = "3",
            tags = listOf(
                listOf("description", requestJson("cd".repeat(32), amountMsats = 2_100_000L)),
            ),
        )
        assertEquals(2_100L, ZapReceipts.amountSats(event))
    }

    @Test
    fun supportersAggregatesFiltersAndSorts() {
        val big = "aa".repeat(32)
        val small = "bb".repeat(32)
        val legend = "cc".repeat(32)
        val events = listOf(
            zap("1", big, sats = 2_100),
            zap("2", big, sats = 1_000),
            zap("3", small, sats = 500),
            zap("4", legend, sats = 70_000),
        )
        val supporters = ZapReceipts.supporters(events)
        assertEquals(listOf(legend, big), supporters.map { it.pubkey })
        assertEquals(70_000L, supporters[0].totalSats)
        assertEquals(true, supporters[0].legend)
        assertEquals(3_100L, supporters[1].totalSats)
        assertEquals(2, supporters[1].zapCount)
        assertEquals(false, supporters[1].legend)
    }

    @Test
    fun supportersDedupesByEventId() {
        val zapper = "aa".repeat(32)
        val event = zap("1", zapper, sats = 2_100)
        assertEquals(1, ZapReceipts.supporters(listOf(event, event))[0].zapCount)
    }

    private fun zap(id: String, zapper: String, sats: Long): Nip01Event = receipt(
        id = id,
        tags = listOf(
            listOf("P", zapper),
            listOf("bolt11", "lnbc${sats * 10}n1p3xyz"),
        ),
    )

    private fun requestJson(pubkey: String, amountMsats: Long? = null): String {
        val tags = if (amountMsats == null) "" else """["amount","$amountMsats"]"""
        return """{"kind":9734,"pubkey":"$pubkey","tags":[$tags]}"""
    }

    private fun receipt(id: String, tags: List<List<String>>): Nip01Event = Nip01Event(
        id = id.padStart(64, '0'),
        pubkey = "ee".repeat(32),
        createdAt = 1,
        kind = Nip01Event.KIND_ZAP_RECEIPT,
        tags = tags,
        content = "",
        sig = "ff".repeat(32),
    )
}
