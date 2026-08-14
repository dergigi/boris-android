package com.readwithboris

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.readwithboris.data.UrlExtractor
import com.readwithboris.ui.BorisApp
import com.readwithboris.ui.theme.BorisTheme

class MainActivity : ComponentActivity() {
    private var incomingUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingUrl = urlFrom(intent)
        enableEdgeToEdge()
        setContent {
            BorisTheme {
                BorisApp(incomingUrl = incomingUrl)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUrl = urlFrom(intent)
    }

    private fun urlFrom(intent: Intent): String? {
        return when (intent.action) {
            Intent.ACTION_SEND -> UrlExtractor.extract(intent.getStringExtra(Intent.EXTRA_TEXT))
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
    }
}
