package org.dergigi.boris.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.ui.HighlightCard
import org.dergigi.boris.ui.HighlightCardMenu
import org.dergigi.boris.ui.browser.InAppBrowser
import org.dergigi.boris.ui.theme.rememberDisplayLook

@Composable
internal fun ReaderLoadingPane(
    state: ReaderUiState.Loading,
    modifier: Modifier = Modifier,
) {
    val hasPreview = !state.title.isNullOrBlank() || !state.imageUrl.isNullOrBlank()
    if (!hasPreview) {
        Box(
            modifier = Modifier.fillMaxSize().then(modifier),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(modifier)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val coverUrl = state.imageUrl?.takeIf { it.isNotBlank() }
            if (coverUrl != null) {
                ArticleHero(
                    imageUrl = coverUrl,
                    title = state.title,
                    summary = null,
                    onClick = {},
                )
            } else if (!state.title.isNullOrBlank()) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 12.dp),
                )
            }
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 32.dp, bottom = 48.dp),
            )
        }
    }
}

@Composable
internal fun ReaderErrorPane(
    state: ReaderUiState.Error,
    onRetry: () -> Unit,
    onOpenOriginal: () -> Unit,
    onOpenWayback: () -> Unit,
    onOpenArchivePh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var detailsOpen by remember(state.detail) { mutableStateOf(false) }
    val coverUrl = state.imageUrl?.takeIf { it.isNotBlank() }
    val hasPreview = coverUrl != null || !state.title.isNullOrBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .then(if (hasPreview) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        verticalArrangement = if (hasPreview) Arrangement.Top else Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (coverUrl != null) {
            ArticleHero(
                imageUrl = coverUrl,
                title = state.title,
                summary = null,
                onClick = {},
            )
        } else if (!state.title.isNullOrBlank()) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
            )
        }
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = if (hasPreview) 24.dp else 0.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            )
            if (!state.detail.isNullOrBlank()) {
                TextButton(onClick = { detailsOpen = !detailsOpen }) {
                    Text(
                        stringResource(
                            if (detailsOpen) {
                                R.string.reader_error_hide_details
                            } else {
                                R.string.reader_error_details
                            },
                        ),
                    )
                }
                if (detailsOpen) {
                    Text(
                        text = state.detail,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textAlign = TextAlign.Center,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("Try again")
            }
            if (state.url.isNotBlank()) {
                OutlinedButton(
                    onClick = onOpenOriginal,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reader_open_in_browser))
                }
                if (InAppBrowser.waybackUrl(state.url) != null) {
                    TextButton(onClick = onOpenWayback) {
                        Text(stringResource(R.string.reader_open_wayback))
                    }
                    TextButton(onClick = onOpenArchivePh) {
                        Text(stringResource(R.string.reader_open_archive_ph))
                    }
                }
            }
        }
    }
}

@Composable
internal fun OpenedHighlightPane(
    state: ReaderUiState.Highlight,
    author: Profile?,
    settings: UserSettings,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    onOpenProfile: (String) -> Unit,
    canDeleteHighlight: (String?) -> Boolean,
    onDeleteHighlight: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlight = state.highlight
    val context = LocalContext.current
    val sessionHex = remember { SessionStore.load(context)?.pubkeyHex }
    val mine = highlight.authorPubkey.equals(sessionHex, ignoreCase = true)
    val look = rememberDisplayLook(settings)
    val color = if (mine) look.mine else look.nostrverse
    val authorName = Profile.displayName(highlight.authorPubkey, author)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HighlightCard(
            quote = highlight.quote,
            context = highlight.context,
            color = color,
            createdAt = highlight.createdAt,
            authorName = authorName,
            host = highlight.host,
            url = highlight.articleUrl,
            authorPicture = author?.picture,
            onClick = {
                onOpenHighlight(highlight.articleUrl, highlight.id, highlight.quote)
            },
            menu = HighlightCardMenu(
                highlightId = highlight.id,
                authorHex = highlight.authorPubkey,
                onGoToQuote = {
                    onOpenHighlight(highlight.articleUrl, highlight.id, highlight.quote)
                },
                onViewProfile = { onOpenProfile(highlight.authorPubkey) },
                onDelete = if (canDeleteHighlight(highlight.authorPubkey)) {
                    { onDeleteHighlight(highlight.id) }
                } else {
                    null
                },
            ),
            modifier = Modifier.widthIn(max = 720.dp),
        )
    }
}
