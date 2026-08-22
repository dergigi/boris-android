package org.dergigi.boris.ui.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSelectionStateTest {
    @Test
    fun beginThenExtendKeepsTheAnchor() {
        val state = ReaderSelectionState()
        val owner = Any()
        state.begin(owner, "the quick brown fox", TextRange(4, 9))
        assertEquals("quick", state.selectedText)
        state.extendTo(15)
        assertEquals("quick brown", state.selectedText)
        state.extendTo(0)
        assertEquals("the quick", state.selectedText)
    }

    @Test
    fun moveBoundUsesVisualMinAndMax() {
        val state = ReaderSelectionState()
        val owner = Any()
        state.begin(owner, "abcdef", TextRange(2, 4))
        state.moveBound(movingMin = true, offset = 1)
        assertEquals("bcd", state.selectedText)
        state.moveBound(movingMin = false, offset = 5)
        assertEquals("bcde", state.selectedText)
    }

    @Test
    fun clearDropsTheSelection() {
        val state = ReaderSelectionState()
        state.begin(Any(), "hello", TextRange(0, 5))
        assertTrue(state.hasSelection)
        state.clear()
        assertFalse(state.hasSelection)
        assertEquals("", state.selectedText)
    }

    @Test
    fun beginLeavesToolbarUnreadyUntilLayout() {
        val state = ReaderSelectionState()
        state.begin(Any(), "hello", TextRange(0, 5))
        assertTrue(state.hasSelection)
        assertFalse(state.toolbarReady)
    }

    @Test
    fun extendAndMoveBoundLeaveToolbarHidden() {
        val state = ReaderSelectionState()
        val owner = Any()
        state.begin(owner, "the quick brown fox", TextRange(4, 9))
        assertFalse(state.toolbarReady)
        state.extendTo(15)
        assertEquals("quick brown", state.selectedText)
        assertFalse(state.toolbarReady)
        state.moveBound(movingMin = false, offset = 19)
        assertEquals("quick brown fox", state.selectedText)
        assertFalse(state.toolbarReady)
        assertTrue(state.hasSelection)
    }

    @Test
    fun selectAllKeepsTheWholeParagraph() {
        val state = ReaderSelectionState()
        val owner = Any()
        state.begin(owner, "hello world", TextRange(0, 5))
        state.selectAll(owner, "hello world")
        assertEquals("hello world", state.selectedText)
        assertTrue(state.hasSelection)
    }

    @Test
    fun loupeClearsWithSelection() {
        val state = ReaderSelectionState()
        state.begin(Any(), "hello world", TextRange(0, 5))
        state.showLoupe(Offset(12f, 8f))
        assertEquals(Offset(12f, 8f), state.loupeCenter)
        state.clear()
        assertEquals(Offset.Unspecified, state.loupeCenter)
        assertFalse(state.hasSelection)
    }

    @Test
    fun hideLoupeKeepsTheSelection() {
        val state = ReaderSelectionState()
        state.begin(Any(), "hello world", TextRange(0, 5))
        state.showLoupe(Offset(4f, 2f))
        state.hideLoupe()
        assertEquals(Offset.Unspecified, state.loupeCenter)
        assertEquals("hello", state.selectedText)
    }

    @Test
    fun loupeSitsAboveTheTouchPoint() {
        val center = loupeDisplayCenter(
            source = Offset(20f, 200f),
            sourceWindow = Offset(20f, 400f),
            liftPx = 80f,
            minWindowY = 100f,
        )
        assertEquals(20f, center.x, 0.01f)
        assertEquals(120f, center.y, 0.01f)
    }

    @Test
    fun loupeStaysBelowTheTopSafeEdge() {
        val center = loupeDisplayCenter(
            source = Offset(20f, 30f),
            sourceWindow = Offset(20f, 140f),
            liftPx = 80f,
            minWindowY = 100f,
        )
        assertEquals(20f, center.x, 0.01f)
        assertEquals(-10f, center.y, 0.01f)
    }
}
