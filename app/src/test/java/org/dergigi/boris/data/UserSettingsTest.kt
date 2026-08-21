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
        assertTrue(settings.openLinksInReader)
        assertTrue(settings.volumeButtonScroll)
        assertEquals(90, settings.volumeButtonScrollPercent)
        assertTrue(settings.archiveClosesReader)
        assertTrue(settings.useLocalRelayAsCache)
        assertFalse(settings.hideArchivedOnHome)
        assertEquals(2.1, settings.ttsDefaultSpeed, 0.0)
        assertEquals("content", settings.ttsLanguageMode)
        assertFalse(settings.ttsUseSystemLanguage)
        assertTrue(settings.ttsDetectContentLanguage)
        assertTrue(settings.ttsFollowAlong)
        assertFalse(settings.firstTimeDismissed)
        assertTrue(settings.offlineDownloadEnabled("offlineDownloadImages"))
    }

    @Test
    fun firstTimeDismissedReadsAndRoundTrips() {
        assertTrue(UserSettings.parse("""{"firstTimeDismissed":true}""").firstTimeDismissed)
        val updated = UserSettings.defaults().withBoolean("firstTimeDismissed", true)
        assertTrue(updated.firstTimeDismissed)
        assertTrue(UserSettings.parse(updated.toJson()).firstTimeDismissed)
    }

    @Test
    fun parseReadsKnownKeys() {
        val settings = UserSettings.parse(
            """{"fontSize":24,"highlightStyle":"underline","showHighlights":false,"paragraphAlignment":"left","fullWidthImages":false,"openLinksInReader":false}""",
        )
        assertEquals(24, settings.fontSize)
        assertEquals("underline", settings.highlightStyle)
        assertFalse(settings.showHighlights)
        assertEquals("left", settings.paragraphAlignment)
        assertEquals("source-serif-4", settings.readingFont)
        assertFalse(settings.fullWidthImages)
        assertFalse(settings.openLinksInReader)
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
    fun ownHighlightsVisibleEnablesGlobalAndMineOnly() {
        val settings = UserSettings.parse(
            """{"showHighlights":false,"defaultHighlightVisibilityMine":false,"defaultHighlightVisibilityFriends":false,"defaultHighlightVisibilityNostrverse":false}""",
        )
        val updated = settings.withOwnHighlightsVisible()
        assertTrue(updated.showHighlights)
        assertTrue(updated.defaultHighlightVisibilityMine)
        assertFalse(updated.defaultHighlightVisibilityFriends)
        assertFalse(updated.defaultHighlightVisibilityNostrverse)
        assertTrue(updated.visibleMine())
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

    @Test
    fun rssFeedsRoundTrip() {
        val settings = UserSettings.defaults()
        assertTrue(settings.rssFeeds.isEmpty())
        val feeds = listOf("https://dergigi.com/feed.xml", "https://example.com/rss")
        val updated = settings.withStringList("rssFeeds", feeds)
        assertEquals(feeds, updated.rssFeeds)
        val roundTrip = UserSettings.parse(updated.toJson())
        assertEquals(feeds, roundTrip.rssFeeds)
        val removed = roundTrip.withStringList("rssFeeds", feeds - feeds.first())
        assertEquals(listOf("https://example.com/rss"), removed.rssFeeds)
    }

    @Test
    fun rssFeedsIgnoresMalformedValues() {
        assertTrue(UserSettings.parse("""{"rssFeeds":"nope"}""").rssFeeds.isEmpty())
        assertTrue(UserSettings.parse("""{"rssFeeds":[1,2]}""").rssFeeds.isEmpty())
        assertEquals(
            listOf("https://a.com/feed"),
            UserSettings.parse("""{"rssFeeds":["https://a.com/feed"]}""").rssFeeds,
        )
    }

    @Test
    fun defaultLibraryViewReadsAndFallsBack() {
        assertEquals(
            BookmarkBucket.All,
            UserSettings.defaults().defaultLibraryView,
        )
        assertEquals(
            BookmarkBucket.Private,
            UserSettings.parse("""{"defaultLibraryView":"Private"}""").defaultLibraryView,
        )
        assertEquals(
            BookmarkBucket.Public,
            UserSettings.parse("""{"defaultLibraryView":"Public"}""").defaultLibraryView,
        )
        assertEquals(
            BookmarkBucket.Look,
            UserSettings.parse("""{"defaultLibraryView":"Look"}""").defaultLibraryView,
        )
        assertEquals(
            BookmarkBucket.All,
            UserSettings.parse("""{"defaultLibraryView":"nope"}""").defaultLibraryView,
        )
        val updated = UserSettings.defaults().withString("defaultLibraryView", "Archive")
        assertEquals(BookmarkBucket.Archive, updated.defaultLibraryView)
        assertEquals(
            BookmarkBucket.Archive,
            UserSettings.parse(updated.toJson()).defaultLibraryView,
        )
    }

    @Test
    fun volumeButtonScrollReadsAndClamps() {
        val off = UserSettings.parse("""{"volumeButtonScroll":false,"volumeButtonScrollPercent":50}""")
        assertFalse(off.volumeButtonScroll)
        assertEquals(50, off.volumeButtonScrollPercent)
        val high = UserSettings.parse("""{"volumeButtonScrollPercent":140}""")
        assertEquals(100, high.volumeButtonScrollPercent)
        val low = UserSettings.parse("""{"volumeButtonScrollPercent":10}""")
        assertEquals(25, low.volumeButtonScrollPercent)
    }

    @Test
    fun archiveClosesReaderDefaultsOnAndCanBeDisabled() {
        assertTrue(UserSettings.defaults().archiveClosesReader)
        assertFalse(UserSettings.parse("""{"archiveClosesReader":false}""").archiveClosesReader)
        val updated = UserSettings.defaults().withBoolean("archiveClosesReader", false)
        assertFalse(UserSettings.parse(updated.toJson()).archiveClosesReader)
    }
}
