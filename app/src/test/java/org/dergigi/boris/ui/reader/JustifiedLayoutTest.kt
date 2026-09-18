package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class JustifiedLayoutTest {
    @Test
    fun visualXStretchesBySpacesOnAJustifiedLine() {
        val x = JustifiedLayout.visualX(
            offset = 8,
            lineStart = 0,
            lineEnd = 16,
            lineLeft = 0f,
            lineRight = 1000f,
            naturalLeft = 0f,
            naturalRight = 800f,
            naturalX = 400f,
            stretchUnitsOnLine = 4,
            stretchUnitsBefore = 2,
            atLineStart = false,
            atLineEnd = false,
        )
        assertEquals(500f, x, 0.01f)
    }

    @Test
    fun visualXUsesLineEdgesAtTheEnds() {
        val left = JustifiedLayout.visualX(
            offset = 0,
            lineStart = 0,
            lineEnd = 16,
            lineLeft = 12f,
            lineRight = 1000f,
            naturalLeft = 0f,
            naturalRight = 800f,
            naturalX = 0f,
            stretchUnitsOnLine = 4,
            stretchUnitsBefore = 0,
            atLineStart = true,
            atLineEnd = false,
        )
        val right = JustifiedLayout.visualX(
            offset = 16,
            lineStart = 0,
            lineEnd = 16,
            lineLeft = 12f,
            lineRight = 1000f,
            naturalLeft = 0f,
            naturalRight = 800f,
            naturalX = 800f,
            stretchUnitsOnLine = 4,
            stretchUnitsBefore = 4,
            atLineStart = false,
            atLineEnd = true,
        )
        assertEquals(12f, left, 0.01f)
        assertEquals(1000f, right, 0.01f)
    }

    @Test
    fun visualXLeavesLeftAlignedLinesAlone() {
        val x = JustifiedLayout.visualX(
            offset = 8,
            lineStart = 0,
            lineEnd = 16,
            lineLeft = 0f,
            lineRight = 800f,
            naturalLeft = 0f,
            naturalRight = 800f,
            naturalX = 400f,
            stretchUnitsOnLine = 4,
            stretchUnitsBefore = 2,
            atLineStart = false,
            atLineEnd = false,
        )
        assertEquals(400f, x, 0.01f)
    }

    @Test
    fun stretchUnitsIncludeCjkGapsAndSpaces() {
        val spaced = "中文 https://example.com"
        val adjacent = "中文https://example.com"

        assertEquals(2, JustifiedLayout.stretchUnitCount(spaced, 0, spaced.length))
        assertEquals(2, JustifiedLayout.stretchUnitCount(adjacent, 0, adjacent.length))
    }

    @Test
    fun visualXDistributesJustificationAcrossCjkGaps() {
        val x = JustifiedLayout.visualX(
            offset = 2,
            lineStart = 0,
            lineEnd = 7,
            lineLeft = 0f,
            lineRight = 700f,
            naturalLeft = 0f,
            naturalRight = 500f,
            naturalX = 200f,
            stretchUnitsOnLine = 4,
            stretchUnitsBefore = 2,
            atLineStart = false,
            atLineEnd = false,
        )

        assertEquals(300f, x, 0.01f)
    }

    @Test
    fun offsetForXFindsTheCursorOnAStretchedLine() {
        val cursors = floatArrayOf(0f, 50f, 150f, 250f, 400f, 550f, 700f, 1000f)
        val offset = JustifiedLayout.offsetForX(260f, 0, cursors.lastIndex) { cursors[it] }
        assertEquals(3, offset)
    }

    @Test
    fun offsetForXSnapsToTheNearerSideOfAGlyph() {
        val cursors = floatArrayOf(0f, 0f, 100f, 200f)
        val left = JustifiedLayout.offsetForX(20f, 0, 3) { cursors[it] }
        val right = JustifiedLayout.offsetForX(80f, 0, 3) { cursors[it] }
        assertEquals(1, left)
        assertEquals(2, right)
    }
}
