package org.dergigi.boris.nostr

import org.dergigi.boris.data.ReadableContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZapRecipientsTest {
    private val author = "aa".repeat(32)
    private val alice = "bb".repeat(32)
    private val bob = "cc".repeat(32)

    @Test
    fun fallsBackToTheAuthor() {
        val targets = ZapRecipients.targets(ReadableContent(url = "https://x", authorPubkey = author.uppercase()))
        assertEquals(listOf(ZapTarget(author, null, 1.0)), targets)
    }

    @Test
    fun noAuthorMeansNoTargets() {
        assertTrue(ZapRecipients.targets(ReadableContent(url = "https://x")).isEmpty())
        assertTrue(ZapRecipients.targets(ReadableContent(url = "https://x", authorPubkey = "nope")).isEmpty())
    }

    @Test
    fun zapTagsWinOverTheAuthorAndKeepWeights() {
        val content = ReadableContent(
            url = "nostr:naddr1",
            authorPubkey = author,
            sourceZapTags = listOf(
                listOf("zap", alice, "wss://relay.one", "3"),
                listOf("zap", bob, "", "1"),
                listOf("zap", alice, "wss://dupe", "9"),
                listOf("zap", "not-a-key", "", "5"),
            ),
        )
        assertEquals(
            listOf(ZapTarget(alice, "wss://relay.one", 3.0), ZapTarget(bob, null, 1.0)),
            ZapRecipients.targets(content),
        )
    }

    @Test
    fun zapTagWithoutWeightCountsAsOne() {
        val content = ReadableContent(url = "x", sourceZapTags = listOf(listOf("zap", alice)))
        assertEquals(1.0, ZapRecipients.targets(content).single().weight, 0.0)
    }

    @Test
    fun sharesSumToTheTotalWithRemainderOnTheLargest() {
        assertEquals(listOf(67L, 33L), ZapRecipients.shares(100, listOf(2.0, 1.0)))
        assertEquals(listOf(7L, 7L, 7L), ZapRecipients.shares(21, listOf(1.0, 1.0, 1.0)))
        assertEquals(listOf(8L, 7L, 7L), ZapRecipients.shares(22, listOf(1.0, 1.0, 1.0)))
        assertEquals(listOf(21L), ZapRecipients.shares(21, listOf(50.0)))
    }

    @Test
    fun tinySharesRoundDownToZero() {
        assertEquals(listOf(2L, 0L), ZapRecipients.shares(2, listOf(99.0, 1.0)))
        assertEquals(listOf(0L, 0L), ZapRecipients.shares(0, listOf(1.0, 1.0)))
        assertEquals(listOf(0L), ZapRecipients.shares(10, listOf(0.0)))
    }
}
