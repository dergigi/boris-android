package org.dergigi.boris.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromeColorTest {
    @Test
    fun yellowOnSepiaGetsDarkerForChrome() {
        val chrome = ChromeColor.of(HighlightMine, Sepia)
        assertTrue(ChromeColor.contrast(HighlightMine, Sepia) < 3f)
        assertTrue(ChromeColor.contrast(chrome, Sepia) >= 3f)
        assertTrue(chrome.luminance() < HighlightMine.luminance())
    }

    @Test
    fun yellowOnPaperGetsDarkerForChrome() {
        val chrome = ChromeColor.of(HighlightMine, Paper)
        assertTrue(ChromeColor.contrast(chrome, Paper) >= 3f)
    }

    @Test
    fun orangeOnSepiaMeetsChromeContrast() {
        val chrome = ChromeColor.of(HighlightFriends, Sepia)
        assertTrue(ChromeColor.contrast(chrome, Sepia) >= 3f)
    }

    @Test
    fun purpleOnSepiaStaysPut() {
        val chrome = ChromeColor.of(HighlightOther, Sepia)
        assertEquals(HighlightOther, chrome)
    }

    @Test
    fun yellowOnMidnightStaysPut() {
        val chrome = ChromeColor.of(HighlightMine, Black)
        assertEquals(HighlightMine, chrome)
    }
}
