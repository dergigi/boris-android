package org.dergigi.boris.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OgPreviewCacheTest {
    private val file = File.createTempFile("og_preview_cache", ".json")

    @After
    fun tearDown() {
        OgPreviewCache.clear()
        file.delete()
    }

    @Test
    fun putAndGet() {
        OgPreviewCache.clear()
        val preview = OgPreview(title = "Title", imageUrl = "https://example.com/img.png", siteName = "Example")
        OgPreviewCache.put("https://example.com/post", preview)
        assertEquals(preview, OgPreviewCache.get("https://example.com/post"))
        assertNull(OgPreviewCache.get("https://example.com/other"))
    }

    @Test
    fun attemptsExpire() {
        OgPreviewCache.clear()
        val url = "https://example.com/no-og"
        assertFalse(OgPreviewCache.recentlyAttempted(url, now = 1_000L))
        OgPreviewCache.markAttempted(url, now = 1_000L)
        assertTrue(OgPreviewCache.recentlyAttempted(url, now = 1_000L + 60 * 60_000L))
        assertFalse(OgPreviewCache.recentlyAttempted(url, now = 1_000L + 7 * 60 * 60_000L))
    }

    @Test
    fun emptyPreviewNotStored() {
        OgPreviewCache.clear()
        OgPreviewCache.put("https://example.com/empty", OgPreview(title = null, imageUrl = null, siteName = null))
        assertNull(OgPreviewCache.get("https://example.com/empty"))
    }
}
