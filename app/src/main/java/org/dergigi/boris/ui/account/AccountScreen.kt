package org.dergigi.boris.ui.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.R
import org.dergigi.boris.ui.auth.AuthBar
import org.dergigi.boris.ui.auth.AuthUiState
import org.dergigi.boris.ui.auth.AuthViewModel
import org.dergigi.boris.ui.auth.NstartFooter
import org.dergigi.boris.ui.you.YouHighlights
import org.dergigi.boris.ui.you.YouLoggedOut

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onOpenSettings: () -> Unit,
    onOpenArticle: (String) -> Unit,
    incomingBunker: String? = null,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(),
) {
    var bunkerUri by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(incomingBunker) {
        if (!incomingBunker.isNullOrBlank()) {
            bunkerUri = incomingBunker
        }
    }
    val authState by viewModel.state.collectAsStateWithLifecycle()
    val authMessage by viewModel.message.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onSignerResult(result.resultCode, result.data)
    }
    val loggedIn = authState is AuthUiState.LoggedIn
    var confirmSignOut by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.auth_sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.auth_sign_out_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSignOut = false
                        viewModel.signOut()
                    },
                ) {
                    Text(stringResource(R.string.auth_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) {
                    Text(stringResource(R.string.auth_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    if (loggedIn) {
                        IconButton(onClick = { confirmSignOut = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = stringResource(R.string.auth_sign_out),
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        when (val current = authState) {
            is AuthUiState.LoggedIn -> {
                YouHighlights(
                    npub = current.npub,
                    profile = profile,
                    onOpenArticle = onOpenArticle,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                        .imePadding(),
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                    ) {
                        YouLoggedOut()
                        AuthBar(
                            state = authState,
                            message = authMessage,
                            bunkerUri = bunkerUri,
                            onBunkerUriChange = { bunkerUri = it },
                            onConnect = {
                                viewModel.connectIntent()?.let(authLauncher::launch)
                            },
                            onConnectBunker = { viewModel.connectBunker(bunkerUri) },
                        )
                        NstartFooter()
                    }
                }
            }
        }
    }
}
