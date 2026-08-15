package org.dergigi.boris.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun emptyPreviewNotStored() {
        OgPreviewCache.clear()
        OgPreviewCache.put("https://example.com/empty", OgPreview(title = null, imageUrl = null, siteName = null))
        assertNull(OgPreviewCache.get("https://example.com/empty"))
    }
}
