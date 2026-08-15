package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Nip84Test {
    @Test
    fun tagsIncludeRAndLockedAndroidAlt() {
        val tags = Nip84.tags("https://example.com/article", null)
        assertEquals(listOf("r", "https://example.com/article"), tags[0])
        assertEquals(listOf("alt", "Highlight created by Boris Android. readwithboris.com"), tags.last())
        assertEquals(setOf("r", "alt"), tags.map { it[0] }.toSet())
    }

    @Test
    fun tagsIncludeOptionalContext() {
        val tags = Nip84.tags("https://example.com/article", "previous. selected. next.")
        assertEquals(
            listOf(
                listOf("r", "https://example.com/article"),
                listOf("context", "previous. selected. next."),
                listOf("alt", Nip84.ALT),
            ),
            tags,
        )
        assertEquals(setOf("r", "context", "alt"), tags.map { it[0] }.toSet())
    }

    @Test
    fun createdTagsExcludeLanternSelectors() {
        val names = Nip84.tags("https://example.com/article", "surrounding sentence").map { it[0] }
        assertFalse(names.contains("textquoteselector"))
        assertFalse(names.contains("textpositionselector"))
        assertFalse(names.contains("rangeselector"))
        assertTrue(names.containsAll(listOf("r", "context", "alt")))
        assertEquals(3, names.size)
    }

    @Test
    fun articleUrlReadsHttpRTag() {
        val event = Nip01Event(
            id = "1".padStart(64, '0'),
            pubkey = "aa".repeat(32),
            createdAt = 1,
            kind = Nip01Event.KIND_HIGHLIGHT,
            tags = listOf(
                listOf("r", "wss://not-an-article"),
                listOf("r", "https://example.com/article"),
            ),
            content = "quote",
            sig = "bb".repeat(32),
        )
        assertEquals("https://example.com/article", Nip84.articleUrl(event))
    }

    @Test
    fun articleUrlMissingWhenNoHttpRTag() {
        val event = Nip01Event(
            id = "2".padStart(64, '0'),
            pubkey = "aa".repeat(32),
            createdAt = 1,
            kind = Nip01Event.KIND_HIGHLIGHT,
            tags = listOf(listOf("alt", "x")),
            content = "quote",
            sig = "bb".repeat(32),
        )
        assertNull(Nip84.articleUrl(event))
    }

    @Test
    fun extractContextReturnsNullForSingleSentence() {
        assertNull(Nip84.extractContext("only one", "This is only one sentence."))
    }

    @Test
    fun extractContextUsesNeighborSentences() {
        val body = "First sentence. Selected quote here. Third sentence."
        assertEquals(
            "First sentence. Selected quote here. Third sentence.",
            Nip84.extractContext("Selected quote here", body),
        )
    }
}
