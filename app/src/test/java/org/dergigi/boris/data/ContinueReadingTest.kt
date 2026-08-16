package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinueReadingTest {
    @Test
    fun inProgressExcludesBarelyStartedAndFinished() {
        assertFalse(ContinueReading.inProgress(0f))
        assertFalse(ContinueReading.inProgress(0.01f))
        assertTrue(ContinueReading.inProgress(0.02f))
        assertTrue(ContinueReading.inProgress(0.5f))
        assertTrue(ContinueReading.inProgress(0.95f))
        assertFalse(ContinueReading.inProgress(0.96f))
        assertFalse(ContinueReading.inProgress(1f))
    }

    @Test
    fun urlForKeyPassesWebUrlsThrough() {
        assertEquals(
            "https://example.com/post",
            ContinueReading.urlForKey("https://example.com/post"),
        )
    }

    @Test
    fun urlForKeyEncodesEventIds() {
        val id = "a".repeat(64)
        val url = ContinueReading.urlForKey(id)
        assertTrue(url != null && url.startsWith("nostr:note1"))
    }

    @Test
    fun urlForKeyEncodesArticleCoordinates() {
        val coordinate = "30023:${"b".repeat(64)}:my-article"
        val url = ContinueReading.urlForKey(coordinate)
        assertTrue(url != null && url.startsWith("nostr:naddr1"))
    }

    @Test
    fun urlForKeyRejectsGarbage() {
        assertNull(ContinueReading.urlForKey("not-a-key"))
        assertNull(ContinueReading.urlForKey("12345:abc:def"))
    }
}
