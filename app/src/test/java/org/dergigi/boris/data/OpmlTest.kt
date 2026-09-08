package org.dergigi.boris.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpmlTest {
    @Test
    fun exportWritesDistinctHttpFeeds() {
        val opml = Opml.export(
            listOf(
                "https://example.com/feed.xml",
                " https://example.com/feed.xml ",
                "ftp://example.com/feed.xml",
                "http://blog.example.org/rss",
            ),
        )

        assertTrue(opml.contains("""<opml version="2.0">"""))
        assertTrue(opml.contains("""xmlUrl="https://example.com/feed.xml""""))
        assertTrue(opml.contains("""xmlUrl="http://blog.example.org/rss""""))
        assertFalse(opml.contains("ftp://example.com/feed.xml"))
        assertTrue(opml.indexOf("https://example.com/feed.xml") == opml.lastIndexOf("https://example.com/feed.xml"))
    }

    @Test
    fun exportEscapesFeedAttributes() {
        val opml = Opml.export(listOf("https://example.com/feed.xml?one=1&two=2"))

        assertTrue(opml.contains("https://example.com/feed.xml?one=1&amp;two=2"))
    }
}
