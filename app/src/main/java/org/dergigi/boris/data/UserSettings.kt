package org.dergigi.boris.data

class UserSettings internal constructor(
    private val values: Map<String, JsonValue>,
) {
    val showHighlights: Boolean get() = bool("showHighlights", true)
    val readingFont: String get() = string("readingFont", "source-serif-4")
    val fontSize: Int get() = int("fontSize", 21)
    val highlightStyle: String get() = string("highlightStyle", "marker")
    val highlightColorMine: String get() = string("highlightColorMine", "#fde047")
    val highlightColorFriends: String get() = string("highlightColorFriends", "#f97316")
    val highlightColorNostrverse: String get() = string("highlightColorNostrverse", "#9333ea")
    val defaultHighlightVisibilityNostrverse: Boolean
        get() = bool("defaultHighlightVisibilityNostrverse", true)
    val defaultHighlightVisibilityFriends: Boolean
        get() = bool("defaultHighlightVisibilityFriends", true)
    val defaultHighlightVisibilityMine: Boolean
        get() = bool("defaultHighlightVisibilityMine", true)
    val defaultExploreScopeNostrverse: Boolean
        get() = bool("defaultExploreScopeNostrverse", false)
    val defaultExploreScopeFriends: Boolean
        get() = bool("defaultExploreScopeFriends", true)
    val defaultExploreScopeMine: Boolean
        get() = bool("defaultExploreScopeMine", false)
    val paragraphAlignment: String get() = string("paragraphAlignment", "justify")
    val linkColorDark: String get() = string("linkColorDark", "#38bdf8")
    val linkColorLight: String get() = string("linkColorLight", "#3b82f6")
    val theme: String
        get() = when (val value = string("theme", "system")) {
            "light", "dark" -> value
            else -> "system"
        }
    val darkColorTheme: String
        get() = when (val value = string("darkColorTheme", "midnight")) {
            "black", "charcoal" -> value
            else -> "midnight"
        }
    val lightColorTheme: String
        get() = when (val value = string("lightColorTheme", "sepia")) {
            "paper-white", "ivory" -> value
            else -> "sepia"
        }

    val markerStyle: Boolean get() = highlightStyle != "underline"
    val justifyParagraphs: Boolean get() = paragraphAlignment != "left"
    val fullWidthImages: Boolean get() = bool("fullWidthImages", true)
    val openLinksInReader: Boolean get() = bool("openLinksInReader", true)
    val useLocalRelayAsCache: Boolean get() = bool("useLocalRelayAsCache", true)
    val hideArchivedOnHome: Boolean get() = bool("hideArchivedOnHome", true)
    val hideCompletedOnHome: Boolean get() = bool("hideCompletedOnHome", false)
    val hideNsfwOnHome: Boolean get() = bool("hideNsfwOnHome", false)
    val nsfwWarnInReader: Boolean get() = bool("nsfwWarnInReader", true)
    val homeSectionOrder: List<String> get() = stringList("homeSectionOrder")
    val defaultLibraryView: BookmarkBucket
        get() = when (string("defaultLibraryView", "All")) {
            "Private" -> BookmarkBucket.Private
            "Public" -> BookmarkBucket.Public
            "Web" -> BookmarkBucket.Web
            "Look" -> BookmarkBucket.Look
            "Archive" -> BookmarkBucket.Archive
            else -> BookmarkBucket.All
        }
    val defaultFeedView: String get() = string("defaultFeedView", "All")
    val hideTopBarOnScroll: Boolean get() = bool("hideTopBarOnScroll", true)
    val syncReadingPosition: Boolean get() = bool("syncReadingPosition", true)
    val autoScrollToReadingPosition: Boolean get() = bool("autoScrollToReadingPosition", true)
    val autoMarkAsReadOnCompletion: Boolean get() = bool("autoMarkAsReadOnCompletion", false)
    val archiveClosesReader: Boolean get() = bool("archiveClosesReader", true)
    val volumeButtonScroll: Boolean get() = bool("volumeButtonScroll", true)
    val volumeButtonScrollPercent: Int
        get() = int("volumeButtonScrollPercent", 90).coerceIn(25, 100)
    val ttsDefaultSpeed: Double get() = double("ttsDefaultSpeed", 2.1)
    val ttsLanguageMode: String get() = string("ttsLanguageMode", "content")
    val ttsUseSystemLanguage: Boolean get() = bool("ttsUseSystemLanguage", false)
    val ttsDetectContentLanguage: Boolean get() = bool("ttsDetectContentLanguage", true)
    val ttsFollowAlong: Boolean get() = bool("ttsFollowAlong", true)
    val rssFeeds: List<String> get() = stringList("rssFeeds")
    val zapSplitsEnabled: Boolean get() = bool("zapSplitsEnabled", true)
    val zapSplitHighlighterWeight: Double get() = double("zapSplitHighlighterWeight", 50.0)
    val zapSplitBorisWeight: Double get() = double("zapSplitBorisWeight", 2.1)
    val zapSplitAuthorWeight: Double get() = double("zapSplitAuthorWeight", 50.0)
    val firstTimeDismissed: Boolean get() = bool("firstTimeDismissed", false)

    fun offlineDownloadEnabled(key: String): Boolean = bool(key, true)

    fun isDark(systemDark: Boolean): Boolean = when (theme) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    fun withString(key: String, value: String): UserSettings = overlay(key, JsonValue.Str(value))

    fun withBoolean(key: String, value: Boolean): UserSettings = overlay(key, JsonValue.Bool(value))

    fun withInt(key: String, value: Int): UserSettings = overlay(key, JsonValue.Num(value.toString()))

    fun withDouble(key: String, value: Double): UserSettings {
        val raw = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        return overlay(key, JsonValue.Num(raw))
    }

    fun withStringList(key: String, values: List<String>): UserSettings =
        overlay(key, JsonValue.Raw(JsonMap.stringifyStrings(values)))

    fun resetKeys(keys: Set<String>): UserSettings {
        if (keys.isEmpty()) return this
        val next = LinkedHashMap(values)
        keys.forEach(next::remove)
        return UserSettings(next)
    }

    fun hasNonDefaultValues(keys: Set<String>): Boolean {
        if (keys.isEmpty()) return false
        val defaults = defaults()
        return keys.any { valueForResetComparison(it) != defaults.valueForResetComparison(it) }
    }

    fun toJson(): String = JsonMap.stringify(values)

    fun visibleMine(): Boolean = showHighlights && defaultHighlightVisibilityMine

    fun visibleFriends(): Boolean = showHighlights && defaultHighlightVisibilityFriends

    fun visibleNostrverse(): Boolean = showHighlights && defaultHighlightVisibilityNostrverse

    fun withOwnHighlightsVisible(): UserSettings {
        if (showHighlights && defaultHighlightVisibilityMine) return this
        return withBoolean("showHighlights", true)
            .withBoolean("defaultHighlightVisibilityMine", true)
    }

    private fun overlay(key: String, value: JsonValue): UserSettings {
        val next = LinkedHashMap(values)
        next[key] = value
        return UserSettings(next)
    }

    private fun string(key: String, default: String): String {
        val value = values[key] as? JsonValue.Str ?: return default
        return value.value.ifBlank { default }
    }

    private fun bool(key: String, default: Boolean): Boolean {
        val value = values[key] as? JsonValue.Bool ?: return default
        return value.value
    }

    private fun int(key: String, default: Int): Int {
        val raw = (values[key] as? JsonValue.Num)?.raw ?: return default
        return raw.toDoubleOrNull()?.toInt() ?: default
    }

    private fun stringList(key: String): List<String> {
        val raw = (values[key] as? JsonValue.Raw)?.json ?: return emptyList()
        return JsonMap.parseStringArray(raw)
    }

    private fun double(key: String, default: Double): Double {
        val raw = (values[key] as? JsonValue.Num)?.raw ?: return default
        return raw.toDoubleOrNull() ?: default
    }

    private fun valueForResetComparison(key: String): Any? = when (key) {
        "archiveClosesReader" -> archiveClosesReader
        "autoMarkAsReadOnCompletion" -> autoMarkAsReadOnCompletion
        "autoScrollToReadingPosition" -> autoScrollToReadingPosition
        "darkColorTheme" -> darkColorTheme
        "defaultExploreScopeFriends" -> defaultExploreScopeFriends
        "defaultExploreScopeMine" -> defaultExploreScopeMine
        "defaultExploreScopeNostrverse" -> defaultExploreScopeNostrverse
        "defaultFeedView" -> defaultFeedView
        "defaultHighlightVisibilityFriends" -> defaultHighlightVisibilityFriends
        "defaultHighlightVisibilityMine" -> defaultHighlightVisibilityMine
        "defaultHighlightVisibilityNostrverse" -> defaultHighlightVisibilityNostrverse
        "defaultLibraryView" -> defaultLibraryView
        "fontSize" -> fontSize
        "fullWidthImages" -> fullWidthImages
        "hideArchivedOnHome" -> hideArchivedOnHome
        "hideCompletedOnHome" -> hideCompletedOnHome
        "hideNsfwOnHome" -> hideNsfwOnHome
        "hideTopBarOnScroll" -> hideTopBarOnScroll
        "highlightColorFriends" -> highlightColorFriends
        "highlightColorMine" -> highlightColorMine
        "highlightColorNostrverse" -> highlightColorNostrverse
        "highlightStyle" -> highlightStyle
        "homeSectionOrder" -> homeSectionOrder
        "lightColorTheme" -> lightColorTheme
        "openLinksInReader" -> openLinksInReader
        "paragraphAlignment" -> paragraphAlignment
        "readingFont" -> readingFont
        "rssFeeds" -> rssFeeds
        "showHighlights" -> showHighlights
        "syncReadingPosition" -> syncReadingPosition
        "theme" -> theme
        "ttsDefaultSpeed" -> ttsDefaultSpeed
        "ttsDetectContentLanguage" -> ttsDetectContentLanguage
        "ttsFollowAlong" -> ttsFollowAlong
        "ttsLanguageMode" -> ttsLanguageMode
        "ttsUseSystemLanguage" -> ttsUseSystemLanguage
        "useLocalRelayAsCache" -> useLocalRelayAsCache
        "volumeButtonScroll" -> volumeButtonScroll
        "volumeButtonScrollPercent" -> volumeButtonScrollPercent
        "zapSplitAuthorWeight" -> zapSplitAuthorWeight
        "zapSplitBorisWeight" -> zapSplitBorisWeight
        "zapSplitHighlighterWeight" -> zapSplitHighlighterWeight
        "zapSplitsEnabled" -> zapSplitsEnabled
        ArticleImages.SETTINGS_KEY -> offlineDownloadEnabled(key)
        in OfflineShelf.entries.map { it.settingsKey } -> offlineDownloadEnabled(key)
        else -> values[key]
    }

    companion object {
        fun defaults(): UserSettings = parse(DEFAULT_JSON)

        fun parse(content: String): UserSettings {
            val map = JsonMap.parseObject(content) ?: return defaults()
            return UserSettings(map)
        }
    }
}

