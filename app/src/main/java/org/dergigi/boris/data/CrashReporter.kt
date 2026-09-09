package org.dergigi.boris.data

import android.content.Context
import android.os.Build
import org.dergigi.boris.BuildConfig
import org.dergigi.boris.nostr.ClientKeypair
import org.dergigi.boris.nostr.Nip17
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.ZapSplits
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.util.ArrayDeque

/**
 * Opt-in crash reports (#79). An uncaught exception writes a plain-text
 * report to disk; the next launch offers to send it as a NIP-17 DM from a
 * one-time key, copy it, or drop it. Nothing leaves the device otherwise.
 */
object CrashReporter {
    const val RECIPIENT_HEX = ZapSplits.BORIS_PUBKEY
    private const val FILE_NAME = "crash_report.txt"
    private const val MAX_BREADCRUMBS = 12
    private const val MAX_STACK_CHARS = 8_000

    private lateinit var file: File

    @Volatile
    private var articleUrl: String? = null
    private val breadcrumbs = ArrayDeque<String>()

    fun install(context: Context) {
        file = File(context.filesDir, FILE_NAME)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { file.writeText(format(thread.name, throwable)) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Route pattern plus the reader URL, if any. Route patterns carry no arguments. */
    fun onNavigate(route: String?, articleUrl: String?) {
        this.articleUrl = articleUrl
        val crumb = route ?: return
        synchronized(breadcrumbs) {
            if (breadcrumbs.peekLast() == crumb) return
            breadcrumbs.addLast(crumb)
            while (breadcrumbs.size > MAX_BREADCRUMBS) breadcrumbs.removeFirst()
        }
    }

    fun pending(): String? = file.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }

    fun clear() {
        runCatching { file.delete() }
    }

    /** Publishes [report] to the recipient's inbox relays. Blocking; call off the main thread. */
    fun send(report: String): Boolean {
        val sender = ClientKeypair.generate()
        val wrap = try {
            Nip17.giftWrap(report, RECIPIENT_HEX, sender)
        } finally {
            sender.privkey.fill(0)
        }
        val relays = runCatching { RelayQuery.fetchDmRelays(RECIPIENT_HEX) }.getOrDefault(emptyList())
        return RelayQuery.publish((relays + RelayList.FALLBACK).distinct(), wrap).remoteOk
    }

    internal fun format(
        threadName: String,
        throwable: Throwable,
        now: Instant = Instant.now(),
        articleUrl: String? = this.articleUrl,
        breadcrumbs: List<String> = synchronized(this.breadcrumbs) { this.breadcrumbs.toList() },
        appVersion: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.GIT_COMMIT}",
        android: String = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
        device: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    ): String = buildString {
        appendLine("Boris crash report")
        appendLine()
        appendLine("App: $appVersion")
        appendLine("Android: $android")
        appendLine("Device: $device")
        appendLine("Time: $now")
        appendLine("Thread: $threadName")
        articleUrl?.let { appendLine("Article: $it") }
        if (breadcrumbs.isNotEmpty()) appendLine("Recent screens: ${breadcrumbs.joinToString(" > ")}")
        appendLine()
        appendLine("Stack trace:")
        append(stackTrace(throwable).take(MAX_STACK_CHARS))
    }

    private fun stackTrace(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString().trimEnd()
    }
}
