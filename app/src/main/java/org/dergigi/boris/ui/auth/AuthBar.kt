package org.dergigi.boris.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.ui.theme.HighlightMine

const val AMBER_ZAPSTORE_URL = "https://zapstore.dev/apps/com.greenart7c3.nostrsigner"
const val AMBER_FDROID_URL = "https://f-droid.org/packages/com.greenart7c3.nostrsigner/"
const val AMBER_GITHUB_URL = "https://github.com/greenart7c3/Amber/releases"
const val NSTART_URL = "https://nstart.me/"

private val LoginShape = RoundedCornerShape(8.dp)

@Composable
fun AuthBar(
    state: AuthUiState,
    message: String?,
    bunkerUri: String,
    onBunkerUriChange: (String) -> Unit,
    onConnect: () -> Unit,
    onConnectBunker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connecting = state is AuthUiState.Connecting
    val prior = (state as? AuthUiState.Connecting)?.prior
    val chrome = prior ?: state
    var showBunker by rememberSaveable { mutableStateOf(bunkerUri.isNotBlank()) }
    var showMissingAmber by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(bunkerUri) {
        if (bunkerUri.isNotBlank()) showBunker = true
    }
    LaunchedEffect(chrome) {
        if (chrome !is AuthUiState.MissingSigner) showMissingAmber = false
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state !is AuthUiState.LoggedIn) {
                if (!showBunker) {
                    LoginButton(
                        label = stringResource(R.string.auth_connect),
                        icon = Icons.Filled.Key,
                        primary = true,
                        enabled = !connecting,
                        onClick = {
                            if (chrome is AuthUiState.MissingSigner) {
                                showMissingAmber = true
                            } else {
                                onConnect()
                            }
                        },
                    )
                    LoginButton(
                        label = stringResource(R.string.auth_bunker),
                        icon = Icons.Filled.Shield,
                        primary = false,
                        enabled = !connecting,
                        onClick = { showBunker = true },
                    )
                } else {
                    BunkerFields(
                        uri = bunkerUri,
                        onUriChange = onBunkerUriChange,
                        onConnect = onConnectBunker,
                        onCancel = {
                            showBunker = false
                            onBunkerUriChange("")
                        },
                        connecting = connecting,
                    )
                }
                if (showMissingAmber) {
                    MissingAmberCard()
                }
                if (!message.isNullOrBlank()) {
                    NoticeCard(message)
                }
        }
    }
}

@Composable
private fun LoginButton(
    label: String,
    icon: ImageVector,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    val iconTint = if (primary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = iconTint)
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = LoginShape,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            content()
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = LoginShape,
            contentPadding = contentPadding,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun BunkerFields(
    uri: String,
    onUriChange: (String) -> Unit,
    onConnect: () -> Unit,
    onCancel: () -> Unit,
    connecting: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LoginShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, LoginShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = uri,
            onValueChange = onUriChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.auth_bunker_hint)) },
            singleLine = true,
            enabled = !connecting,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (uri.isNotBlank()) onConnect() }),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onConnect,
                enabled = !connecting && uri.isNotBlank(),
                shape = LoginShape,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
                Text(
                    stringResource(
                        if (connecting) R.string.auth_bunker_connecting else R.string.auth_connect_bunker,
                    ),
                )
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !connecting,
                shape = LoginShape,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.auth_bunker_cancel))
            }
        }
    }
}

@Composable
private fun MissingAmberCard() {
    NoticeCard(stringResource(R.string.auth_missing_amber)) {
        InstallLink(stringResource(R.string.auth_install_zapstore), AMBER_ZAPSTORE_URL)
        InstallLink(stringResource(R.string.auth_install_fdroid), AMBER_FDROID_URL)
        InstallLink(stringResource(R.string.auth_install_github), AMBER_GITHUB_URL)
    }
}

@Composable
private fun NoticeCard(
    message: String,
    extra: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LoginShape)
            .border(1.dp, HighlightMine.copy(alpha = 0.35f), LoginShape)
            .background(HighlightMine.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Start),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        extra()
    }
}

@Composable
fun NstartFooter() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.auth_nstart_prefix),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
        Text(
            text = stringResource(R.string.auth_nstart_host),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(NSTART_URL)))
            },
        )
    }
}

@Composable
private fun InstallLink(label: String, url: String) {
    val context = LocalContext.current
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .padding(vertical = 4.dp),
    )
}
