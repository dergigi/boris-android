package org.dergigi.boris.ui.reader

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
}
