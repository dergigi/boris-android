package org.dergigi.boris.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleCopyLinksTest {
    @Test
    fun webArticleHasOnlyThePlainCopy() {
        assertFalse(hasAlternateCopyLinks("https://www.citadel21.com/the-paranoid-wallet"))
    }

    @Test
    fun nostrArticleAddsNjump() {
        val naddr =
            "naddr1qqxnzd3cxqmrzv3exgmr2wfeqy08wumn8ghj7mn0wd68yttsw43zuam9d3kx7unyv4ezumn9wshszyrhwden5te0dehhxarj9ekk7mf0qy88wumn8ghj7mn0wvhxcmmv9uq3zamnwvaz7tmwdaehgu3wwa5kuef0qy2hwumn8ghj7un9d3shjtnwdaehgu3wvfnj7q3qdergggklka99wwrs92yz8wdjs952h2ux2ha2ed598ngwu9w7a6fsxpqqqp65wy2vhhv"
        assertTrue(hasAlternateCopyLinks("nostr:$naddr"))
    }

    @Test
    fun tweetAddsXcancel() {
        assertTrue(hasAlternateCopyLinks("https://x.com/jack/status/20"))
    }
}
