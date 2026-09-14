package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FeatureSuggestionTest {
    @Test
    fun formatIncludesSuggestionAndDiagnostics() {
        val report = FeatureSuggestion.format(
            suggestion = "  Add a calmer inbox for reading suggestions.  ",
            now = Instant.parse("2026-09-14T08:00:00Z"),
            appVersion = "1.6.18 (217) abc1234",
            android = "17 (SDK 37)",
            device = "Google Pixel 9 Pro XL",
        )

        val lines = report.lines()
        assertEquals("Boris feature suggestion", lines[0])
        assertTrue(report.contains("Add a calmer inbox for reading suggestions."))
        assertTrue(report.contains("App: 1.6.18 (217) abc1234"))
        assertTrue(report.contains("Android: 17 (SDK 37)"))
        assertTrue(report.contains("Device: Google Pixel 9 Pro XL"))
        assertTrue(report.contains("Time: 2026-09-14T08:00:00Z"))
    }
}
