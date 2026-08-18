package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RssParserTest {
    @Test
    fun emptyRssDocumentIsStillAFeed() {
        assertEquals(
            "rss",
            xmlRootLocalName(
                """
                <?xml version="1.0"?>
                <rss version="2.0">
                  <channel><title>Example</title></channel>
                </rss>
                """.trimIndent(),
            )?.lowercase(),
        )
    }

    @Test
    fun emptyAtomDocumentIsStillAFeed() {
        assertEquals(
            "feed",
            xmlRootLocalName(
                """
                <?xml version="1.0"?>
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <title>Example</title>
                </feed>
                """.trimIndent(),
            )?.lowercase(),
        )
    }

    @Test
    fun wellFormedNonFeedXmlIsNotAFeed() {
        assertEquals(
            "document",
            xmlRootLocalName("<document><title>Not a feed</title></document>"),
        )
    }
}
