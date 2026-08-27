package org.dergigi.boris.ui.reader

import org.dergigi.boris.data.ArticlePreview
import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.OgPreviewCache
import org.dergigi.boris.data.ReadableContent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReaderPreviewTest {
    @Before
    fun setUp() {
        OgPreviewCache.clear()
    }

    @After
    fun tearDown() {
        OgPreviewCache.clear()
    }

    @Test
    fun loadingStateUsesCachedWebPreview() {
        val url = "https://dergigi.com/2024/01/01/post"
        ArticlePreview.remember(
            ReadableContent(
                url = url,
                title = "Cached title",
                imageUrl = "https://cdn.example.com/cover.jpg",
            ),
        )
        val state = readerLoadingState(url)
        assertEquals(url, state.url)
        assertEquals("Cached title", state.title)
        assertEquals("https://cdn.example.com/cover.jpg", state.imageUrl)
    }

    @Test
    fun loadingStateDecodesHtmlEntitiesInCachedTitle() {
        val url = "https://example.com/random-bytes"
        ArticlePreview.remember(
            ReadableContent(
                url = url,
                title = "When random.bytes() runs but doesn&#x27;t work",
            ),
        )
        val state = readerLoadingState(url)
        assertEquals("When random.bytes() runs but doesn't work", state.title)
    }

    @Test
    fun loadingStateWithoutCacheKeepsSpinnerOnly() {
        val state = readerLoadingState("https://unknown.example/post")
        assertEquals("https://unknown.example/post", state.url)
        assertNull(state.title)
        assertNull(state.imageUrl)
    }

    @Test
    fun loadingStateFindsNostrAliasPreview() {
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
        val fromCoordinate = readerLoadingState(coordinate)
        assertEquals("I Left the Future and Arrived at Home", fromCoordinate.title)
        assertEquals("https://cdn.example.com/cover.jpg", fromCoordinate.imageUrl)
        assertTrue(fromCoordinate.url == coordinate)
    }

    @Test
    fun authorFooterOnlyShowsForNostrLongFormArticles() {
        val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        assertTrue(
            showNostrAuthorFooterCard(
                ReadableContent(
                    url = "nostr:naddr1qq",
                    articleCoordinate = "30023:$pubkey:essay",
                    authorPubkey = pubkey,
                ),
            ),
        )
        assertTrue(
            !showNostrAuthorFooterCard(
                ReadableContent(
                    url = "https://example.com/post",
                    title = "Web article",
                    authorPubkey = pubkey,
                ),
            ),
        )
    }
}
