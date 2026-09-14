package org.dergigi.boris.data

import android.os.Build
import org.dergigi.boris.BuildConfig
import org.dergigi.boris.nostr.ClientKeypair
import org.dergigi.boris.nostr.Nip17
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import java.time.Instant

object FeatureSuggestion {
    private const val MAX_SUGGESTION_CHARS = 4_000

    fun send(suggestion: String): Boolean {
        val trimmed = suggestion.trim().take(MAX_SUGGESTION_CHARS)
        if (trimmed.isBlank()) return false
        val message = format(trimmed)
        val sender = ClientKeypair.generate()
        val wrap = try {
            Nip17.giftWrap(message, CrashReporter.RECIPIENT_HEX, sender)
        } finally {
            sender.privkey.fill(0)
        }
        val relays = runCatching {
            RelayQuery.fetchDmRelays(CrashReporter.RECIPIENT_HEX)
        }.getOrDefault(emptyList())
        return RelayQuery.publish((relays + RelayList.FALLBACK).distinct(), wrap).remoteOk
    }

    internal fun format(
        suggestion: String,
        now: Instant = Instant.now(),
        appVersion: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.GIT_COMMIT}",
        android: String = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
        device: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    ): String = buildString {
        appendLine("Boris feature suggestion")
        appendLine()
        appendLine(suggestion.trim().take(MAX_SUGGESTION_CHARS))
        appendLine()
        appendLine("App: $appVersion")
        appendLine("Android: $android")
        appendLine("Device: $device")
        appendLine("Time: $now")
    }
}
