package org.dergigi.boris

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.ui.BorisApp
import org.dergigi.boris.ui.theme.BorisTheme

class MainActivity : ComponentActivity() {
    private var incomingUrl by mutableStateOf<String?>(null)
    private var incomingBunker by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyIntent(intent)
        enableEdgeToEdge()
        setContent {
            BorisTheme {
                BorisApp(
                    incomingUrl = incomingUrl,
                    incomingBunker = incomingBunker,
                )
            }
        }
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
}
