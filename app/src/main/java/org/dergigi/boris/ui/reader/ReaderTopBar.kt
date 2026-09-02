package org.dergigi.boris.ui.reader

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.dergigi.boris.R
import org.dergigi.boris.data.LibrarySave
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.ReadingPositionStore
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.tts.TtsPlayback
import org.dergigi.boris.tts.TtsText
import org.dergigi.boris.tts.requestTtsNotificationPermissionOnce
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.rememberDisplayLook

internal val ArchiveGreen = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderTopBar(
    modifier: Modifier = Modifier,
    state: ReaderUiState,
    articleUrl: String?,
    galleryUrls: List<String>,
    outlineItems: List<ArticleOutlineItem>,
    pane: ReaderPaneState,
    loggedIn: Boolean,
    highlights: List<PaintedHighlight>,
    highlightCount: Int,
    settings: UserSettings,
    inLibrary: Boolean,
    archived: Boolean,
    canSave: Boolean,
    author: Profile?,
    canOpenArchive: Boolean,
    scrollState: ScrollState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSave: (Boolean) -> Unit,
    onArchive: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onOpenGallery: (List<String>, Int) -> Unit,
    onOpenReaderSettings: () -> Unit,
    onShare: () -> Unit,
    onOpenOriginal: () -> Unit,
    onOpenNative: () -> Unit,
    onOpenWayback: () -> Unit,
    onOpenArchivePh: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    TopAppBar(
        modifier = modifier,
        title = {
            val title = (state as? ReaderUiState.Ready)?.content?.title
                ?: (state as? ReaderUiState.Loading)?.title
                ?: (state as? ReaderUiState.Highlight)?.let {
                    stringResource(R.string.you_tab_highlights)
                }
            val scrollTopLabel = stringResource(R.string.reader_scroll_to_top)
            Text(
                text = title.orEmpty(),
                modifier = if (title.isNullOrBlank()) {
                    Modifier
                } else {
                    Modifier.clickable(onClickLabel = scrollTopLabel) {
                        scope.launch {
                            scrollState.animateScrollTo(0)
                        }
                    }
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        navigationIcon = {
            Row {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                if (outlineItems.isNotEmpty()) {
                    IconButton(
                        onClick = { pane.openOutline() },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.FormatListBulleted,
                            contentDescription = stringResource(R.string.reader_outline),
                        )
                    }
                }
            }
        },
        actions = {
            if (loggedIn && state is ReaderUiState.Ready) {
                if (inLibrary && !archived) {
                    HighlightsBarButton(
                        highlights = highlights,
                        hasHighlights = highlightCount > 0,
                        settings = settings,
                        onClick = { pane.openHighlights() },
                    )
                } else {
                    SaveLibraryButton(
                        choosePrivacy = !LibrarySave.isWeb(state.content),
                        inLibrary = inLibrary,
                        archived = archived,
                        canSave = canSave,
                        onSave = onSave,
                    )
                }
            }
            if (state is ReaderUiState.Ready) {
                TtsListenButton(
                    content = state.content,
                    author = author,
                    onEmpty = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.tts_empty_heading) +
                                    "\n" + context.getString(R.string.tts_empty_body),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    },
                )
            }
            if (articleUrl != null) {
                val nativeUri = remember(articleUrl) {
                    NostrLink.parse(articleUrl)?.uri
                }
                ReaderOverflowMenu(
                    articleUrl = articleUrl,
                    state = state,
                    pane = pane,
                    loggedIn = loggedIn,
                    archived = archived,
                    galleryUrls = galleryUrls,
                    nativeUri = nativeUri,
                    canOpenArchive = canOpenArchive,
                    scrollState = scrollState,
                    onRefresh = onRefresh,
                    onArchive = onArchive,
                    onOpenGallery = onOpenGallery,
                    onOpenReaderSettings = onOpenReaderSettings,
                    onShare = onShare,
                    onOpenOriginal = onOpenOriginal,
                    onOpenNative = onOpenNative,
                    onOpenWayback = onOpenWayback,
                    onOpenArchivePh = onOpenArchivePh,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
private fun HighlightsBarButton(
    highlights: List<PaintedHighlight>,
    hasHighlights: Boolean,
    settings: UserSettings,
    onClick: () -> Unit,
) {
    val look = rememberDisplayLook(settings)
    val tint = highlightPillColor(
        highlights,
        look.mine,
        look.friends,
        look.foaf,
        look.nostrverse,
    )
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (hasHighlights) BorisIcons.Highlighter else Icons.Filled.Bookmark,
            contentDescription = stringResource(R.string.reader_open_highlights),
            tint = if (hasHighlights) tint else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SaveLibraryButton(
    choosePrivacy: Boolean,
    inLibrary: Boolean,
    archived: Boolean,
    canSave: Boolean,
    onSave: (privateBookmark: Boolean) -> Unit,
) {
    val icon = when {
        archived -> Icons.Filled.CheckCircle
        inLibrary -> Icons.Filled.Bookmark
        else -> Icons.Outlined.AddCircle
    }
    val description = stringResource(
        when {
            archived -> R.string.reader_archived
            inLibrary -> R.string.reader_already_saved
            else -> R.string.reader_save_library
        },
    )
    if (!canSave || archived) {
        IconButton(onClick = {}) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (archived) ArchiveGreen else MaterialTheme.colorScheme.onSurface,
            )
        }
        return
    }
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val preferPrivate = settings.defaultPrivateBookmark
    if (!choosePrivacy) {
        IconButton(onClick = { onSave(preferPrivate) }) {
            Icon(icon, contentDescription = description)
        }
        return
    }
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(icon, contentDescription = description)
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            val privateFirst = preferPrivate
            val items = if (privateFirst) {
                listOf(true to R.string.reader_save_private, false to R.string.reader_save_public)
            } else {
                listOf(false to R.string.reader_save_public, true to R.string.reader_save_private)
            }
            items.forEach { (privateBookmark, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    leadingIcon = {
                        Icon(
                            if (privateBookmark) Icons.Outlined.Lock else Icons.Outlined.Public,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onSave(privateBookmark)
                    },
                )
            }
        }
    }
}

@Composable
private fun TtsListenButton(
    content: ReadableContent,
    author: Profile?,
    onEmpty: () -> Unit,
) {
    val session by TtsPlayback.session.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val mine = session?.url == content.url
    val speaking = mine && session?.playing == true && session?.started == true
    val initializing = mine && session?.playing == true && session?.started != true
    val paused = mine && session?.paused == true
    val description = stringResource(
        when {
            speaking -> R.string.tts_pause_playback
            paused -> R.string.tts_resume_playback
            else -> R.string.tts_listen_to_article
        },
    )
    IconButton(onClick = {
        when {
            initializing -> Unit
            speaking -> TtsPlayback.pause()
            paused -> TtsPlayback.resume()
            else -> {
                val paragraphs = TtsText.paragraphs(content)
                if (paragraphs.isEmpty()) {
                    onEmpty()
                    return@IconButton
                }
                requestTtsNotificationPermissionOnce(context) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                TtsPlayback.start(
                    context = context,
                    content = content,
                    startIndex = TtsText.startIndex(
                        ReadingPositionStore.fraction(content.url),
                        paragraphs.size,
                    ),
                    author = content.authorPubkey?.trim()
                        ?.takeIf { it.length == 64 }
                        ?.let { Profile.displayName(it, author) },
                )
            }
        }
    }) {
        Icon(
            imageVector = if (speaking) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
