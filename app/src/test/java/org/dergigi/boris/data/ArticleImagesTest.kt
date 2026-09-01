package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArticleImagesTest {
    @Test
    fun cacheKeyIsStableAndHttpsCanonical() {
        val first = ArticleImages.cacheKey("https://cdn.example.com/a.jpg")
        assertNotNull(first)
        assertEquals(16, first!!.length)
        assertEquals(first, ArticleImages.cacheKey("https://cdn.example.com/a.jpg"))
        assertEquals(first, ArticleImages.cacheKey("http://cdn.example.com/a.jpg"))
    }

    @Test
    fun shouldConvertSkipsGifAndSvg() {
        assertTrue(ArticleImages.shouldConvert("https://cdn.example.com/a.jpg"))
        assertTrue(ArticleImages.shouldConvert("https://cdn.example.com/a.webp"))
        assertFalse(ArticleImages.shouldConvert("https://cdn.example.com/a.gif"))
        assertFalse(ArticleImages.shouldConvert("https://cdn.example.com/a.GIF?w=2"))
        assertFalse(ArticleImages.shouldConvert("https://cdn.example.com/icon.svg"))
    }

    @Test
    fun urlsToFetchIsEmptyWhenFileExists() {
        val dir = File(System.getProperty("java.io.tmpdir"), "boris-article-images-test")
        dir.deleteRecursively()
        dir.mkdirs()
        try {
            ArticleImages.init(dir)
            val url = "https://cdn.example.com/photo.jpg"
            val dest = ArticleImages.fileFor(url)
            assertNotNull(dest)
            dest!!.writeBytes(byteArrayOf(1, 2, 3))
            assertFalse(ArticleImages.needsFetch(url))
            assertTrue(ArticleImages.urlsToFetch(listOf(url)).isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun urlsForPutsCoverFirstAndSkipsDuplicates() {
        val content = ReadableContent(
            url = "https://example.com/a",
            markdown = "![a](https://cdn.example.com/a.jpg)\n\n![b](https://cdn.example.com/b.jpg)",
            imageUrl = "https://cdn.example.com/cover.jpg",
        )
        assertEquals(
            listOf(
                "https://cdn.example.com/cover.jpg",
                "https://cdn.example.com/a.jpg",
                "https://cdn.example.com/b.jpg",
            ),
            ArticleImages.urlsFor(content),
        )
        val stripped = content.copy(
            markdown = ArticleCover.stripLeadingImage(
                "![cover](https://cdn.example.com/cover.jpg)\n\n![a](https://cdn.example.com/a.jpg)",
                "https://cdn.example.com/cover.jpg",
            ),
        )
        assertEquals(
            listOf("https://cdn.example.com/cover.jpg", "https://cdn.example.com/a.jpg"),
            ArticleImages.urlsFor(stripped),
        )
        val coverAlsoInBody = content.copy(imageUrl = "https://cdn.example.com/a.jpg")
        assertEquals(
            listOf("https://cdn.example.com/a.jpg", "https://cdn.example.com/b.jpg"),
            ArticleImages.urlsFor(coverAlsoInBody),
        )
    }

    @Test
    fun offlineImagesDefaultOn() {
        assertTrue(UserSettings.defaults().offlineDownloadEnabled(ArticleImages.SETTINGS_KEY))
        assertFalse(
            UserSettings.defaults()
                .withBoolean(ArticleImages.SETTINGS_KEY, false)
                .offlineDownloadEnabled(ArticleImages.SETTINGS_KEY),
        )
    }
}
