package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageStoreTest {
    @Test
    fun filenameFromPathSegment() {
        assertEquals(
            "cat.png",
            ImageStore.filenameFor("https://cdn.example.com/photos/cat.png", 0),
        )
    }

    @Test
    fun filenameStripsQuery() {
        assertEquals(
            "hero.jpg",
            ImageStore.filenameFor("https://cdn.example.com/hero.jpg?w=1200&q=80", 1),
        )
    }

    @Test
    fun filenameFallsBackWhenMissingExtension() {
        assertEquals(
            "image-3.jpg",
            ImageStore.filenameFor("https://cdn.example.com/img", 2),
        )
    }

    @Test
    fun mimeFromFilename() {
        assertEquals("image/png", ImageStore.mimeFor("photo.png"))
        assertEquals("image/jpeg", ImageStore.mimeFor("photo.jpg"))
        assertEquals("image/webp", ImageStore.mimeFor("photo.webp", "text/html"))
    }

    @Test
    fun mimePrefersImageContentType() {
        assertEquals("image/avif", ImageStore.mimeFor("photo.jpg", "image/avif; charset=binary"))
    }
}
