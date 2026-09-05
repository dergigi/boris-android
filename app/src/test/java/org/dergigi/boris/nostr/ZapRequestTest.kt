package org.dergigi.boris.nostr

import org.dergigi.boris.data.ReadableContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZapRequestTest {
    private val author = "aa".repeat(32)
    private val eventId = "ee".repeat(32)
    private val relays = listOf("wss://relay.one", "wss://relay.two")

    @Test
    fun longFormCarriesAddressEventAndKind() {
        val coordinate = "30023:$author:hello"
        val tags = ZapRequest.tags(
            content = ReadableContent(url = "nostr:naddr1", articleCoordinate = coordinate, eventId = eventId, authorPubkey = author),
            recipientPubkey = author.uppercase(),
            amountMsats = 21_000,
            relays = relays,
        )
        assertEquals(listOf("relays") + relays, tags[0])
        assertEquals(listOf("amount", "21000"), tags[1])
        assertEquals(listOf("p", author), tags[2])
        assertEquals(listOf("a", coordinate), tags[3])
        assertEquals(listOf("e", eventId), tags[4])
        assertEquals(listOf("k", "30023"), tags[5])
    }

    @Test
    fun noteCarriesEventAndKindOne() {
        val tags = ZapRequest.tags(
            content = ReadableContent(url = "nostr:nevent1", eventId = eventId, authorPubkey = author),
            recipientPubkey = author,
            amountMsats = 1_000,
            relays = relays,
        )
        assertTrue(tags.contains(listOf("e", eventId)))
        assertTrue(tags.contains(listOf("k", "1")))
        assertFalse(tags.any { it[0] == "a" })
    }

    @Test
    fun webPageOnlyTagsTheRecipient() {
        val tags = ZapRequest.tags(
            content = ReadableContent(url = "https://example.com", authorPubkey = author),
            recipientPubkey = author,
            amountMsats = 1_000,
            relays = relays,
        )
        assertEquals(setOf("relays", "amount", "p"), tags.map { it[0] }.toSet())
    }
}
