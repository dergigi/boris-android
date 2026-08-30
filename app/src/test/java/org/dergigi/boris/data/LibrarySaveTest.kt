package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip51
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySaveTest {
    private val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
    private val coordinate = "30023:$pubkey:my-article"
    private val eventId = "aa".repeat(32)

    @Test
    fun shareWebUrlBecomesAWebBookmark() {
        val content = LibrarySave.contentFromShare("https://example.com/later", "Later")
        assertEquals("https://example.com/later", content?.url)
        assertEquals("Later", content?.title)
        assertTrue(LibrarySave.isWeb(content!!))
        assertEquals(listOf("r", "https://example.com/later"), LibrarySave.hiddenTag(content))
    }

    @Test
    fun shareNoteBecomesAPrivateBookmarkTag() {
        val note = org.dergigi.boris.nostr.Nip19.noteEncode(eventId)
        val content = LibrarySave.contentFromShare("nostr:$note")
        assertFalse(LibrarySave.isWeb(content!!))
        assertEquals(listOf("e", eventId), LibrarySave.hiddenTag(content))
    }

    @Test
    fun shareArticleBecomesAPrivateBookmarkTag() {
        val naddr =
            "naddr1qvzqqqr4gupzqwlsccluhy6xxsr6l9a9uhhxf75g85g8a709tprjcn4e42h053vaqq9x67fdv9e8g6trd3jsrnn0q2"
        val content = LibrarySave.contentFromShare("https://njump.to/$naddr")
        assertFalse(LibrarySave.isWeb(content!!))
        assertEquals("a", LibrarySave.hiddenTag(content)?.first())
    }

    @Test
    fun shareProfileIsNotABookmark() {
        assertNull(
            LibrarySave.contentFromShare(
                "npub1dergggklka99wwrs92yz8wdjs952h2ux2ha2ed598ngwu9w7a6fsh9xzpc",
            ),
        )
    }

    @Test
    fun webUrlsAreNotNostrNative() {
        val content = ReadableContent(url = "https://example.com/post")
        assertTrue(LibrarySave.isWeb(content))
        assertEquals(listOf("r", "https://example.com/post"), LibrarySave.hiddenTag(content))
    }

    @Test
    fun nostrArticlesUseAnATag() {
        val content = ReadableContent(
            url = "nostr:naddr1qq",
            articleCoordinate = coordinate,
            eventId = eventId,
        )
        assertFalse(LibrarySave.isWeb(content))
        assertEquals(listOf("a", coordinate), LibrarySave.hiddenTag(content))
    }

    @Test
    fun notesUseAnETag() {
        val content = ReadableContent(url = "nostr:note1qq", eventId = eventId)
        assertEquals(listOf("e", eventId), LibrarySave.hiddenTag(content))
    }

    @Test
    fun webUrlIsSavedWhenA39701Exists() {
        val content = ReadableContent(url = "https://Alice.blog/post")
        val web = event(
            kind = Nip01Event.KIND_WEB_BOOKMARK,
            tags = listOf(listOf("d", "alice.blog/post")),
        )
        assertTrue(LibrarySave.isSaved(content, listEvent = null, webEvents = listOf(web)))
        assertFalse(LibrarySave.isSaved(content, listEvent = null, webEvents = emptyList()))
    }

    @Test
    fun nostrArticleIsSavedOnThePublicList() {
        val content = ReadableContent(
            url = "nostr:naddr1qq",
            articleCoordinate = coordinate,
        )
        val list = event(
            kind = Nip01Event.KIND_BOOKMARKS,
            tags = listOf(listOf("a", coordinate)),
        )
        assertTrue(LibrarySave.isSaved(content, list, emptyList()))
    }

    @Test
    fun nostrNoteIsSavedInPrivateTags() {
        val content = ReadableContent(url = "nostr:note1qq", eventId = eventId)
        val list = event(kind = Nip01Event.KIND_BOOKMARKS, tags = emptyList(), content = "cipher")
        assertTrue(LibrarySave.isSaved(content, list, emptyList(), hiddenTags = listOf(listOf("e", eventId))))
        assertFalse(LibrarySave.isSaved(content, list, emptyList()))
    }

    @Test
    fun encodeTagArrayRoundTrips() {
        val tags = listOf(listOf("a", coordinate), listOf("e", eventId))
        assertEquals(tags, Nip51.parseTagArray(Nip51.encodeTagArray(tags)))
        assertTrue(Nip51.containsTag(tags, listOf("a", coordinate.uppercase())))
    }

    private fun event(
        kind: Int,
        tags: List<List<String>>,
        content: String = "",
    ): Nip01Event = Nip01Event(
        id = "d7a92714f81d0f712e715556aee69ea6da6bfb287e6baf794a095d301d603ec7",
        pubkey = pubkey,
        createdAt = 99L,
        kind = kind,
        tags = tags,
        content = content,
        sig = "36d34e6448fe0223e9999361c39c492a208bc423d2fcdfc2a3404e04df7c22dc",
    )
}
