package com.readwithboris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {
    @Test
    fun extractsBareHttpsUrl() {
        assertEquals(
            "https://example.com/article",
            UrlExtractor.extract("https://example.com/article"),
        )
    }

    @Test
    fun extractsUrlFromShareText() {
        assertEquals(
            "https://example.com/hello",
            UrlExtractor.extract("A great read\nhttps://example.com/hello"),
        )
    }

    @Test
    fun normalizesProtocolLessUrls() {
        assertEquals("https://www.example.com", UrlExtractor.normalize("www.example.com"))
    }

    @Test
    fun returnsNullWhenEmpty() {
        assertNull(UrlExtractor.extract("   "))
        assertNull(UrlExtractor.extract(null))
    }
}
