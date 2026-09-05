package org.dergigi.boris.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class HighlightMarksStrokeTest {
    @Test
    fun colorModeKeepsASingleSolidStroke() {
        val mine = HighlightMarks.highlightStroke(eink = false, mine = true)
        val other = HighlightMarks.highlightStroke(eink = false, mine = false)
        assertEquals(InkUnderline.Solid, mine.ink)
        assertEquals(InkUnderline.Solid, other.ink)
        assertEquals(HighlightMarks.ColorStroke, mine.width)
        assertEquals(HighlightMarks.ColorStroke, other.width)
    }

    @Test
    fun einkMineIsThickSolid() {
        val stroke = HighlightMarks.highlightStroke(eink = true, mine = true)
        assertEquals(InkUnderline.Solid, stroke.ink)
        assertEquals(HighlightMarks.MineStroke, stroke.width)
    }

    @Test
    fun einkOthersAreDashed() {
        val stroke = HighlightMarks.highlightStroke(eink = true, mine = false)
        assertEquals(InkUnderline.Dashed, stroke.ink)
        assertEquals(HighlightMarks.OtherStroke, stroke.width)
    }
}
