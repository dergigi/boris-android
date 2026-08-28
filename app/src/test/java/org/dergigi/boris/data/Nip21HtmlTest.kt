package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.NprofilePointer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Nip21HtmlTest {
    private val gigiNpub = "npub1dergggklka99wwrs92yz8wdjs952h2ux2ha2ed598ngwu9w7a6fsh9xzpc"
    private val gigiHex = Nip19.npubDecode(gigiNpub)
    private val otherHex = "aa".repeat(32)
    private val naddr =
        "naddr1qvzqqqr4gupzqwlsccluhy6xxsr6l9a9uhhxf75g85g8a709tprjcn4e42h053vaqq9x67fdv9e8g6trd3jsrnn0q2"
    private val naddrCoordinate = NostrArticle.parse(naddr)!!.coordinate

    @Test
    fun dergigiHeadTagsResolveAuthor() {
        val html = """
            <html><head>
            <link rel="me" href="nostr:$gigiNpub" />
            <link rel="author" href="nostr:$gigiNpub" />
            </head></html>
        """.trimIndent()
        val links = Nip21Html.parse(html)
        assertEquals(gigiHex, links.authorPubkey)
        assertNull(links.articleCoordinate)
    }

    @Test
    fun prefersAuthorOverMe() {
        val other = Nip19.npubEncode(otherHex)
        val html = """
            <link rel="me" href="nostr:$gigiNpub">
            <link rel="author" href="nostr:$other">
        """.trimIndent()
        assertEquals(otherHex, Nip21Html.parse(html).authorPubkey)
    }

    @Test
    fun meIsSiteOwnerFallback() {
        val html = """<link rel="me" href="nostr:$gigiNpub">"""
        assertEquals(gigiHex, Nip21Html.parse(html).authorPubkey)
    }

    @Test
    fun alternateNaddrConnectsLongForm() {
        val html = """<link rel="alternate" href="nostr:$naddr">"""
        val links = Nip21Html.parse(html)
        assertEquals(naddrCoordinate, links.articleCoordinate)
        assertEquals(NostrArticle.parse(naddr)!!.pointer.pubkey, links.authorPubkey)
    }

    @Test
    fun authorWinsWhenAlternatePubkeyDiffers() {
        val html = """
            <link rel="author" href="nostr:${Nip19.npubEncode(otherHex)}">
            <link rel="alternate" href="nostr:$naddr">
        """.trimIndent()
        val links = Nip21Html.parse(html)
        assertEquals(otherHex, links.authorPubkey)
        assertEquals(naddrCoordinate, links.articleCoordinate)
    }

    @Test
    fun acceptsNprofileAuthor() {
        val nprofile = Nip19.nprofileEncode(NprofilePointer(gigiHex))
        val html = """<link rel="author" href="nostr:$nprofile">"""
        assertEquals(gigiHex, Nip21Html.parse(html).authorPubkey)
    }

    @Test
    fun ignoresMalformedAndUnsupportedNostrHrefs() {
        val html = """
            <link rel="author" href="nostr:npub1not-valid">
            <link rel="author" href="https://njump.to/$gigiNpub">
            <link rel="author" href="nostr:note1qq">
            <link rel="alternate" href="nostr:$gigiNpub">
            <link rel="me" href="mailto:hi@example.com">
        """.trimIndent()
        val links = Nip21Html.parse(html)
        assertNull(links.authorPubkey)
        assertNull(links.articleCoordinate)
    }

    @Test
    fun emptyHtmlIsANoOp() {
        assertEquals(Nip21Links(), Nip21Html.parse(""))
        assertEquals(Nip21Links(), Nip21Html.parse("<html><head></head></html>"))
    }
}
