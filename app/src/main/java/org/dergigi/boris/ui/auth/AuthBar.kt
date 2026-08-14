package org.dergigi.boris.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R

const val AMBER_ZAPSTORE_URL = "https://zapstore.dev/apps/com.greenart7c3.nostrsigner"
const val AMBER_FDROID_URL = "https://f-droid.org/packages/com.greenart7c3.nostrsigner/"
const val AMBER_GITHUB_URL = "https://github.com/greenart7c3/Amber/releases"

@Composable
fun AuthBar(
    state: AuthUiState,
    message: String?,
    onConnect: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            AuthUiState.LoggedOut -> {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.auth_connect))
                }
            }
            AuthUiState.MissingSigner -> {
                Text(
                    text = stringResource(R.string.auth_missing_amber),
                    style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                InstallLink(stringResource(R.string.auth_install_zapstore), AMBER_ZAPSTORE_URL)
                InstallLink(stringResource(R.string.auth_install_fdroid), AMBER_FDROID_URL)
                InstallLink(stringResource(R.string.auth_install_github), AMBER_GITHUB_URL)
            }
            is AuthUiState.LoggedIn -> {
                SelectionContainer {
                    Text(
                        text = state.npub,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onSignOut) {
                    Text(stringResource(R.string.auth_sign_out))
                }
            }
        }
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun InstallLink(label: String, url: String) {
    val context = LocalContext.current
    TextButton(
        onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
    }
}
