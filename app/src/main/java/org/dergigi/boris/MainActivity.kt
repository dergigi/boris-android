package org.dergigi.boris

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.dergigi.boris.data.CacheLimit
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.OfflineDownloader
import org.dergigi.boris.data.OfflineStore
import org.dergigi.boris.data.OgPreviewCache
import org.dergigi.boris.data.ReaderRepository
import org.dergigi.boris.data.ReadingPositionStore
import org.dergigi.boris.data.ReadingPositionSync
import org.dergigi.boris.data.RssRepository
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.nostr.EventCache
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
        ReadingPositionStore.init(File(filesDir, "reading_positions.json"))
        ReadingPositionSync.refreshAsync(applicationContext)
        ReaderRepository.init(File(filesDir, "reader_http_cache"), CacheLimit.bytes(this))
        RssRepository.init(File(filesDir, "rss_http_cache"))
        OfflineOutbox.init(File(filesDir, "offline_outbox.json"))
        OgPreviewCache.init(File(filesDir, "og_preview_cache.json"))
        applyIntent(intent)
        enableEdgeToEdge()
        setContent {
            val homeViewModel: HomeViewModel = viewModel()
            // Fullscreen quote while a cold start has nothing local and home is
            // still talking to relays. Warm starts (cached highlights) skip it.
            var coldStartLoading by remember { mutableStateOf(false) }
            var appReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) { EventCache.awaitReady() }
                val deepLink = !incomingUrl.isNullOrBlank() || !incomingBunker.isNullOrBlank()
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
                coldStartLoading = false
                appReady = true
            }
            BorisTheme {
                when {
                    coldStartLoading -> LoadingScreen()
                    appReady -> BorisApp(
                        incomingUrl = incomingUrl,
                        incomingBunker = incomingBunker,
                        homeViewModel = homeViewModel,
                    )
                    // System splash still covers this while we decide.
                    else -> LoadingScreen()
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
        val bunker = bunkerFrom(intent)
        if (bunker != null) {
            incomingBunker = bunker
            incomingUrl = null
        } else {
            incomingUrl = urlFrom(intent)
            incomingBunker = null
        }
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
        return when (intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_PROCESS_TEXT -> UrlExtractor.extract(textFrom(intent))
            Intent.ACTION_VIEW -> {
                val raw = intent.dataString ?: textFrom(intent)
                NostrLink.parse(raw)?.uri ?: UrlExtractor.extract(raw) ?: raw?.trim()?.ifEmpty { null }
            }
            else -> UrlExtractor.extract(textFrom(intent))
        }
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
        private const val COLD_START_SPLASH_MS = 5_000L
    }
}
