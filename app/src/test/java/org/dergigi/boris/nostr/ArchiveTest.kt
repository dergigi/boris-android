package org.dergigi.boris.nostr

import org.dergigi.boris.data.ReadableContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveTest {
    private val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
    private val eventId = "aa".repeat(32)
    private val coordinate = "30023:$pubkey:my-article"

    @Test
    fun onlyBooksEmojiCounts() {
        assertTrue(Archive.isArchive(reaction("📚", Nip01Event.KIND_REACTION)))
        assertTrue(Archive.isArchive(reaction("📚", Nip01Event.KIND_URL_REACTION)))
        assertFalse(Archive.isArchive(reaction("👀", Nip01Event.KIND_REACTION)))
        assertFalse(Archive.isArchive(reaction("📚", Nip01Event.KIND_HIGHLIGHT)))
    }

    @Test
    fun normalizeUrlDropsHashAndTrailingSlash() {
        assertEquals(
            "https://example.com/path?q=1",
            Archive.normalizeUrl("https://example.com/path?q=1#section"),
        )
        assertEquals(
            "https://example.com/path",
            Archive.normalizeUrl("https://example.com/path/"),
        )
    }

    @Test
    fun normalizeUrlKeepsQueryAndWww() {
        assertEquals(
            "https://www.example.com/path?utm_source=x",
            Archive.normalizeUrl("https://www.example.com/path?utm_source=x"),
        )
    }

    @Test
    fun nostrArticleUsesKind7Tags() {
        val content = ReadableContent(
            url = "nostr:naddr1qq",
            articleCoordinate = coordinate,
            eventId = eventId,
            authorPubkey = pubkey,
        )
        assertEquals(Nip01Event.KIND_REACTION, Archive.kind(content))
        assertEquals(
            listOf(
                listOf("e", eventId),
                listOf("p", pubkey),
                listOf("k", "30023"),
                listOf("a", coordinate),
            ),
            Archive.tags(content),
        )
    }

    @Test
    fun webUrlUsesKind17() {
        val content = ReadableContent(url = "https://example.com/post/#top")
        assertEquals(Nip01Event.KIND_URL_REACTION, Archive.kind(content))
        assertEquals(
            listOf(listOf("r", "https://example.com/post")),
            Archive.tags(content),
        )
    }

    @Test
    fun webUrlWriteMatchesHomeCardIdentity() {
        val content = ReadableContent(url = "http://www.example.com/post/?utm_source=x#top")
        assertEquals(
            listOf(listOf("r", "https://example.com/post")),
            Archive.tags(content),
        )
    }

    @Test
    fun noteWithoutAuthorCannotArchive() {
        val content = ReadableContent(url = "nostr:note1qq", eventId = eventId)
        assertNull(Archive.kind(content))
        assertNull(Archive.tags(content))
    }

    @Test
    fun targetRefPrefersLongFormAddress() {
        val event = reaction(
            "📚",
            Nip01Event.KIND_REACTION,
            listOf(listOf("e", eventId), listOf("a", coordinate)),
        )
        assertEquals(BookmarkRef(BookmarkRefKind.Article, coordinate), Archive.targetRef(event))
    }

    @Test
    fun targetRefReadsWebsiteUrl() {
        val event = reaction(
            "📚",
            Nip01Event.KIND_URL_REACTION,
            listOf(listOf("r", "https://example.com/post")),
        )
        assertEquals(
            BookmarkRef(BookmarkRefKind.Url, "https://example.com/post"),
            Archive.targetRef(event),
        )
    }

    @Test
    fun targetRefIgnoresLookmarks() {
        assertNull(Archive.targetRef(reaction("👀", Nip01Event.KIND_REACTION, listOf(listOf("e", eventId)))))
    }

    @Test
    fun deleteTagsEachReaction() {
        val other = "bb".repeat(32)
        assertEquals(
            listOf(listOf("e", eventId), listOf("e", other)),
            Archive.deleteTags(listOf(eventId, other, "short")),
        )
        assertTrue(Archive.deleteTags(listOf("nope")).isEmpty())
    }

    private fun reaction(
        content: String,
        kind: Int,
        tags: List<List<String>> = emptyList(),
    ): Nip01Event = Nip01Event(
        id = "11".repeat(32),
        pubkey = pubkey,
        createdAt = 1,
        kind = kind,
        tags = tags,
        content = content,
        sig = "22".repeat(64),
    )
}
