package org.dergigi.boris.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrivateBookmarksTest {
    private val pubkey = "aa".repeat(32)
    private val tags = listOf(listOf("r", "https://example.com/post"))

    @After
    fun tearDown() {
        PrivateBookmarks.clear()
    }

    @Test
    fun returnsTagsForTheSameCiphertext() {
        PrivateBookmarks.remember(pubkey, "cipher-1", tags)
        assertEquals(tags, PrivateBookmarks.tagsFor(pubkey, "cipher-1"))
        assertEquals(tags, PrivateBookmarks.tagsFor(pubkey.uppercase(), "cipher-1"))
    }

    @Test
    fun locksAgainWhenTheCiphertextChanges() {
        PrivateBookmarks.remember(pubkey, "cipher-1", tags)
        assertNull(PrivateBookmarks.tagsFor(pubkey, "cipher-2"))
    }

    @Test
    fun doesNotLeakAcrossIdentities() {
        PrivateBookmarks.remember(pubkey, "cipher-1", tags)
        assertNull(PrivateBookmarks.tagsFor("bb".repeat(32), "cipher-1"))
    }

    @Test
    fun ignoresBlankCiphertext() {
        PrivateBookmarks.remember(pubkey, "", tags)
        assertNull(PrivateBookmarks.tagsFor(pubkey, ""))
    }

    @Test
    fun clearForgetsEverything() {
        PrivateBookmarks.remember(pubkey, "cipher-1", tags)
        PrivateBookmarks.clear()
        assertNull(PrivateBookmarks.tagsFor(pubkey, "cipher-1"))
    }
}
