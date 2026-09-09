package org.dergigi.boris.ui

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.CrashReporter
import org.dergigi.boris.data.SettingsSync

/** Offers a pending crash report once per launch; see [CrashReporter]. */
@Composable
fun CrashReportPrompt() {
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val ready by SettingsSync.ready.collectAsStateWithLifecycle()
    var report by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        report = withContext(Dispatchers.IO) { CrashReporter.pending() }
    }
    if (!ready) return
    val pending = report ?: return
    if (!settings.offerCrashReports) {
        LaunchedEffect(pending) { withContext(Dispatchers.IO) { CrashReporter.clear() } }
        return
    }
    CrashReportDialog(
        report = pending,
        onDone = {
            CrashReporter.clear()
            report = null
        },
    )
}

@Composable
private fun CrashReportDialog(
    report: String,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!sending) onDone() },
        title = { Text(stringResource(R.string.crash_report_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.crash_report_body))
                Text(
                    text = report,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                )
                if (failed) {
                    Text(
                        text = stringResource(R.string.crash_report_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sending,
                onClick = {
                    sending = true
                    failed = false
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { runCatching { CrashReporter.send(report) }.getOrDefault(false) }
                        sending = false
                        if (ok) {
                            Toast.makeText(context, R.string.crash_report_sent, Toast.LENGTH_SHORT).show()
                            onDone()
                        } else {
                            failed = true
                        }
                    }
                },
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp).padding(2.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.crash_report_send))
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(report))
                        Toast.makeText(context, R.string.action_copied, Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text(stringResource(R.string.crash_report_copy))
                }
                TextButton(enabled = !sending, onClick = onDone) {
                    Text(stringResource(R.string.crash_report_dismiss))
                }
            }
        },
    )
}
