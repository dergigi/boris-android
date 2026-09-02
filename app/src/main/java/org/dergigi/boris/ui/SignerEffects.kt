package org.dergigi.boris.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Amber activity-result launcher plus toast for a signer [signIntent] / [message] pair.
 * Returns a function that launches an Intent on the same launcher (for APIs that
 * still return the Amber intent from request()).
 */
@Composable
fun SignerEffects(
    signIntent: Intent? = null,
    message: String? = null,
    onConsumeSignIntent: () -> Unit = {},
    onConsumeMessage: () -> Unit = {},
    onSignerResult: (resultCode: Int, data: Intent?) -> Unit,
    onMessage: ((String) -> Unit)? = null,
): (Intent) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        onSignerResult(result.resultCode, result.data)
    }
    LaunchedEffect(signIntent) {
        val intent = signIntent ?: return@LaunchedEffect
        onConsumeSignIntent()
        launcher.launch(intent)
    }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        onConsumeMessage()
        if (onMessage != null) {
            onMessage(text)
        } else {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
    return remember(launcher) { { intent -> launcher.launch(intent) } }
}
