package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Nip23Test {
    @Test
    fun readsNip23Tags() {
        val event = article(
            d = "bitcoin-is-time",
            title = "Bitcoin Is Time",
            summary = "Time is money.",
            image = "https://dergigi.com/cover.png",
            publishedAt = "1610582400",
        )
        assertEquals("bitcoin-is-time", Nip23.identifier(event))
        assertEquals("Bitcoin Is Time", Nip23.title(event))
        assertEquals("Time is money.", Nip23.summary(event))
        assertEquals("https://dergigi.com/cover.png", Nip23.image(event))
        assertEquals(1_610_582_400L, Nip23.publishedAt(event))
    }

    @Test
    fun fallsBackToCreatedAtWithoutPublishedAt() {
        val event = article(d = "draft", title = null, publishedAt = null)
        assertNull(Nip23.title(event))
        assertEquals(42L, Nip23.publishedAt(event))
    }

    private fun article(
        d: String,
        title: String?,
        summary: String? = null,
        image: String? = null,
        publishedAt: String? = "1610582400",
    ): Nip01Event {
        val tags = buildList {
            add(listOf("d", d))
            if (title != null) add(listOf("title", title))
            if (summary != null) add(listOf("summary", summary))
            if (image != null) add(listOf("image", image))
            if (publishedAt != null) add(listOf("published_at", publishedAt))
        }
        return Nip01Event(
            id = "d7a92714f81d0f712e715556aee69ea6da6bfb287e6baf794a095d301d603ec7",
            pubkey = "2729620da105979b22acfdfe9585274a78c282869b493abfa4120d3af2061298",
            createdAt = 42L,
            kind = Nip01Event.KIND_LONG_FORM,
            tags = tags,
            content = "body",
            sig = "36d34e6448fe0223e9999361c39c492a208bc423d2fcdfc2a3404e04df7c22dc",
        )
    }
}
