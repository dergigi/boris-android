package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Nip51Test {
    @Test
    fun parseTagsReadsArticlesUrlsAndNotes() {
        val refs = Nip51.parseTags(
            listOf(
                listOf("a", "30023:${PUBKEY}:my-article", "wss://relay.example"),
                listOf("r", "https://www.citadel21.com/the-paranoid-wallet"),
                listOf("e", EVENT_ID),
                listOf("t", "bitcoin"),
                listOf("p", PUBKEY),
            ),
        )
        assertEquals(
            listOf(
                BookmarkRef(BookmarkRefKind.Article, "30023:${PUBKEY}:my-article"),
                BookmarkRef(BookmarkRefKind.Url, "https://www.citadel21.com/the-paranoid-wallet"),
                BookmarkRef(BookmarkRefKind.Note, EVENT_ID),
            ),
            refs,
        )
    }

    @Test
    fun hiddenRefsParseEncryptedTagJson() {
        val plaintext =
            """[["a","30023:$PUBKEY:essay"],["r","example.com/post"],["e","$EVENT_ID"]]"""
        val refs = Nip51.hiddenRefs(plaintext)
        assertEquals(BookmarkRefKind.Article, refs[0].kind)
        assertEquals("30023:${PUBKEY}:essay", refs[0].value)
        assertEquals(BookmarkRefKind.Url, refs[1].kind)
        assertEquals("https://example.com/post", refs[1].value)
        assertEquals(BookmarkRefKind.Note, refs[2].kind)
    }

    @Test
    fun ignoresNonArticleAddresses() {
        assertEquals(
            emptyList<BookmarkRef>(),
            Nip51.parseTags(listOf(listOf("a", "30024:$PUBKEY:draft"))),
        )
    }

    @Test
    fun looksEncryptedAndDetectsNip04() {
        assertFalse(Nip51.looksEncrypted(""))
        assertTrue(Nip51.looksEncrypted("AgABcipher"))
        assertTrue(Nip51.isNip04("Y2lwaGVy?iv=aXY="))
        assertFalse(Nip51.isNip04("AgABcipher"))
        assertNull(Nip51.parseTagArray("not json"))
    }

    companion object {
        private const val PUBKEY = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        private const val EVENT_ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}
