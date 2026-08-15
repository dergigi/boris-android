package org.dergigi.boris.ui.you

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    npub: String,
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: YouViewModel = viewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val title = Profile.displayName(
        runCatching { Nip19.npubDecode(npub) }.getOrDefault(""),
        profile,
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        YouHighlights(
            npub = npub,
            profile = profile,
            onOpenArticle = onOpenArticle,
            modifier = Modifier.padding(innerPadding),
            viewModel = viewModel,
        )
    }
}
