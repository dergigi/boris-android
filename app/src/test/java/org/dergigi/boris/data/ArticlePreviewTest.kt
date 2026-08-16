package org.dergigi.boris.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ArticlePreviewTest {
    @Before
    fun setUp() {
        OgPreviewCache.clear()
    }

    @After
    fun tearDown() {
        OgPreviewCache.clear()
    }

    @Test
    fun rememberIsReadableFromCoordinateAndNaddr() {
        val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val coordinate = "30023:$pubkey:i-left-the-future-and-arrived-at-home"
        val article = NostrArticle.fromCoordinate(coordinate)!!
        ArticlePreview.remember(
            ReadableContent(
                url = article.uri,
                title = "I Left the Future and Arrived at Home",
                imageUrl = "https://cdn.example.com/cover.jpg",
                articleCoordinate = coordinate,
            ),
        )
        val fromUri = ArticlePreview.get(article.uri)
        val fromCoordinate = ArticlePreview.get(coordinate)
        assertEquals("I Left the Future and Arrived at Home", fromUri?.title)
        assertEquals("https://cdn.example.com/cover.jpg", fromUri?.imageUrl)
        assertEquals(fromUri, fromCoordinate)
    }

    @Test
    fun keysIncludeNostrAliases() {
        val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val coordinate = "30023:$pubkey:slug"
        val article = NostrArticle.fromCoordinate(coordinate)!!
        val keys = ArticlePreview.keysFor(article.uri, coordinate)
        assertTrue(keys.contains(article.uri))
        assertTrue(keys.contains(coordinate))
        assertTrue(keys.contains(article.publicUrl))
    }
}
