package org.dergigi.boris.nostr

import org.dergigi.boris.data.NostrArticle
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
    fun articleUrlReadsLongFormATag() {
        val coordinate =
            "30023:3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d:my-article"
        val event = Nip01Event(
            id = "3".padStart(64, '0'),
            pubkey = "aa".repeat(32),
            createdAt = 1,
            kind = Nip01Event.KIND_HIGHLIGHT,
            tags = listOf(listOf("a", coordinate)),
            content = "quote",
            sig = "bb".repeat(32),
        )
        val url = Nip84.articleUrl(event)
        assertEquals("nostr:", url?.take(6))
        assertEquals("my-article", NostrArticle.parse(url)?.pointer?.identifier)
    }

    @Test
    fun tagsForNostrArticleUseAddressPointer() {
        val coordinate =
            "30023:3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d:my-article"
        val tags = Nip84.tags(
            url = "nostr:naddr1qq",
            context = null,
            coordinate = coordinate,
            eventId = "ab".repeat(32),
            authorPubkey = "cd".repeat(32),
        )
        assertEquals(listOf("a", coordinate), tags[0])
        assertEquals(listOf("e", "ab".repeat(32)), tags[1])
        assertEquals(listOf("p", "cd".repeat(32)), tags[2])
        assertEquals(listOf("alt", Nip84.ALT), tags.last())
        assertFalse(tags.any { it[0] == "r" })
    }

    @Test
    fun tagsForWebArticleKeepUrlWhenAlternateNaddrIsPresent() {
        val coordinate =
            "30023:3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d:my-article"
        val tags = Nip84.tags(
            url = "https://dergigi.com/post",
            context = null,
            coordinate = coordinate,
            authorPubkey = "cd".repeat(32),
        )
        assertEquals(listOf("a", coordinate), tags[0])
        assertEquals(listOf("p", "cd".repeat(32)), tags[1])
        assertEquals(listOf("r", "https://dergigi.com/post"), tags[2])
    }

    @Test
    fun tagsForNotesUseEventPointer() {
        val eventId = "ab".repeat(32)
        val author = "cd".repeat(32)
        val tags = Nip84.tags(
            url = "nostr:note1qq",
            context = null,
            eventId = eventId,
            authorPubkey = author,
        )
        assertEquals(listOf("e", eventId), tags[0])
        assertEquals(listOf("p", author), tags[1])
        assertFalse(tags.any { it[0] == "r" || it[0] == "a" })
    }

    @Test
    fun articleUrlReadsNoteETag() {
        val eventId = "d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f"
        val event = Nip01Event(
            id = "4".padStart(64, '0'),
            pubkey = "aa".repeat(32),
            createdAt = 1,
            kind = Nip01Event.KIND_HIGHLIGHT,
            tags = listOf(listOf("e", eventId)),
            content = "quote",
            sig = "bb".repeat(32),
        )
        assertEquals("nostr:${Nip19.noteEncode(eventId)}", Nip84.articleUrl(event))
    }

    @Test
    fun extractContextFallsBackToAWindowForSingleSentence() {
        val body = "This is only one sentence."
        val context = Nip84.extractContext("only one", body)
        assertTrue(context != null && context.contains("only one"))
    }

    @Test
    fun extractContextUsesNeighborSentences() {
        val body = "First sentence. Selected quote here. Third sentence."
        assertEquals(
            "First sentence. Selected quote here. Third sentence.",
            Nip84.extractContext("Selected quote here", body),
        )
    }

    @Test
    fun extractContextPrefersTheSelectedOccurrence() {
        val body = "Alpha uses the word. Beta uses the word. Gamma uses the word."
        val first = body.indexOf("the word")
        val second = body.indexOf("the word", first + 1)
        val firstContext = Nip84.extractContext("the word", body, first)
        val secondContext = Nip84.extractContext("the word", body, second)
        assertTrue(secondContext != null && secondContext.contains("Beta"))
        assertTrue(firstContext != secondContext)
    }

    @Test
    fun locateSelectionUsesOwnerOffset() {
        val body = "The cat sat. The cat ran."
        val owner = "The cat sat. The cat ran."
        val second = owner.lastIndexOf("cat")
        assertEquals(
            second,
            Nip84.locateSelection(body, "cat", ownerText = owner, ownerOffset = second),
        )
    }
}
