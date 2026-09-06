package org.dergigi.boris.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.DEFAULT_ZAP_PRESETS
import org.dergigi.boris.data.NwcConnection
import org.dergigi.boris.data.NwcStore
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.nostr.NwcClient
import org.dergigi.boris.nostr.NwcResult
import org.dergigi.boris.nostr.NwcUri
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.ui.reader.formatSats
import java.util.Locale

@Composable
fun WalletSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val connection by NwcStore.connection.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { NwcStore.load(context) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val current = connection
        if (current == null) ConnectWallet() else ConnectedWallet(current)
        ZapBehavior(settings, onUpdate)
        DefaultAmount(settings, onUpdate)
        DefaultMessage(settings, onUpdate)
    }
}

@Composable
private fun ConnectWallet() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    WalletPanel {
        Text(
            text = stringResource(R.string.settings_wallet_connect_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.settings_wallet_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                error = null
            },
            placeholder = { Text(stringResource(R.string.settings_wallet_uri_hint)) },
            isError = error != null,
            supportingText = error?.let { { Text(stringResource(it)) } },
            minLines = 2,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                val uri = NwcUri.parse(input)
                if (uri == null) {
                    error = R.string.settings_wallet_invalid
                    return@Button
                }
                busy = true
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        val secret = uri.secretBytes()
                        try {
                            NwcClient(uri.walletPubkey, uri.relays, secret).getInfo() is NwcResult.Ok
                        } finally {
                            secret.fill(0)
                        }
                    }
                    if (ok) {
                        NwcStore.save(context, uri)
                        input = ""
                    } else {
                        error = R.string.settings_wallet_unreachable
                    }
                    busy = false
                }
            },
            enabled = !busy && input.isNotBlank(),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.settings_wallet_connect))
            }
        }
        Text(
            text = stringResource(R.string.settings_wallet_secret_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConnectedWallet(connection: NwcConnection) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var balance by remember(connection) { mutableStateOf<Long?>(null) }
    var loading by remember(connection) { mutableStateOf(false) }
    var checked by remember(connection) { mutableStateOf(false) }

    fun refresh() {
        if (loading) return
        loading = true
        scope.launch {
            balance = withContext(Dispatchers.IO) {
                val secret = NwcStore.secret(context) ?: return@withContext null
                try {
                    val result = NwcClient(connection.walletPubkey, connection.relays, secret).getBalance()
                    (result as? NwcResult.Ok)?.result?.optLong("balance", -1L)?.takeIf { it >= 0 }?.div(1000)
                } finally {
                    secret.fill(0)
                }
            }
            checked = true
            loading = false
        }
    }
    LaunchedEffect(connection) { refresh() }

    WalletPanel {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.settings_wallet_connected),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Text(
            text = stringResource(R.string.settings_wallet_connected_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Detail(stringResource(R.string.settings_wallet_pubkey, Profile.shortNpub(connection.walletPubkey)))
            Detail(stringResource(R.string.settings_wallet_relay, connection.relays.first()))
            connection.lud16?.let { Detail(stringResource(R.string.settings_wallet_address, it)) }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sats = balance
            Text(
                text = when {
                    sats != null -> stringResource(R.string.settings_wallet_balance, formatSats(sats))
                    checked -> stringResource(R.string.settings_wallet_balance_unknown)
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = ::refresh) { Text(stringResource(R.string.settings_wallet_refresh)) }
            }
        }
        OutlinedButton(onClick = { NwcStore.clear(context) }) {
            Text(stringResource(R.string.settings_wallet_disconnect))
        }
        Text(
            text = stringResource(R.string.settings_wallet_secret_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Detail(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WalletPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefaultAmount(settings: UserSettings, onUpdate: (UserSettings) -> Unit) {
    val current = settings.defaultZapAmount.toLong()
    var editingPresets by rememberSaveable { mutableStateOf(false) }
    if (editingPresets) {
        ZapPresetEditorDialog(
            settings = settings,
            onUpdate = onUpdate,
            onDismiss = { editingPresets = false },
        )
    }
    WalletPanel {
        Text(
            text = stringResource(R.string.settings_wallet_default_amount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.settings_wallet_default_amount_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            settings.zapPresets.forEach { preset ->
                FilterChip(
                    selected = current == preset,
                    onClick = { onUpdate(settings.withInt("defaultZapAmount", preset.toInt())) },
                    label = { Text(formatSats(preset)) },
                )
            }
        }
        OutlinedButton(onClick = { editingPresets = true }) {
            Text(stringResource(R.string.settings_wallet_presets_edit))
        }
    }
}

@Composable
private fun ZapBehavior(settings: UserSettings, onUpdate: (UserSettings) -> Unit) {
    WalletPanel {
        Text(
            text = stringResource(R.string.settings_wallet_zap_behavior),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        SettingCheckbox(
            label = stringResource(R.string.settings_wallet_one_tap_zaps),
            checked = settings.oneTapZaps,
            onCheckedChange = { onUpdate(settings.withBoolean("oneTapZaps", it)) },
        )
        Text(
            text = stringResource(R.string.settings_wallet_one_tap_zaps_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ZapPresetEditorDialog(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable(settings.zapPresets) {
        mutableStateOf(settings.zapPresets.joinToString(", "))
    }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_wallet_presets_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_wallet_presets_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.settings_wallet_presets_hint)) },
                    supportingText = error?.let { { Text(stringResource(it)) } },
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val presets = parseZapPresetInput(input)
                    val validationError = validateZapPresets(presets)
                    if (validationError != null) {
                        error = validationError
                        return@TextButton
                    }
                    onUpdate(settings.withWalletZapPresets(presets))
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.settings_wallet_presets_save))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        input = DEFAULT_ZAP_PRESETS.joinToString(", ")
                        error = null
                    },
                ) {
                    Text(stringResource(R.string.settings_wallet_presets_reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        },
    )
}

@Composable
private fun DefaultMessage(settings: UserSettings, onUpdate: (UserSettings) -> Unit) {
    var message by rememberSaveable(settings.defaultZapMessage) {
        mutableStateOf(settings.defaultZapMessage)
    }
    WalletPanel {
        Text(
            text = stringResource(R.string.settings_wallet_default_message),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.settings_wallet_default_message_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = message,
            onValueChange = {
                message = it
                onUpdate(settings.withString("defaultZapMessage", it))
            },
            label = { Text(stringResource(R.string.settings_wallet_default_message_hint)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun UserSettings.withWalletZapPresets(values: List<Long>): UserSettings {
    val next = withZapPresets(values)
    if (next.defaultZapAmount.toLong() in next.zapPresets) return next
    return next.withInt("defaultZapAmount", next.zapPresets.first().toInt())
}

internal fun parseZapPresetInput(input: String): List<Long> =
    input
        .split(',', ' ', '\n', '\t')
        .mapNotNull { parseZapPresetToken(it.trim()) }
        .distinct()

internal fun validateZapPresets(presets: List<Long>): Int? = when {
    presets.isEmpty() -> R.string.settings_wallet_presets_invalid
    presets.size > MAX_WALLET_ZAP_PRESETS -> R.string.settings_wallet_presets_too_many
    else -> null
}

private fun parseZapPresetToken(token: String): Long? {
    if (token.isBlank()) return null
    val normalized = token.lowercase(Locale.US).replace("_", "")
    val multiplier = if (normalized.endsWith("k")) 1000L else 1L
    val digits = if (multiplier == 1000L) normalized.dropLast(1) else normalized
    val value = digits.toLongOrNull() ?: return null
    if (value !in 1..(MAX_WALLET_ZAP_PRESET / multiplier)) return null
    return value * multiplier
}

internal const val MAX_WALLET_ZAP_PRESETS = 8
private const val MAX_WALLET_ZAP_PRESET = 999_999_999L
