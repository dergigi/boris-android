package org.dergigi.boris.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {
    private val groups = listOf(
        listOf(SettingsCategory.Appearance, SettingsCategory.Reading, SettingsCategory.Tts),
        listOf(SettingsCategory.Home, SettingsCategory.Library, SettingsCategory.Airplane),
        listOf(SettingsCategory.About),
    )

    private val texts = mapOf(
        SettingsCategory.Appearance to listOf("Appearance", "Theme, dark and light colors", "Sepia"),
        SettingsCategory.Reading to listOf("Reading", "Font, size, alignment, weblinks", "Font Size"),
        SettingsCategory.Tts to listOf("Text-to-Speech", "Speed, voice, preview, follow-along"),
        SettingsCategory.Home to listOf("Home", "Sections, filters", "Hide NSFW articles"),
        SettingsCategory.Library to listOf("Library", "Default view"),
        SettingsCategory.Airplane to listOf("Airplane mode", "Downloads, storage, local relays", "Citrine"),
        SettingsCategory.About to listOf("About", "Boris, tutorial, FAQ, support, links"),
    )

    @Test
    fun emptyQueryKeepsGroupsUnchanged() {
        assertEquals(groups, SettingsSearch.filterGroups(groups, "", ::textsFor))
        assertEquals(groups, SettingsSearch.filterGroups(groups, "   ", ::textsFor))
    }

    @Test
    fun queryMatchesTitleAndDropsEmptyGroups() {
        val filtered = SettingsSearch.filterGroups(groups, "reading", ::textsFor)
        assertEquals(listOf(listOf(SettingsCategory.Reading)), filtered)
    }

    @Test
    fun queryMatchesRowLabel() {
        val filtered = SettingsSearch.filterGroups(groups, "nsfw", ::textsFor)
        assertEquals(listOf(listOf(SettingsCategory.Home)), filtered)
    }

    @Test
    fun queryMatchesSummaryAcrossAGroup() {
        val filtered = SettingsSearch.filterGroups(groups, "theme", ::textsFor)
        assertEquals(listOf(listOf(SettingsCategory.Appearance)), filtered)
    }

    @Test
    fun unknownQueryYieldsNoGroups() {
        assertTrue(SettingsSearch.filterGroups(groups, "zzzz", ::textsFor).isEmpty())
    }

    private fun textsFor(category: SettingsCategory): List<String> = texts[category].orEmpty()
}
