package org.dergigi.boris.ui.reader

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderMetaTest {
    private val mine = Color.Yellow
    private val friends = Color(0xFFFF9800)
    private val other = Color(0xFF9C27B0)

    private fun highlight(mine: Boolean = false, friend: Boolean = false) =
        PaintedHighlight(id = "id", quote = "q", mine = mine, friend = friend)

    @Test
    fun pillColorPrefersMine() {
        val highlights = listOf(highlight(), highlight(friend = true), highlight(mine = true))
        assertEquals(mine, highlightPillColor(highlights, mine, friends, other))
    }

    @Test
    fun pillColorFriendsWhenNoneMine() {
        val highlights = listOf(highlight(), highlight(friend = true))
        assertEquals(friends, highlightPillColor(highlights, mine, friends, other))
    }

    @Test
    fun pillColorNostrverseOtherwise() {
        assertEquals(other, highlightPillColor(listOf(highlight()), mine, friends, other))
    }

    @Test
    fun readingTimeUsesWordCount() {
        val text = List(200) { "word" }.joinToString(" ")
        assertEquals("1 min read", readingTimeLabel(text))
        assertEquals("2 min read", readingTimeLabel(text + " " + List(200) { "more" }.joinToString(" ")))
    }

    @Test
    fun readingTimeEmptyIsNull() {
        assertNull(readingTimeLabel(""))
        assertNull(readingTimeLabel("   "))
    }

    @Test
    fun highlightCountHidesZero() {
        assertNull(highlightCountLabel(0))
        assertEquals("1 highlight", highlightCountLabel(1))
        assertEquals("9 highlights", highlightCountLabel(9))
    }
}
