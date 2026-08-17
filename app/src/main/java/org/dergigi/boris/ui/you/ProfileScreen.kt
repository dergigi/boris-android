package org.dergigi.boris.ui.you

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Smartphone
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.R
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.ui.TopBarMenuItem
import org.dergigi.boris.ui.TopBarMoreMenu
import org.dergigi.boris.ui.openExternalUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    npub: String,
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit = { url, _, _ ->
        onOpenArticle(url)
    },
    viewModel: YouViewModel = viewModel(),
) {
    val context = LocalContext.current
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
                actions = {
                    TopBarMoreMenu(
                        items = listOf(
                            TopBarMenuItem(
                                label = stringResource(R.string.highlight_menu_open_njump),
                                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                                onClick = {
                                    openExternalUri(context, NostrLink.gatewayUrl(npub))
                                },
                            ),
                            TopBarMenuItem(
                                label = stringResource(R.string.highlight_menu_open_native),
                                icon = Icons.Outlined.Smartphone,
                                onClick = {
                                    openExternalUri(context, "nostr:$npub")
                                },
                            ),
                        ),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        YouHighlights(
            npub = npub,
            profile = profile,
            onOpenArticle = onOpenArticle,
            onOpenHighlight = onOpenHighlight,
            modifier = Modifier.padding(innerPadding),
            viewModel = viewModel,
        )
    }
}
