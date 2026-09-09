package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CrashReporterTest {
    @Test
    fun formatIncludesDiagnosticsAndStackTrace() {
        val report = CrashReporter.format(
            threadName = "main",
            throwable = IllegalStateException("boom"),
            now = Instant.parse("2026-09-09T07:44:00Z"),
            articleUrl = "https://example.com/post",
            breadcrumbs = listOf("home", "reader?url={url}&highlight={highlight}"),
            appVersion = "1.6.11 (210) abc1234",
            android = "14 (SDK 34)",
            device = "Google Pixel 8",
        )
        val lines = report.lines()
        assertEquals("Boris crash report", lines[0])
        assertTrue(report.contains("App: 1.6.11 (210) abc1234"))
        assertTrue(report.contains("Android: 14 (SDK 34)"))
        assertTrue(report.contains("Device: Google Pixel 8"))
        assertTrue(report.contains("Time: 2026-09-09T07:44:00Z"))
        assertTrue(report.contains("Thread: main"))
        assertTrue(report.contains("Article: https://example.com/post"))
        assertTrue(report.contains("Recent screens: home > reader?url={url}&highlight={highlight}"))
        assertTrue(report.contains("java.lang.IllegalStateException: boom"))
        assertTrue(report.contains("CrashReporterTest"))
    }

    @Test
    fun formatOmitsEmptyContext() {
        val report = CrashReporter.format(
            threadName = "main",
            throwable = RuntimeException("x"),
            now = Instant.EPOCH,
            articleUrl = null,
            breadcrumbs = emptyList(),
            appVersion = "v",
            android = "a",
            device = "d",
        )
        assertFalse(report.contains("Article:"))
        assertFalse(report.contains("Recent screens:"))
    }
}
