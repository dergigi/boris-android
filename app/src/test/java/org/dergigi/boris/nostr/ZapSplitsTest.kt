package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZapSplitsTest {

    private val highlighter = "a".repeat(64)
    private val author = "b".repeat(64)
    private val coAuthor = "c".repeat(64)

    @Test
    fun defaultSplitTagsHighlighterBorisAndAuthor() {
        val tags = ZapSplits.tags(
            highlighterPubkey = highlighter,
            sourceAuthorPubkey = author,
            sourceZapTags = emptyList(),
            highlighterWeight = 50.0,
            borisWeight = 2.1,
            authorWeight = 50.0,
        )
        assertEquals(
            listOf(
                listOf("zap", highlighter, ZapSplits.ZAP_RELAY, "50"),
                listOf("zap", ZapSplits.BORIS_PUBKEY, ZapSplits.ZAP_RELAY, "2.1"),
                listOf("zap", author, ZapSplits.ZAP_RELAY, "50.0"),
            ),
            tags,
        )
    }

    @Test
    fun zeroWeightsAreOmitted() {
        val tags = ZapSplits.tags(
            highlighterPubkey = highlighter,
            sourceAuthorPubkey = author,
            sourceZapTags = emptyList(),
            highlighterWeight = 0.0,
            borisWeight = 0.0,
            authorWeight = 80.0,
        )
        assertEquals(listOf(listOf("zap", author, ZapSplits.ZAP_RELAY, "80.0")), tags)
    }

    @Test
    fun existingZapTagsSplitAuthorShareProportionally() {
        val sourceZapTags = listOf(
            listOf("zap", author, "wss://relay.example.com", "3"),
            listOf("zap", coAuthor, "", "1"),
        )
        val tags = ZapSplits.tags(
            highlighterPubkey = highlighter,
            sourceAuthorPubkey = author,
            sourceZapTags = sourceZapTags,
            highlighterWeight = 50.0,
            borisWeight = 2.1,
            authorWeight = 40.0,
        )
        assertEquals(
            listOf(
                listOf("zap", highlighter, ZapSplits.ZAP_RELAY, "50"),
                listOf("zap", ZapSplits.BORIS_PUBKEY, ZapSplits.ZAP_RELAY, "2.1"),
                listOf("zap", author, "wss://relay.example.com", "30.0"),
                listOf("zap", coAuthor, ZapSplits.ZAP_RELAY, "10.0"),
            ),
            tags,
        )
    }

    @Test
    fun highlighterAndBorisAreNotDoubledFromSourceTags() {
        val sourceZapTags = listOf(
            listOf("zap", highlighter, ZapSplits.ZAP_RELAY, "1"),
            listOf("zap", ZapSplits.BORIS_PUBKEY, ZapSplits.ZAP_RELAY, "1"),
            listOf("zap", coAuthor, ZapSplits.ZAP_RELAY, "2"),
        )
        val tags = ZapSplits.tags(
            highlighterPubkey = highlighter,
            sourceAuthorPubkey = author,
            sourceZapTags = sourceZapTags,
            highlighterWeight = 50.0,
            borisWeight = 2.1,
            authorWeight = 50.0,
        )
        assertEquals(1, tags.count { it[1] == highlighter })
        assertEquals(1, tags.count { it[1] == ZapSplits.BORIS_PUBKEY })
        assertEquals(listOf("zap", coAuthor, ZapSplits.ZAP_RELAY, "25.0"), tags.last())
    }

    @Test
    fun authorTagSkippedWhenAuthorIsHighlighter() {
        val tags = ZapSplits.tags(
            highlighterPubkey = highlighter,
            sourceAuthorPubkey = highlighter,
            sourceZapTags = emptyList(),
            highlighterWeight = 50.0,
            borisWeight = 2.1,
            authorWeight = 50.0,
        )
        assertEquals(2, tags.size)
        assertTrue(tags.none { it[1] == highlighter && it[3] == "50.0" })
    }

    @Test
    fun borisTagSkippedWhenBorisIsHighlighter() {
        val tags = ZapSplits.tags(
            highlighterPubkey = ZapSplits.BORIS_PUBKEY,
            sourceAuthorPubkey = author,
            sourceZapTags = emptyList(),
            highlighterWeight = 50.0,
            borisWeight = 2.1,
            authorWeight = 50.0,
        )
        assertEquals(
            listOf(
                listOf("zap", ZapSplits.BORIS_PUBKEY, ZapSplits.ZAP_RELAY, "50"),
                listOf("zap", author, ZapSplits.ZAP_RELAY, "50.0"),
            ),
            tags,
        )
    }

    @Test
    fun highlightTagsIncludeZapSplits() {
        val zapSplits = ZapSplits.tags(
            highlighterPubkey = highlighter,
            sourceAuthorPubkey = author,
            sourceZapTags = emptyList(),
            highlighterWeight = 50.0,
            borisWeight = 2.1,
            authorWeight = 50.0,
        )
        val tags = Nip84.tags(
            url = "nostr:naddr1example",
            context = null,
            coordinate = "30023:$author:example",
            eventId = "e".repeat(64),
            authorPubkey = author,
            zapSplits = zapSplits,
        )
        assertEquals(3, tags.count { it[0] == "zap" })
        assertEquals(zapSplits, tags.takeLast(3))
    }
}
