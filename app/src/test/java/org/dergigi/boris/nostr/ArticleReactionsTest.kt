package org.dergigi.boris.nostr

import org.dergigi.boris.data.ReadableContent
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleReactionsTest {
    private val author = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
    private val reader = "11".repeat(32)
    private val articleEventId = "aa".repeat(32)
    private val coordinate = "30023:$author:my-article"

    @Test
    fun vocabularyStartsCurated() {
        assertEquals(ArticleReaction.Slop, ArticleReaction.fromContent("🤖"))
        assertEquals(ArticleReaction.Love, ArticleReaction.fromContent("🧡"))
        assertEquals(ArticleReaction.Good, ArticleReaction.fromContent("👍"))
        assertEquals(ArticleReaction.Love, ArticleReaction.DEFAULT)
        assertNull(ArticleReaction.fromContent("❤️"))
        assertNull(ArticleReaction.fromContent("🔥"))
    }

    @Test
    fun nostrArticleUsesKind7AddressTags() {
        val content = article()

        assertEquals(Nip01Event.KIND_REACTION, ArticleReactions.kind(content))
        assertEquals(
            listOf(
                listOf("a", coordinate),
                listOf("p", author),
                listOf("k", "30023"),
                listOf("e", articleEventId),
            ),
            ArticleReactions.tags(content),
        )
    }

    @Test
    fun nostrArticleUsesCoordinateAuthorForTags() {
        val otherAuthor = "22".repeat(32)
        val tags = ArticleReactions.tags(article(authorPubkey = otherAuthor))

        assertEquals(
            listOf("p", author),
            tags?.firstOrNull { it.firstOrNull() == "p" },
        )
    }

    @Test
    fun invalidEventIdIsNotTagged() {
        val tags = ArticleReactions.tags(article(eventId = "z".repeat(64)))

        assertEquals(
            listOf(
                listOf("a", coordinate),
                listOf("p", author),
                listOf("k", "30023"),
            ),
            tags,
        )
    }

    @Test
    fun webArticleUsesKind17UrlReaction() {
        val content = ReadableContent(url = "https://example.com/post")

        assertEquals(Nip01Event.KIND_URL_REACTION, ArticleReactions.kind(content))
        assertEquals(listOf(listOf("r", "https://example.com/post")), ArticleReactions.tags(content))
        val json = JSONObject(ArticleReactions.unsignedJson(ArticleReaction.Good, content, reader, createdAt = 123)!!)
        assertEquals(Nip01Event.KIND_URL_REACTION, json.getInt("kind"))
        assertEquals("👍", json.getString("content"))
    }

    @Test
    fun noteUsesArchiveStyleEventTags() {
        val content = ReadableContent(url = "nostr:nevent1qq", eventId = articleEventId, authorPubkey = author)

        assertEquals(Nip01Event.KIND_REACTION, ArticleReactions.kind(content))
        assertEquals(
            listOf(listOf("e", articleEventId), listOf("p", author), listOf("k", "1")),
            ArticleReactions.tags(content),
        )
    }

    @Test
    fun currentReactionMatchesWebUrlByNormalizedRTag() {
        val content = ReadableContent(url = "https://www.example.com/post?utm_source=x")
        val match = Nip01Event(
            id = "00".repeat(32),
            pubkey = reader,
            createdAt = 10,
            kind = Nip01Event.KIND_URL_REACTION,
            tags = listOf(listOf("r", "https://example.com/post")),
            content = "🧡",
            sig = "ff".repeat(64),
        )
        val otherPage = match.copy(tags = listOf(listOf("r", "https://example.com/other")), createdAt = 20)

        assertEquals(ArticleReaction.Love, ArticleReactions.currentReaction(listOf(match, otherPage), content, reader))
    }

    @Test
    fun unsupportedContentHasNoReactionKind() {
        val content = ReadableContent(url = "file:///tmp/x")

        assertNull(ArticleReactions.kind(content))
        assertNull(ArticleReactions.tags(content))
        assertNull(ArticleReactions.unsignedJson(ArticleReaction.Good, content, reader, createdAt = 123))
    }

    @Test
    fun unsignedJsonUsesSelectedReaction() {
        val raw = ArticleReactions.unsignedJson(
            reaction = ArticleReaction.Love,
            content = article(),
            pubkeyHex = reader,
            createdAt = 123,
        )
        val json = JSONObject(raw!!)

        assertEquals(Nip01Event.KIND_REACTION, json.getInt("kind"))
        assertEquals("🧡", json.getString("content"))
        assertEquals(reader, json.getString("pubkey"))
        assertEquals(123, json.getLong("created_at"))
        assertEquals("a", json.getJSONArray("tags").getJSONArray(0).getString(0))
        assertEquals(coordinate, json.getJSONArray("tags").getJSONArray(0).getString(1))
    }

    @Test
    fun currentReactionUsesLatestMatchingUserReaction() {
        val old = reaction(content = "👍", createdAt = 10)
        val latest = reaction(content = "🧡", createdAt = 20)
        val otherUser = reaction(content = "🤖", createdAt = 30, pubkey = "22".repeat(32))

        assertEquals(
            ArticleReaction.Love,
            ArticleReactions.currentReaction(listOf(old, latest, otherUser), article(), reader),
        )
    }

    @Test
    fun currentReactionIgnoresArchiveAndOtherTargets() {
        val archive = reaction(content = Archive.EMOJI, createdAt = 20)
        val other = reaction(
            content = "👍",
            createdAt = 30,
            tags = listOf(listOf("a", "30023:$author:other")),
        )

        assertNull(ArticleReactions.currentReaction(listOf(archive, other), article(), reader))
    }

    @Test
    fun currentReactionRequiresMatchingArticleAddress() {
        val matchingEventId = reaction(
            content = "👍",
            createdAt = 30,
            tags = listOf(
                listOf("a", "30023:$author:other"),
                listOf("e", articleEventId),
            ),
        )

        assertNull(ArticleReactions.currentReaction(listOf(matchingEventId), article(), reader))
    }

    @Test
    fun currentReactionIgnoresWebArticleEventIds() {
        val content = ReadableContent(
            url = "https://example.com/post",
            eventId = articleEventId,
        )
        val eventIdOnly = reaction(
            content = "👍",
            createdAt = 30,
            tags = listOf(listOf("e", articleEventId)),
        )

        assertNull(ArticleReactions.currentReaction(listOf(eventIdOnly), content, reader))
    }

    private fun article(
        eventId: String = articleEventId,
        authorPubkey: String? = author,
    ): ReadableContent = ReadableContent(
        url = "nostr:naddr1qq",
        articleCoordinate = coordinate,
        eventId = eventId,
        authorPubkey = authorPubkey,
    )

    private fun reaction(
        content: String,
        createdAt: Long,
        pubkey: String = reader,
        tags: List<List<String>> = listOf(listOf("a", coordinate), listOf("e", articleEventId)),
    ): Nip01Event = Nip01Event(
        id = "00".repeat(32),
        pubkey = pubkey,
        createdAt = createdAt,
        kind = Nip01Event.KIND_REACTION,
        tags = tags,
        content = content,
        sig = "ff".repeat(64),
    )
}
