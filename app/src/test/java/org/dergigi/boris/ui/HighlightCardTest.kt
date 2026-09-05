package org.dergigi.boris.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HighlightCardTest {
    @Test
    fun colorModeUsesMarkerBackground() {
        val style = highlightQuoteMarkStyle(
            color = Color.Yellow,
            onBackground = Color.Black,
            eink = false,
        )

        assertEquals(Color.Yellow.copy(alpha = 0.45f), style.background)
        assertEquals(Color.Black, style.color)
        assertNull(style.textDecoration)
    }

    @Test
    fun einkModeUsesUnderlineWithoutMarkerBackground() {
        val style = highlightQuoteMarkStyle(
            color = Color.Yellow,
            onBackground = Color.Black,
            eink = true,
        )

        assertEquals(Color.Unspecified, style.background)
        assertEquals(Color.Black, style.color)
        assertEquals(TextDecoration.Underline, style.textDecoration)
    }
}
