package org.dergigi.boris.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.DEFAULT_ZAP_PRESETS
import org.dergigi.boris.nostr.ZapRecipients
import org.dergigi.boris.ui.ZapProgress

/** Amount, comment, and recipient breakdown; then progress and the outcome. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZapDialog(
    progress: ZapProgress,
    defaultSats: Long,
    defaultComment: String = "",
    presets: List<Long> = DEFAULT_ZAP_PRESETS,
    onConfirm: (sats: Long, comment: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf(defaultSats.toString()) }
    var comment by rememberSaveable(defaultComment) { mutableStateOf(defaultComment) }
    val sats = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val paying = progress is ZapProgress.Paying
    AlertDialog(
        onDismissRequest = { if (!paying) onDismiss() },
        title = { Text(stringResource(R.string.zap_title)) },
        text = {
            when (progress) {
                ZapProgress.Resolving -> Progress(stringResource(R.string.zap_resolving))
                is ZapProgress.Paying -> Progress(
                    stringResource(R.string.zap_paying, progress.index + 1, progress.total),
                )
                is ZapProgress.Failed -> Text(progress.message)
                is ZapProgress.Done -> DoneText(progress)
                is ZapProgress.Ready -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = sats == preset,
                                onClick = { amountText = preset.toString() },
                                label = { Text(formatSats(preset)) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() }.take(9) },
                        label = { Text(stringResource(R.string.zap_amount_sats)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(stringResource(R.string.zap_comment_hint)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    RecipientList(progress, sats)
                }
            }
        },
        confirmButton = {
            when (progress) {
                is ZapProgress.Ready -> TextButton(
                    onClick = { onConfirm(sats, comment) },
                    enabled = sats > 0,
                ) {
                    Text(stringResource(R.string.zap_confirm, formatSats(sats)))
                }
                is ZapProgress.Done, is ZapProgress.Failed -> TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
                else -> {}
            }
        },
        dismissButton = {
            if (progress is ZapProgress.Ready || progress is ZapProgress.Resolving) {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            }
        },
    )
}

/** Shown when there is no wallet yet; sends the reader to the Wallet settings. */
@Composable
fun ZapWalletHintDialog(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.zap_title)) },
        text = { Text(stringResource(R.string.zap_no_wallet)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.zap_open_wallet_settings)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun RecipientList(ready: ZapProgress.Ready, sats: Long) {
    val shares = ZapRecipients.shares(sats, ready.recipients.map { it.weight })
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ready.recipients.forEachIndexed { index, recipient ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = recipient.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatSats(shares[index]),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (ready.dropped > 0) {
            Text(
                text = pluralStringResource(R.plurals.zap_dropped_recipients, ready.dropped, ready.dropped),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DoneText(done: ZapProgress.Done) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (done.failed.isNotEmpty()) {
            Text(
                text = stringResource(R.string.zap_failed_for, done.failed.joinToString(", ")),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun Progress(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(label)
    }
}

internal fun formatSats(sats: Long): String = when {
    sats >= 1_000_000 && sats % 1_000_000 == 0L -> "${sats / 1_000_000}M"
    sats >= 1_000 && sats % 1_000 == 0L -> "${sats / 1_000}k"
    else -> sats.toString()
}
