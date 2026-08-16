package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingPositionStoreTest {
    // The store is a singleton without file backing in unit tests,
    // so each test uses unique URLs to stay independent.

    private fun url(name: String) = "https://example.com/$name-${System.nanoTime()}"

    private val naddr =
        "naddr1qqxnzd3cxqmrzv3exgmr2wfeqy08wumn8ghj7mn0wd68yttsw43zuam9d3kx7unyv4ezumn9wshszyrhwden5te0dehhxarj9ekk7mf0qy88wumn8ghj7mn0wvhxcmmv9uq3zamnwvaz7tmwdaehgu3wwa5kuef0qy2hwumn8ghj7un9d3shjtnwdaehgu3wvfnj7q3qdergggklka99wwrs92yz8wdjs952h2ux2ha2ed598ngwu9w7a6fsxpqqqp65wy2vhhv"

    @Test
    fun savesAndClampsFraction() {
        ReadingPositionStore.save("https://example.com/a", 1.5f)
        assertEquals(1f, ReadingPositionStore.fraction("https://example.com/a"), 0f)
        ReadingPositionStore.save("https://example.com/a", 0.42f)
        assertEquals(0.42f, ReadingPositionStore.fraction("https://example.com/a"), 0.0001f)
        assertEquals(0f, ReadingPositionStore.fraction("https://example.com/never-read"), 0f)
    }

    @Test
    fun webKeysNormalizeSchemelessUrls() {
        ReadingPositionStore.save("example.com/post", 0.3f)
        assertEquals(0.3f, ReadingPositionStore.fraction("https://example.com/post"), 0.0001f)
    }

    @Test
    fun nostrArticleKeysByCoordinateNotRelayHints() {
        val target = NostrLink.parse("nostr:$naddr") as NostrTarget.Article
        assertEquals(target.ref.coordinate, ReadingPositionStore.key("nostr:$naddr"))
        ReadingPositionStore.save("nostr:$naddr", 0.6f)
        assertEquals(0.6f, ReadingPositionStore.fraction("nostr://$naddr"), 0.0001f)
    }

    @Test
    fun mergeAppliesNewerRemotePositions() {
        val url = url("merge-newer")
        val key = ReadingPositionStore.key(url)
        assertTrue(ReadingPositionStore.merge(key, 0.4f, 100))
        assertEquals(0.4f, ReadingPositionStore.fraction(url), 0.0001f)
        assertTrue(ReadingPositionStore.merge(key, 0.7f, 200))
        assertEquals(0.7f, ReadingPositionStore.fraction(url), 0.0001f)
    }

    @Test
    fun mergeIgnoresOlderRemotePositions() {
        val url = url("merge-older")
        val key = ReadingPositionStore.key(url)
        assertTrue(ReadingPositionStore.merge(key, 0.8f, 200))
        assertFalse(ReadingPositionStore.merge(key, 0.2f, 100))
        assertFalse(ReadingPositionStore.merge(key, 0.2f, 200))
        assertEquals(0.8f, ReadingPositionStore.fraction(url), 0.0001f)
    }

    @Test
    fun localSaveWinsOverStaleRemote() {
        val url = url("merge-local")
        val key = ReadingPositionStore.key(url)
        ReadingPositionStore.save(url, 0.5f)
        assertFalse(ReadingPositionStore.merge(key, 0.9f, 100))
        assertEquals(0.5f, ReadingPositionStore.fraction(url), 0.0001f)
    }

    @Test
    fun entriesSortNewestFirst() {
        val a = ReadingPositionStore.key(url("entries-a"))
        val b = ReadingPositionStore.key(url("entries-b"))
        ReadingPositionStore.merge(a, 0.3f, 100)
        ReadingPositionStore.merge(b, 0.6f, 200)
        val keys = ReadingPositionStore.entries().map { it.first }
        assertTrue(keys.indexOf(b) < keys.indexOf(a))
    }
}
