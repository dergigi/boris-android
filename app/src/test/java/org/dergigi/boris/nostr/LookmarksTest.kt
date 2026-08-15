package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LookmarksTest {
    private val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
    private val eventId = "aa".repeat(32)
    private val coordinate = "30023:$pubkey:my-article"

    @Test
    fun onlyKind7EyesCount() {
        assertTrue(Lookmarks.isLook(reaction("👀", listOf(listOf("e", eventId)))))
        assertFalse(Lookmarks.isLook(reaction("+", listOf(listOf("e", eventId)))))
        assertFalse(Lookmarks.isLook(reaction("👀", listOf(listOf("e", eventId)), kind = 1)))
    }

    @Test
    fun targetPrefersLastETag() {
        val event = reaction(
            "👀",
            listOf(
                listOf("e", "bb".repeat(32)),
                listOf("e", eventId, "wss://nos.lol"),
            ),
        )
        assertEquals(BookmarkRef(BookmarkRefKind.Note, eventId), Lookmarks.targetRef(event))
    }

    @Test
    fun targetPrefersLongFormAddress() {
        val event = reaction(
            "👀",
            listOf(
                listOf("e", eventId),
                listOf("a", coordinate),
            ),
        )
        assertEquals(BookmarkRef(BookmarkRefKind.Article, coordinate), Lookmarks.targetRef(event))
    }

    @Test
    fun ignoresReactionsWithoutATarget() {
        assertNull(Lookmarks.targetRef(reaction("👀", emptyList())))
    }

    private fun reaction(
        content: String,
        tags: List<List<String>>,
        kind: Int = Nip01Event.KIND_REACTION,
    ): Nip01Event = Nip01Event(
        id = "11".repeat(32),
        pubkey = pubkey,
        createdAt = 1,
        kind = kind,
        tags = tags,
        content = content,
        sig = "22".repeat(64),
    )
}
