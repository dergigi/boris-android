package org.dergigi.boris.ui.theme

import androidx.compose.ui.graphics.Color
import org.dergigi.boris.data.DisplayType
import org.dergigi.boris.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayLookTest {
    private val settings = UserSettings.defaults()

    @Test
    fun colorModeUsesSavedHighlightColors() {
        val look = resolveDisplayLook(settings, DisplayType.Color, dark = false)
        assertFalse(look.eink)
        assertFalse(look.underline)
        assertEquals(HighlightMine, look.mine)
        assertEquals(HighlightFriends, look.friends)
        assertEquals(HighlightFoaf, look.foaf)
        assertEquals(HighlightOther, look.nostrverse)
        assertEquals(Color(0xFF3B82F6), look.link)
    }

    @Test
    fun einkForcesUnderlineAndInkSteps() {
        val look = resolveDisplayLook(
            settings.withString("highlightStyle", "marker"),
            DisplayType.Eink,
            dark = false,
        )
        assertTrue(look.eink)
        assertTrue(look.underline)
        assertEquals(EinkInk, look.mine)
        assertEquals(EinkFriends, look.friends)
        assertEquals(EinkFoaf, look.foaf)
        assertEquals(EinkNostrverse, look.nostrverse)
        assertEquals(Color.Black, look.link)
    }

    @Test
    fun einkDarkInvertsInk() {
        val look = resolveDisplayLook(settings, DisplayType.Eink, dark = true)
        assertEquals(EinkInkDark, look.mine)
        assertEquals(Color.White, look.link)
        assertTrue(look.underline)
    }

    @Test
    fun einkLightSchemeIsBlackOnWhite() {
        val scheme = borisColorScheme(settings, darkTheme = false, DisplayType.Eink)
        assertEquals(Color.Black, scheme.primary)
        assertEquals(Color.Black, scheme.onBackground)
        assertEquals(Paper, scheme.background)
    }

    @Test
    fun einkDarkSchemeIsWhiteOnBlack() {
        val scheme = borisColorScheme(settings, darkTheme = true, DisplayType.Eink)
        assertEquals(Color.White, scheme.primary)
        assertEquals(Color.White, scheme.onBackground)
        assertEquals(Black, scheme.background)
    }

    @Test
    fun colorModeKeepsIndigo() {
        val scheme = borisColorScheme(settings, darkTheme = false, DisplayType.Color)
        assertEquals(Indigo600, scheme.primary)
    }
}
