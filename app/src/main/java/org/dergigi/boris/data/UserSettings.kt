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
    val volumeButtonScroll: Boolean get() = bool("volumeButtonScroll", true)
    val volumeButtonScrollPercent: Int
        get() = int("volumeButtonScrollPercent", 90).coerceIn(25, 100)

    fun isDark(systemDark: Boolean): Boolean = when (theme) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    fun withString(key: String, value: String): UserSettings = overlay(key, JsonValue.Str(value))

    fun withBoolean(key: String, value: Boolean): UserSettings = overlay(key, JsonValue.Bool(value))

    fun withInt(key: String, value: Int): UserSettings = overlay(key, JsonValue.Num(value.toString()))

    fun toJson(): String = JsonMap.stringify(values)

    fun visibleMine(): Boolean = showHighlights && defaultHighlightVisibilityMine

    fun visibleFriends(): Boolean = showHighlights && defaultHighlightVisibilityFriends

    fun visibleNostrverse(): Boolean = showHighlights && defaultHighlightVisibilityNostrverse

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
  "hideBookmarksWithoutCreationDate":true,
  "ttsUseSystemLanguage":false,
  "ttsDetectContentLanguage":true,
  "ttsLanguageMode":"content",
  "ttsDefaultSpeed":2.1,
  "linkColorDark":"#38bdf8",
  "linkColorLight":"#3b82f6"
}"""
