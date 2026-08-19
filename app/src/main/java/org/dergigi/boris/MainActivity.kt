package org.dergigi.boris

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.dergigi.boris.data.CacheLimit
import org.dergigi.boris.data.OfflineDownloader
import org.dergigi.boris.data.OfflineStore
import org.dergigi.boris.data.OgPreviewCache
import org.dergigi.boris.data.ReaderRepository
import org.dergigi.boris.data.ReadingPositionStore
import org.dergigi.boris.data.ReadingPositionSync
import org.dergigi.boris.data.RssRepository
import org.dergigi.boris.data.IncomingShare
import org.dergigi.boris.data.IncomingShares
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.HintedRelays
import org.dergigi.boris.nostr.OfflineOutbox
import org.dergigi.boris.nostr.OfflineSync
import org.dergigi.boris.nostr.RelayHealth
import org.dergigi.boris.nostr.RelayPool
import org.dergigi.boris.ui.BorisApp
import org.dergigi.boris.ui.home.HomeHighlightsState
import org.dergigi.boris.ui.home.HomeViewModel
import org.dergigi.boris.ui.reader.VolumeKeys
import org.dergigi.boris.ui.shell.LoadingScreen
import org.dergigi.boris.ui.theme.BorisTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private var incomingUrl by mutableStateOf<String?>(null)
    private var incomingBunker by mutableStateOf<String?>(null)
    private var incomingHighlight by mutableStateOf<IncomingShare?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        OfflineSync.bind(this)
        EventCache.init(File(filesDir, "event_cache"))
        // Hold the system circle until Compose is ready to show either the app
        // or the fullscreen cold-start loading screen.
        var composeDrawn = false
        splash.setKeepOnScreenCondition { !composeDrawn }
        OfflineStore.init(File(filesDir, "offline_downloads.json"))
        RelayHealth.init(File(filesDir, "relay_health.json"))
        HintedRelays.init(File(filesDir, "hinted_relays.json"))
        ReadingPositionStore.init(File(filesDir, "reading_positions.json"))
        ReadingPositionSync.refreshAsync(applicationContext)
        ReaderRepository.init(File(filesDir, "reader_http_cache"), CacheLimit.bytes(this))
        RssRepository.init(File(filesDir, "rss_http_cache"))
        OfflineOutbox.init(File(filesDir, "offline_outbox.json"))
        OgPreviewCache.init(File(filesDir, "og_preview_cache.json"))
        // Only seed deep links on a fresh start. Config change keeps the same
        // share/VIEW Intent; re-applying it would re-open the reader after Back.
        if (savedInstanceState == null) {
            applyIntent(intent)
        }
        enableEdgeToEdge()
        setContent {
            val homeViewModel: HomeViewModel = viewModel()
            // Fullscreen quote while a cold start has nothing local and home is
            // still talking to relays. Warm starts (cached highlights) skip it.
            var coldStartLoading by remember { mutableStateOf(false) }
            var appReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) { EventCache.awaitReady() }
                val deepLink = !incomingUrl.isNullOrBlank() ||
                    !incomingBunker.isNullOrBlank() ||
                    incomingHighlight != null
                val hasLocal = withContext(Dispatchers.IO) { EventCache.hasHighlights() }
                if (hasLocal || deepLink) {
                    composeDrawn = true
                    appReady = true
                    return@LaunchedEffect
                }
                coldStartLoading = true
                composeDrawn = true
                homeViewModel.refresh()
                // Cap the quote screen; home keeps fetching under the app UI.
                withTimeoutOrNull(COLD_START_SPLASH_MS) {
                    homeViewModel.highlights.first { it !is HomeHighlightsState.Loading }
                }
                // App underneath first, then fade the quote out.
                appReady = true
                coldStartLoading = false
            }
            BorisTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (appReady) {
                        BorisApp(
                            incomingUrl = incomingUrl,
                            incomingBunker = incomingBunker,
                            incomingHighlight = incomingHighlight,
                            onIncomingUrlConsumed = { markIncomingIntentConsumed() },
                            onIncomingBunkerConsumed = { markIncomingIntentConsumed() },
                            onIncomingHighlightConsumed = { markIncomingIntentConsumed() },
                            homeViewModel = homeViewModel,
                        )
                    }
                    AnimatedVisibility(
                        visible = coldStartLoading,
                        enter = EnterTransition.None,
                        exit = fadeOut(animationSpec = tween(durationMillis = COLD_START_FADE_MS)),
                    ) {
                        LoadingScreen()
                    }
                    // System splash still covers this while we decide.
                    if (!appReady && !coldStartLoading) {
                        LoadingScreen()
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeKeys.handle(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeKeys.handle(keyCode, event)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        OfflineSync.flush()
        OfflineDownloader.kickoff(applicationContext)
    }

    override fun onStop() {
        super.onStop()
        RelayPool.closeAll()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_INTENT_CONSUMED, false)) {
            incomingUrl = null
            incomingBunker = null
            incomingHighlight = null
            return
        }
        if (intent.action == Intent.ACTION_PROCESS_TEXT) {
            incomingBunker = null
            val share = IncomingShares.fromProcessText(
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString(),
                originatingUrl(intent),
            )
            if (!share.highlightQuote.isNullOrBlank()) {
                incomingHighlight = share
                incomingUrl = null
            } else {
                incomingHighlight = null
                incomingUrl = share.url
            }
            return
        }
        val bunker = bunkerFrom(intent)
        if (bunker != null) {
            incomingBunker = bunker
            incomingUrl = null
            incomingHighlight = null
        } else {
            incomingUrl = urlFrom(intent)
            incomingBunker = null
            incomingHighlight = null
        }
    }

    private fun originatingUrl(intent: Intent): String? {
        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        return IncomingShares.firstPageUrl(
            processTextUrlCandidates(intent, selectedText),
        )
    }

    private fun processTextUrlCandidates(intent: Intent, selectedText: String?): List<String?> = buildList {
        add(intent.getParcelableExtra<Uri>(Intent.EXTRA_REFERRER)?.toString())
        add(intent.getStringExtra(Intent.EXTRA_REFERRER_NAME))
        add(intent.dataString)
        val clip = intent.clipData
        if (clip != null) {
            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i)
                addIfSourceCandidate(item.uri?.toString(), selectedText)
                addIfSourceCandidate(item.text?.toString(), selectedText)
                addIfSourceCandidate(item.htmlText, selectedText)
            }
        }
        val extras = intent.extras ?: return@buildList
        for (key in extras.keySet()) {
            if (key in PROCESS_TEXT_SOURCE_IGNORE_KEYS) continue
            addExtraUrlCandidate(extras.get(key), selectedText)
        }
    }

    private fun MutableList<String?>.addExtraUrlCandidate(value: Any?, selectedText: String?) {
        when (value) {
            null -> Unit
            is Uri -> addIfSourceCandidate(value.toString(), selectedText)
            is CharSequence -> addIfSourceCandidate(value.toString(), selectedText)
            is Array<*> -> value.forEach { addExtraUrlCandidate(it, selectedText) }
            is Iterable<*> -> value.forEach { addExtraUrlCandidate(it, selectedText) }
            is Bundle -> value.keySet().forEach { addExtraUrlCandidate(value.get(it), selectedText) }
        }
    }

    private fun MutableList<String?>.addIfSourceCandidate(value: String?, selectedText: String?) {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) return
        if (selectedText != null && trimmed == selectedText.trim()) return
        add(trimmed)
    }

    /** Marks the current Activity intent so recreate/share replay cannot re-open it. */
    private fun markIncomingIntentConsumed() {
        intent.putExtra(EXTRA_INTENT_CONSUMED, true)
        incomingUrl = null
        incomingBunker = null
        incomingHighlight = null
    }

    private fun bunkerFrom(intent: Intent): String? {
        val raw = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
        val trimmed = raw?.trim().orEmpty()
        return if (trimmed.startsWith("bunker:", ignoreCase = true)) trimmed else null
    }

    private fun urlFrom(intent: Intent): String? {
        val raw = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString ?: textFrom(intent)
            else -> textFrom(intent)
        }
        return UrlExtractor.extract(raw)
    }

    private fun textFrom(intent: Intent): String? {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { return it }
        intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)?.let { return it }
        intent.dataString?.let { return it }
        val clip = intent.clipData ?: return null
        if (clip.itemCount == 0) return null
        val item = clip.getItemAt(0)
        return item.text?.toString() ?: item.uri?.toString()
    }

    companion object {
        const val EXTRA_INTENT_CONSUMED = "org.dergigi.boris.INTENT_CONSUMED"
        private const val COLD_START_SPLASH_MS = 5_000L
        private const val COLD_START_FADE_MS = 700
        private val PROCESS_TEXT_SOURCE_IGNORE_KEYS = setOf(
            Intent.EXTRA_PROCESS_TEXT,
            Intent.EXTRA_PROCESS_TEXT_READONLY,
        )
    }
}
