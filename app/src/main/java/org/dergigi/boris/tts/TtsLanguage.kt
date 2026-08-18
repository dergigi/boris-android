package org.dergigi.boris.tts

import java.util.Locale

object TtsLanguage {
    val MODES = listOf(
        "system", "content",
        "en-US", "en-GB", "zh", "es", "hi", "ar", "fr", "pt", "de", "ja", "ru",
    )

    // Priority matches webapp TTSControls.tsx: specific locale > content detect > system.
    fun resolveLanguage(
        mode: String,
        text: String,
        systemLang: String,
        detect: (String) -> String?,
    ): String {
        if (mode != "system" && mode != "content") return mode
        if (mode == "content") detect(text)?.let { return it }
        return systemLang
    }

    fun mode(
        languageMode: String?,
        useSystemLanguage: Boolean,
        detectContentLanguage: Boolean,
    ): String {
        languageMode?.takeIf { it in MODES }?.let { return it }
        if (useSystemLanguage) return "system"
        return if (detectContentLanguage) "content" else "system"
    }

    fun locale(language: String): Locale = Locale.forLanguageTag(language)

    fun detectHeuristic(text: String): String? {
        val sample = text.take(2000)
        var han = 0
        var kana = 0
        var arabic = 0
        var devanagari = 0
        var cyrillic = 0
        var i = 0
        while (i < sample.length) {
            val codePoint = sample.codePointAt(i)
            when (Character.UnicodeScript.of(codePoint)) {
                Character.UnicodeScript.HAN -> han++
                Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> kana++
                Character.UnicodeScript.ARABIC -> arabic++
                Character.UnicodeScript.DEVANAGARI -> devanagari++
                Character.UnicodeScript.CYRILLIC -> cyrillic++
                else -> Unit
            }
            i += Character.charCount(codePoint)
        }
        // Kana wins over Han because Japanese mixes both scripts.
        if (kana >= MIN_SCRIPT_HITS) return "ja"
        if (han >= MIN_SCRIPT_HITS) return "zh"
        if (arabic >= MIN_SCRIPT_HITS) return "ar"
        if (devanagari >= MIN_SCRIPT_HITS) return "hi"
        if (cyrillic >= MIN_SCRIPT_HITS) return "ru"
        return stopwordLanguage(sample)
    }

    private fun stopwordLanguage(sample: String): String? {
        val words = sample.lowercase().split(NON_LETTERS).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        var bestLang: String? = null
        var bestScore = 0
        for ((lang, stops) in STOPWORDS) {
            val score = words.count { it in stops }
            if (score > bestScore) {
                bestScore = score
                bestLang = lang
            }
        }
        return if (bestScore >= MIN_STOPWORD_HITS) bestLang else null
    }

    private const val MIN_SCRIPT_HITS = 4
    private const val MIN_STOPWORD_HITS = 2
    private val NON_LETTERS = Regex("[^\\p{L}]+")
    private val STOPWORDS = listOf(
        "en" to setOf("the", "and", "is", "of", "to", "that", "it", "was", "with", "this"),
        "es" to setOf("el", "los", "las", "una", "por", "para", "está", "como", "pero", "más"),
        "fr" to setOf("le", "les", "des", "est", "une", "dans", "pour", "qui", "sur", "avec"),
        "pt" to setOf("os", "uma", "não", "para", "com", "mais", "isso", "você", "são", "muito"),
        "de" to setOf("der", "die", "das", "und", "ist", "nicht", "ein", "eine", "mit", "von"),
    )
}
