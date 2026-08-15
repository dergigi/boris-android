package org.dergigi.boris.ui.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.ui.auth.AuthBar
import org.dergigi.boris.ui.auth.AuthUiState
import org.dergigi.boris.ui.auth.AuthViewModel
import org.dergigi.boris.ui.auth.NstartFooter
import org.dergigi.boris.ui.you.YouHighlights
import org.dergigi.boris.ui.you.YouLoggedOut

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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    when (val current = authState) {
        is AuthUiState.LoggedIn -> {
            YouHighlights(
                npub = current.npub,
                profile = profile,
                onOpenSettings = onOpenSettings,
                onOpenArticle = onOpenArticle,
                modifier = modifier,
            )
        }
        else -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
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
                        onSignOut = viewModel::signOut,
                    )
                    NstartFooter()
                }
            }
        }
    }
}
