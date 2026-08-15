package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {
    @Test
    fun defaultsMatchWebappReadingDisplay() {
        val settings = UserSettings.defaults()
        assertEquals("source-serif-4", settings.readingFont)
        assertEquals(21, settings.fontSize)
        assertEquals("marker", settings.highlightStyle)
        assertEquals("#fde047", settings.highlightColorMine)
        assertEquals("#f97316", settings.highlightColorFriends)
        assertEquals("#9333ea", settings.highlightColorNostrverse)
        assertTrue(settings.showHighlights)
        assertTrue(settings.defaultHighlightVisibilityMine)
        assertTrue(settings.defaultHighlightVisibilityFriends)
        assertTrue(settings.defaultHighlightVisibilityNostrverse)
        assertEquals("justify", settings.paragraphAlignment)
        assertEquals("#38bdf8", settings.linkColorDark)
        assertEquals("#3b82f6", settings.linkColorLight)
        assertEquals("system", settings.theme)
        assertEquals("midnight", settings.darkColorTheme)
        assertEquals("sepia", settings.lightColorTheme)
        assertFalse(settings.defaultExploreScopeNostrverse)
        assertTrue(settings.defaultExploreScopeFriends)
        assertFalse(settings.defaultExploreScopeMine)
        assertTrue(settings.fullWidthImages)
    }

    @Test
    fun parseReadsKnownKeys() {
        val settings = UserSettings.parse(
            """{"fontSize":24,"highlightStyle":"underline","showHighlights":false,"paragraphAlignment":"left","fullWidthImages":false}""",
        )
        assertEquals(24, settings.fontSize)
        assertEquals("underline", settings.highlightStyle)
        assertFalse(settings.showHighlights)
        assertEquals("left", settings.paragraphAlignment)
        assertEquals("source-serif-4", settings.readingFont)
        assertFalse(settings.fullWidthImages)
    }

    @Test
    fun overlayKeepsExtraKeys() {
        val loaded = UserSettings.parse(
            """{"fontSize":21,"zapSplitBorisWeight":2.1,"theme":"dark","nested":{"ok":true}}""",
        )
        val updated = loaded.withInt("fontSize", 28).withString("readingFont", "inter")
        val json = updated.toJson()
        assertTrue(json.contains("\"zapSplitBorisWeight\":2.1"))
        assertTrue(json.contains("\"theme\":\"dark\""))
        assertTrue(json.contains("\"nested\":{\"ok\":true}"))
        assertTrue(json.contains("\"fontSize\":28"))
        assertTrue(json.contains("\"readingFont\":\"inter\""))
        val roundTrip = UserSettings.parse(json)
        assertEquals(28, roundTrip.fontSize)
        assertEquals("inter", roundTrip.readingFont)
    }

    @Test
    fun missingBooleanDefaultsOn() {
        val settings = UserSettings.parse("""{"readingFont":"lora"}""")
        assertTrue(settings.showHighlights)
        assertTrue(settings.defaultHighlightVisibilityMine)
        assertTrue(settings.visibleMine())
        assertTrue(settings.visibleNostrverse())
    }

    @Test
    fun hideHighlightsTurnsVisibilityOff() {
        val settings = UserSettings.parse("""{"showHighlights":false}""")
        assertFalse(settings.visibleMine())
        assertFalse(settings.visibleNostrverse())
    }

    @Test
    fun themeKeysMatchWebappDefaultsAndIds() {
        val loaded = UserSettings.parse(
            """{"theme":"dark","darkColorTheme":"black","lightColorTheme":"ivory"}""",
        )
        assertEquals("dark", loaded.theme)
        assertEquals("black", loaded.darkColorTheme)
        assertEquals("ivory", loaded.lightColorTheme)
        assertTrue(loaded.isDark(systemDark = false))
        val light = UserSettings.parse("""{"theme":"light"}""")
        assertFalse(light.isDark(systemDark = true))
        val system = UserSettings.parse("""{}""")
        assertEquals("system", system.theme)
        assertTrue(system.isDark(systemDark = true))
        assertFalse(system.isDark(systemDark = false))
        val unknown = UserSettings.parse("""{"theme":"neon","darkColorTheme":"navy"}""")
        assertEquals("system", unknown.theme)
        assertEquals("midnight", unknown.darkColorTheme)
    }
}
