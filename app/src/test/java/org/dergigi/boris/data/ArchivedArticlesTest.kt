package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchivedArticlesTest {
    private val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
    private val eventId = "aa".repeat(32)
    private val coordinate = "30023:$pubkey:my-article"

    @Test
    fun matchesWebNostrArticleAndNoteArchives() {
        val article = NostrArticle.fromCoordinate(coordinate)!!
        val noteUrl = "nostr:${Nip19.noteEncode(eventId)}"
        val keys = ArchivedArticles.keys(
            listOf(
                reaction(Nip01Event.KIND_URL_REACTION, listOf(listOf("r", "https://example.com/read/"))),
                reaction(Nip01Event.KIND_REACTION, listOf(listOf("a", coordinate))),
                reaction(Nip01Event.KIND_REACTION, listOf(listOf("e", eventId))),
            ),
        )
        assertTrue(ArchivedArticles.isArchived("https://example.com/read#top", keys))
        assertTrue(ArchivedArticles.isArchived(article.uri, keys))
        assertTrue(ArchivedArticles.isArchived(noteUrl, keys))
        assertFalse(ArchivedArticles.isArchived("https://example.com/other", keys))
    }

    @Test
    fun visibleDropsArchivedOnlyWhenAsked() {
        val kept = HighlightedArticle("https://example.com/new", "example.com", "New", null, 2)
        val archived = HighlightedArticle("https://example.com/read", "example.com", "Read", null, 1)
        val keys = ArchivedArticles.keys(
            listOf(reaction(Nip01Event.KIND_URL_REACTION, listOf(listOf("r", "https://example.com/read")))),
        )
        val all = listOf(kept, archived)
        assertEquals(all, ArchivedArticles.visible(all, keys, hideArchived = false))
        assertEquals(listOf(kept), ArchivedArticles.visible(all, keys, hideArchived = true))
        assertEquals(all, ArchivedArticles.visible(all, emptySet(), hideArchived = true))
    }

    private fun reaction(kind: Int, tags: List<List<String>>): Nip01Event = Nip01Event(
        id = "11".repeat(32),
        pubkey = pubkey,
        createdAt = 1,
        kind = kind,
        tags = tags,
        content = "📚",
        sig = "22".repeat(64),
    )
}
