package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RssDiscoveryTest {
    @Test
    fun buildsRootFeedUrlForWebArticles() {
        assertEquals(
            "https://dergigi.com/feed.xml",
            RssDiscovery.rootFeedUrl("https://dergigi.com/2024/01/01/post"),
        )
    }

    @Test
    fun includesFirstPathSegmentFeedCandidate() {
        assertEquals(
            listOf(
                "https://geohot.github.io/feed.xml",
                "https://geohot.github.io/blog/feed.xml",
            ),
            RssDiscovery.feedCandidates("https://geohot.github.io/blog/some-post"),
        )
    }

    @Test
    fun ignoresNostrNativeLinks() {
        assertNull(
            RssDiscovery.rootFeedUrl(
                "nostr:naddr1qqxnzd3cxqmrzv3exgmr2wfeqy08wumn8ghj7mn0wd68yttsw43zuam9d3kx7",
            ),
        )
    }
}
