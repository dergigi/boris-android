package org.dergigi.boris.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.WebArchiveReceipt
import org.dergigi.boris.nostr.WebArchives
import java.net.URI
import java.text.DateFormat
import java.util.Date

@Composable
internal fun rememberWebArchives(url: String?, loggedIn: Boolean): List<WebArchiveReceipt> {
    val context = LocalContext.current
    val pubkey = remember(loggedIn) { SessionStore.load(context)?.pubkeyHex }
    // Key the state as well as the effect so old results never appear on a new article.
    var receipts by remember(url, pubkey) { mutableStateOf(emptyList<WebArchiveReceipt>()) }
    LaunchedEffect(url, pubkey) {
        if (url == null) return@LaunchedEffect
        receipts = withContext(Dispatchers.IO) {
            try {
                val relays = pubkey?.let { RelayQuery.fetchRelayList(it).read } ?: RelayList.FALLBACK
                WebArchives.lookup(url, relays)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
    return receipts
}

@Composable
internal fun WebArchivePicker(
    receipts: List<WebArchiveReceipt>,
    onDismiss: () -> Unit,
    onOpenBrowser: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_archived_versions)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.reader_archive_mirrors_hint))
                receipts.forEach { receipt ->
                    Text(
                        stringResource(
                            if (receipt.naan) R.string.reader_open_naan_archive
                            else R.string.reader_open_archived_version,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    receipt.archivedAt?.let { timestamp ->
                        Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp * 1000)))
                    }
                    receipt.urls.forEachIndexed { index, url ->
                        TextButton(onClick = {
                            onDismiss()
                            onOpenBrowser(url)
                        }) {
                            Text(stringResource(
                                R.string.reader_archive_copy,
                                index + 1,
                                URI(url).host,
                            ))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
