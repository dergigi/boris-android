package org.dergigi.boris.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object PublishedTime {
    private val labelFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val htmlMeta = Regex(
        """<(?:meta)[^>]+(?:property|name)\s*=\s*["'](?:og:)?article:published_time["'][^>]*content\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE,
    )
    private val htmlMetaReversed = Regex(
        """<(?:meta)[^>]+content\s*=\s*["']([^"']+)["'][^>]*(?:property|name)\s*=\s*["'](?:og:)?article:published_time["']""",
        RegexOption.IGNORE_CASE,
    )
    private val jsonLd = Regex(
        """"datePublished"\s*:\s*"([^"]+)"""",
        RegexOption.IGNORE_CASE,
    )
    fun parse(raw: String?): Long? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        trimmed.toLongOrNull()?.let { unix ->
            val seconds = if (unix > 10_000_000_000L) unix / 1000 else unix
            if (seconds in 1_000_000_000L..9_999_999_999L) return seconds
        }
        try {
            return Instant.parse(trimmed).epochSecond
        } catch (_: DateTimeParseException) {
        }
        try {
            return LocalDate.parse(trimmed).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        } catch (_: DateTimeParseException) {
        }
        return null
    }

    fun fromHtml(html: String): Long? {
        val raw = htmlMeta.find(html)?.groupValues?.getOrNull(1)
            ?: htmlMetaReversed.find(html)?.groupValues?.getOrNull(1)
            ?: jsonLd.find(html)?.groupValues?.getOrNull(1)
        return parse(raw)
    }

    fun label(epochSeconds: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochSecond(epochSeconds).atZone(zone).toLocalDate().format(labelFormat)
    }
}
