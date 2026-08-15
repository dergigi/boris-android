package org.dergigi.boris.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.BuildConfig
import org.dergigi.boris.R
import org.dergigi.boris.ui.auth.AuthUiState
import org.dergigi.boris.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val settingsMessage by settingsViewModel.message.collectAsStateWithLifecycle()
    val signIntent by settingsViewModel.signIntent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        settingsViewModel.onSignerResult(result.resultCode, result.data)
    }

    LaunchedEffect(signIntent) {
        val intent = signIntent ?: return@LaunchedEffect
        settingsViewModel.consumeSignIntent()
        settingsLauncher.launch(intent)
    }
    LaunchedEffect(settingsMessage) {
        val text = settingsMessage ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        settingsViewModel.consumeMessage()
    }
    LaunchedEffect(authState) {
        if (authState !is AuthUiState.LoggedIn) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                ThemeSection(
                    settings = settings,
                    onUpdate = { next -> settingsViewModel.update { next } },
                )
                ReadingSection(
                    settings = settings,
                    darkTheme = settings.isDark(isSystemInDarkTheme()),
                    onUpdate = { next -> settingsViewModel.update { next } },
                )
                HighlightsSection(
                    settings = settings,
                    onUpdate = { next -> settingsViewModel.update { next } },
                )
                ReadingPreview(
                    settings = settings,
                    darkTheme = settings.isDark(isSystemInDarkTheme()),
                )
                FeedSettingsSection(
                    settings = settings,
                    onUpdate = { next -> settingsViewModel.update { next } },
                )
                AirplaneModeSection(
                    settings = settings,
                    onUpdate = { next -> settingsViewModel.update { next } },
                    onOpenArticle = onOpenArticle,
                )
                ScrollBehaviourSection(
                    settings = settings,
                    onUpdate = { next -> settingsViewModel.update { next } },
                )
                SettingsVersionFooter()
            }
        }
    }
}

private const val GITHUB_REPO = "https://github.com/dergigi/boris-android"

@Composable
private fun SettingsVersionFooter() {
    val uriHandler = LocalUriHandler.current
    val version = BuildConfig.VERSION_NAME
    val commit = BuildConfig.GIT_COMMIT
    val shortCommit = if (commit.length > 7) commit.take(7) else commit
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_version, version),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable {
                uriHandler.openUri("$GITHUB_REPO/releases/tag/v$version")
            },
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = shortCommit,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable(enabled = commit != "unknown") {
                uriHandler.openUri("$GITHUB_REPO/commit/$commit")
            },
        )
    }
}
