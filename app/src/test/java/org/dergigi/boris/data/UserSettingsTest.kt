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
    }

    @Test
    fun parseReadsKnownKeys() {
        val settings = UserSettings.parse(
            """{"fontSize":24,"highlightStyle":"underline","showHighlights":false,"paragraphAlignment":"left"}""",
        )
        assertEquals(24, settings.fontSize)
        assertEquals("underline", settings.highlightStyle)
        assertFalse(settings.showHighlights)
        assertEquals("left", settings.paragraphAlignment)
        assertEquals("source-serif-4", settings.readingFont)
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
}
