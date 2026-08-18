package org.dergigi.boris.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsLanguageTest {
    @Test
    fun specificLocaleWinsOverDetectAndSystem() {
        val resolved = TtsLanguage.resolveLanguage(
            mode = "en-US",
            text = "こんにちは、世界。日本語のテキストです。",
            systemLang = "de",
            detect = { "ja" },
        )
        assertEquals("en-US", resolved)
    }

    @Test
    fun contentModeUsesDetect() {
        val resolved = TtsLanguage.resolveLanguage(
            mode = "content",
            text = "irrelevant",
            systemLang = "de",
            detect = { "es" },
        )
        assertEquals("es", resolved)
    }

    @Test
    fun contentModeFallsThroughToSystemWhenDetectIsNull() {
        val resolved = TtsLanguage.resolveLanguage(
            mode = "content",
            text = "irrelevant",
            systemLang = "fr",
            detect = { null },
        )
        assertEquals("fr", resolved)
    }

    @Test
    fun systemModeUsesSystemLanguage() {
        val resolved = TtsLanguage.resolveLanguage(
            mode = "system",
            text = "irrelevant",
            systemLang = "pt",
            detect = { "es" },
        )
        assertEquals("pt", resolved)
    }

    @Test
    fun modeResolutionMatchesWebappFallbacks() {
        assertEquals("en-GB", TtsLanguage.mode("en-GB", useSystemLanguage = false, detectContentLanguage = true))
        assertEquals("system", TtsLanguage.mode(null, useSystemLanguage = true, detectContentLanguage = true))
        assertEquals("content", TtsLanguage.mode(null, useSystemLanguage = false, detectContentLanguage = true))
        assertEquals("system", TtsLanguage.mode(null, useSystemLanguage = false, detectContentLanguage = false))
        assertEquals("content", TtsLanguage.mode("bogus", useSystemLanguage = false, detectContentLanguage = true))
    }

    @Test
    fun detectHeuristicRecognizesScripts() {
        assertEquals("zh", TtsLanguage.detectHeuristic("这是一段中文文本，用来测试语言检测。"))
        assertEquals("ja", TtsLanguage.detectHeuristic("これは日本語のテキストです。ひらがなとカタカナ。"))
        assertEquals("ar", TtsLanguage.detectHeuristic("هذا نص باللغة العربية لاختبار الكشف عن اللغة."))
        assertEquals("hi", TtsLanguage.detectHeuristic("यह भाषा पहचान के लिए हिंदी पाठ है।"))
        assertEquals("ru", TtsLanguage.detectHeuristic("Это русский текст для проверки определения языка."))
    }

    @Test
    fun detectHeuristicRecognizesLatinStopwords() {
        assertEquals("en", TtsLanguage.detectHeuristic("The quick brown fox jumps over the lazy dog and it was fun."))
        assertEquals("de", TtsLanguage.detectHeuristic("Der Hund und die Katze sind nicht im Haus."))
    }

    @Test
    fun detectHeuristicReturnsNullForAmbiguousText() {
        assertNull(TtsLanguage.detectHeuristic("xyzzy plugh 12345"))
    }

    @Test
    fun modesMatchWebappDropdown() {
        assertEquals(
            listOf("system", "content", "en-US", "en-GB", "zh", "es", "hi", "ar", "fr", "pt", "de", "ja", "ru"),
            TtsLanguage.MODES,
        )
    }
}
