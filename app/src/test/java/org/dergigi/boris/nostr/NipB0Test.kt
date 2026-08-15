package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Test

class NipB0Test {
    @Test
    fun dTagDropsTheScheme() {
        assertEquals("alice.blog/post?x=1", NipB0.dTag("https://Alice.blog/post?x=1"))
        assertEquals("example.com/a", NipB0.dTag("http://example.com/a"))
    }

    @Test
    fun urlFromDTagAddsHttpsWhenSchemeIsOmitted() {
        val event = webBookmark(d = "alice.blog/post", title = "Blog insights by Alice")
        assertEquals("https://alice.blog/post", NipB0.url(event))
        assertEquals("Blog insights by Alice", NipB0.title(event))
        assertEquals(1_738_863_000L, NipB0.publishedAt(event))
    }

    @Test
    fun keepsExplicitHttpUrls() {
        val event = webBookmark(d = "https://example.com/a", title = null, publishedAt = null)
        assertEquals("https://example.com/a", NipB0.url(event))
        assertEquals(99L, NipB0.publishedAt(event))
    }

    private fun webBookmark(
        d: String,
        title: String?,
        publishedAt: String? = "1738863000",
    ): Nip01Event {
        val tags = buildList {
            add(listOf("d", d))
            if (title != null) add(listOf("title", title))
            if (publishedAt != null) add(listOf("published_at", publishedAt))
        }
        return Nip01Event(
            id = "d7a92714f81d0f712e715556aee69ea6da6bfb287e6baf794a095d301d603ec7",
            pubkey = "2729620da105979b22acfdfe9585274a78c282869b493abfa4120d3af2061298",
            createdAt = 99L,
            kind = Nip01Event.KIND_WEB_BOOKMARK,
            tags = tags,
            content = "A marvelous insight",
            sig = "36d34e6448fe0223e9999361c39c492a208bc423d2fcdfc2a3404e04df7c22dc",
        )
    }
}