private const val DEFAULT_JSON = """{
  "collapseOnArticleOpen":true,
  "defaultViewMode":"compact",
  "showHighlights":true,
  "sidebarCollapsed":true,
  "highlightsCollapsed":true,
  "readingFont":"source-serif-4",
  "fontSize":21,
  "highlightStyle":"marker",
  "highlightColor":"#fde047",
  "highlightColorNostrverse":"#9333ea",
  "highlightColorFriends":"#f97316",
  "highlightColorMine":"#fde047",
  "defaultHighlightVisibilityNostrverse":true,
  "defaultHighlightVisibilityFriends":true,
  "defaultHighlightVisibilityMine":true,
  "defaultExploreScopeNostrverse":false,
  "defaultExploreScopeFriends":true,
  "defaultExploreScopeMine":false,
  "zapSplitHighlighterWeight":50,
  "zapSplitBorisWeight":2.1,
  "zapSplitAuthorWeight":50,
  "useLocalRelayAsCache":true,
  "rebroadcastToAllRelays":false,
  "paragraphAlignment":"justify",
  "fullWidthImages":true,
  "renderVideoLinksAsEmbeds":true,
  "syncReadingPosition":true,
  "autoScrollToReadingPosition":true,
  "autoMarkAsReadOnCompletion":false,
  "archiveClosesReader":true,
  "hideBookmarksWithoutCreationDate":true,
  "ttsUseSystemLanguage":false,
  "ttsDetectContentLanguage":true,
  "ttsLanguageMode":"content",
  "ttsDefaultSpeed":2.1,
  "ttsFollowAlong":true,
  "linkColorDark":"#38bdf8",
  "linkColorLight":"#3b82f6"
}"""
