package org.dergigi.boris.nostr

import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.ReadingPositionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Nip85Test {
    private val coordinate = "30023:${"b".repeat(64)}:my-article"

    private fun event(
        tags: List<List<String>>,
        content: String = "{\"progress\":0.5}",
        createdAt: Long = 100,
    ): Nip01Event = Nip01Event(
        id = "0".repeat(64),
        pubkey = "a".repeat(64),
        createdAt = createdAt,
        kind = Nip85.KIND,
        tags = tags,
        content = content,
        sig = "0".repeat(128),
    )

    @Test
    fun dTagUsesCoordinateForArticles() {
        val uri = NostrArticle.fromCoordinate(coordinate)!!.uri
        assertEquals(coordinate, Nip85.dTag(uri))
    }

    @Test
    fun dTagBase64UrlEncodesWebUrls() {
        assertEquals(
            "url:aHR0cHM6Ly9leGFtcGxlLmNvbS9wb3N0",
            Nip85.dTag("https://example.com/post"),
        )
    }

    @Test
    fun dTagSkipsNotes() {
        assertNull(Nip85.dTag("nostr:${Nip19.noteEncode("c".repeat(64))}"))
    }

    @Test
    fun tagsIncludeCoordinateForArticles() {
        val uri = NostrArticle.fromCoordinate(coordinate)!!.uri
        assertEquals(
            listOf(listOf("d", coordinate), listOf("a", coordinate)),
            Nip85.tags(uri),
        )
    }

    @Test
    fun tagsIncludeRawUrlForWeb() {
        assertEquals(
            listOf(
                listOf("d", "url:aHR0cHM6Ly9leGFtcGxlLmNvbS9wb3N0"),
                listOf("r", "https://example.com/post"),
            ),
            Nip85.tags("https://example.com/post"),
        )
    }

    @Test
    fun contentJsonRoundsAndTrimsNumbers() {
        assertEquals("{\"progress\":0.42,\"ts\":123}", Nip85.contentJson(0.42f, 123))
        assertEquals("{\"progress\":1,\"ts\":5}", Nip85.contentJson(1f, 5))
        assertEquals("{\"progress\":0,\"ts\":5}", Nip85.contentJson(-2f, 5))
    }

    @Test
    fun progressParsesWebappContent() {
        assertEquals(
            0.66f,
            Nip85.progress("{\"progress\":0.66,\"loc\":1432,\"ts\":1734635012}"),
        )
        assertNull(Nip85.progress("{\"progress\":1.5}"))
        assertNull(Nip85.progress("{}"))
    }

    @Test
    fun timestampPrefersContentTs() {
        val withTs = event(emptyList(), "{\"progress\":0.5,\"ts\":42}", createdAt = 100)
        assertEquals(42, Nip85.timestamp(withTs))
        val withoutTs = event(emptyList(), "{\"progress\":0.5}", createdAt = 100)
        assertEquals(100, Nip85.timestamp(withoutTs))
    }

    @Test
    fun positionKeyPrefersCoordinateTag() {
        val e = event(listOf(listOf("d", coordinate), listOf("a", coordinate)))
        assertEquals(coordinate, Nip85.positionKey(e))
    }

    @Test
    fun positionKeyNormalizesUrlTag() {
        val e = event(
            listOf(
                listOf("d", "url:aHR0cHM6Ly9leGFtcGxlLmNvbS9wb3N0"),
                listOf("r", "https://example.com/post"),
            ),
        )
        assertEquals(ReadingPositionStore.key("https://example.com/post"), Nip85.positionKey(e))
    }

    @Test
    fun positionKeyFallsBackToDTag() {
        val e = event(listOf(listOf("d", "url:aHR0cHM6Ly9leGFtcGxlLmNvbS9wb3N0")))
        assertEquals(ReadingPositionStore.key("https://example.com/post"), Nip85.positionKey(e))
    }

    @Test
    fun positionKeyRejectsGarbage() {
        assertNull(Nip85.positionKey(event(listOf(listOf("d", "nonsense")))))
    }
}
