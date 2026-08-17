package org.dergigi.boris.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Navigation encodes with [Routes.reader] / URLEncoder, then [NavType.StringType]
 * decodes once. A second URLDecoder (the old ReaderViewModel path) corrupts `%xx`.
 */
class ReaderRouteTest {
    @Test
    fun percentEncodedQuerySurvivesOneDecode() {
        assertRoundTrip("https://example.com/foo?q=a%26b")
    }

    @Test
    fun percentEncodedPathSegmentSurvivesOneDecode() {
        assertRoundTrip("https://example.com/a%2Fb")
    }

    @Test
    fun plusInPathSurvivesOneDecode() {
        assertRoundTrip("https://example.com/path+plus")
    }

    @Test
    fun plainUrlSurvivesOneDecode() {
        assertRoundTrip("https://example.com/article")
    }

    @Test
    fun secondDecodeCorruptsAmpersandEncoding() {
        val url = "https://example.com/foo?q=a%26b"
        val once = decodeOnce(encodeForNav(url))
        val twice = URLDecoder.decode(once, UTF_8)
        assertEquals(url, once)
        assertEquals("https://example.com/foo?q=a&b", twice)
        assertNotEquals(url, twice)
    }

    @Test
    fun routesReaderPutsUrlInQuery() {
        val url = "https://example.com/foo?q=a%26b"
        val route = Routes.reader(url)
        assertEquals(url, navUrlArg(route))
    }

    private fun assertRoundTrip(url: String) {
        assertEquals(url, navUrlArg(Routes.reader(url)))
    }

    private fun navUrlArg(route: String): String {
        val encoded = route.substringAfter("url=").substringBefore("&highlight=")
        return decodeOnce(encoded)
    }

    private fun encodeForNav(url: String): String =
        URLEncoder.encode(url, UTF_8)

    private fun decodeOnce(encoded: String): String =
        URLDecoder.decode(encoded, UTF_8)

    companion object {
        private val UTF_8 = StandardCharsets.UTF_8.name()
    }
}
