package org.dergigi.boris.ui.reader

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownText
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.PlaceholderConfig
import com.mikepenz.markdown.model.ReferenceLinkHandlerImpl
import com.mikepenz.markdown.model.State as MarkdownParseState
import com.mikepenz.markdown.model.markdownPadding
import com.mikepenz.markdown.model.rememberMarkdownState
import org.dergigi.boris.R
import org.dergigi.boris.data.ArticleUrl
import org.dergigi.boris.data.Footnotes
import org.dergigi.boris.data.LongParagraphs
import org.dergigi.boris.data.LibrarySave
import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.NostrEventRef
import org.dergigi.boris.data.NostrEventRefs
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.NostrMentions
import org.dergigi.boris.data.NoteCover
import org.dergigi.boris.data.ResolvedEventRef
import org.dergigi.boris.data.ArticleImages
import org.dergigi.boris.data.PublishedTime
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.ReadingTime
import org.dergigi.boris.data.ReadingPositionStore
import org.dergigi.boris.data.ReadingPositionSync
import org.dergigi.boris.data.SensitiveContent
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.ui.ArticleRow
import org.dergigi.boris.ui.AuthorCard
import org.dergigi.boris.ui.NsfwBadge
import org.dergigi.boris.tts.TtsPlayback
import org.dergigi.boris.tts.TtsText
import org.dergigi.boris.tts.requestTtsNotificationPermissionOnce
import org.dergigi.boris.ui.HighlightCard
import org.dergigi.boris.ui.HighlightCardMenu
import org.dergigi.boris.ui.HighlightMenuViewModel
import org.dergigi.boris.ui.SignerEffects
import org.dergigi.boris.ui.ArticleCopyMenuItems
import org.dergigi.boris.ui.hasAlternateCopyLinks
import org.dergigi.boris.ui.openExternalUri
import org.dergigi.boris.ui.browser.InAppBrowser
import org.dergigi.boris.ui.openOriginalArticle
import org.dergigi.boris.ui.shareArticleLink
import org.dergigi.boris.ui.shell.TtsMiniPlayerHost
import org.dergigi.boris.ui.settings.ReadingFonts
import org.dergigi.boris.ui.settings.SettingsViewModel
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.rememberDisplayLook
import coil3.compose.AsyncImage
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.parser.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenReaderSettings: () -> Unit,
    onOpenHighlightSettings: () -> Unit = {},
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit = { url, _, _ ->
        onOpenArticle(url)
    },
    onOpenBrowser: (String) -> Unit,
    viewModel: ReaderViewModel = viewModel(),
    menuViewModel: HighlightMenuViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gallery by viewModel.gallery.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val highlightCount by viewModel.highlightCount.collectAsStateWithLifecycle()
    val highlightsLoaded by viewModel.highlightsLoaded.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()
    val inLibrary by viewModel.inLibrary.collectAsStateWithLifecycle()
    val archived by viewModel.archived.collectAsStateWithLifecycle()
    val author by viewModel.author.collectAsStateWithLifecycle()
    val eventRefs by viewModel.eventRefs.collectAsStateWithLifecycle()
    val rssFeedSuggestion by viewModel.rssFeedSuggestion.collectAsStateWithLifecycle()
    val signIntent by viewModel.signIntent.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val deletedIds by menuViewModel.deleted.collectAsStateWithLifecycle()
    val menuSignIntent by menuViewModel.signIntent.collectAsStateWithLifecycle()
    val menuMessage by menuViewModel.message.collectAsStateWithLifecycle()
    val settingsSignIntent by settingsViewModel.signIntent.collectAsStateWithLifecycle()
    val settingsMessage by settingsViewModel.message.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val imageOnly = state as? ReaderUiState.ImageOnly
    if (imageOnly != null) {
        ImageGallery(
            state = gallery ?: ImageGalleryState(listOf(imageOnly.url), 0),
            onDismiss = onBack,
            onPageChange = viewModel::setGalleryIndex,
        )
        return
    }
    val context = LocalContext.current
    var closeAfterArchive by remember { mutableStateOf(false) }
    val launchSign = SignerEffects(
        signIntent = signIntent,
        message = message,
        onConsumeSignIntent = viewModel::consumeSignIntent,
        onConsumeMessage = viewModel::consumeMessage,
        onSignerResult = viewModel::onSignerResult,
        onMessage = { text ->
            if (isArchiveFailureMessage(context, text)) closeAfterArchive = false
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        },
    )
    SignerEffects(
        signIntent = menuSignIntent,
        message = menuMessage,
        onConsumeSignIntent = menuViewModel::consumeSignIntent,
        onConsumeMessage = menuViewModel::consumeMessage,
        onSignerResult = menuViewModel::onSignerResult,
    )
    SignerEffects(
        signIntent = settingsSignIntent,
        message = settingsMessage,
        onConsumeSignIntent = settingsViewModel::consumeSignIntent,
        onConsumeMessage = settingsViewModel::consumeMessage,
        onSignerResult = settingsViewModel::onSignerResult,
    )
    LaunchedEffect(Unit) {
        PendingHighlight.consume()?.let(viewModel::offerExternalHighlight)
    }
    val readyArticleUrl = (state as? ReaderUiState.Ready)?.content?.url
    LaunchedEffect(readyArticleUrl) {
        closeAfterArchive = false
    }
    LaunchedEffect(archived, closeAfterArchive) {
        if (closeAfterArchive && archived) {
            closeAfterArchive = false
            onBack()
        }
    }
    val visibleHighlights = remember(highlights, deletedIds) {
        highlights.filter { it.id !in deletedIds }
    }
    ReaderScreenContent(
        state = state,
        gallery = gallery,
        highlights = visibleHighlights,
        highlightCount = highlightCount,
        highlightsLoaded = highlightsLoaded,
        focusHighlightId = viewModel.focusHighlightId,
        loggedIn = loggedIn,
        canSave = canSave,
        inLibrary = inLibrary,
        archived = archived,
        author = author,
        eventRefs = eventRefs,
        settings = settings,
        rssFeedSuggestion = rssFeedSuggestion?.takeIf { it !in settings.rssFeeds },
        onBack = onBack,
        onRetry = { viewModel.load() },
        onRefresh = viewModel::refresh,
        onOpenArticle = onOpenArticle,
        onOpenHighlight = onOpenHighlight,
        onOpenProfile = onOpenProfile,
        onOpenReaderSettings = onOpenReaderSettings,
        onOpenHighlightSettings = onOpenHighlightSettings,
        onOpenBrowser = onOpenBrowser,
        onOpenGallery = viewModel::openGallery,
        onCloseGallery = viewModel::closeGallery,
        onGalleryPage = viewModel::setGalleryIndex,
        onHighlight = { quote, ownerText, ownerOffset ->
            viewModel.highlight(quote, ownerText, ownerOffset)?.let(launchSign)
        },
        onSave = { privateBookmark ->
            viewModel.saveToLibrary(privateBookmark)?.let(launchSign)
        },
        onArchive = { closeAfterSuccess ->
            closeAfterArchive = closeAfterSuccess && !archived
            val intent = viewModel.archive()
            if (intent == null && !viewModel.archiveInFlight()) closeAfterArchive = false
            intent?.let(launchSign)
        },
        onAddRssFeed = { feedUrl ->
            settingsViewModel.update { current ->
                if (feedUrl in current.rssFeeds) {
                    current
                } else {
                    current.withStringList("rssFeeds", current.rssFeeds + feedUrl)
                }
            }
            viewModel.dismissRssFeedSuggestion()
        },
        onDismissRssFeed = viewModel::dismissRssFeedSuggestion,
        canDeleteHighlight = menuViewModel::canDelete,
        onDeleteHighlight = menuViewModel::delete,
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

private enum class ReaderOverflowPage { Root, Copy, Open }

@Composable
private fun ReaderOverflowBackItem(
    label: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.reader_menu_back),
            )
        },
        onClick = onClick,
    )
    HorizontalDivider()
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

/** The SDK has no Settings.ACTION_TEXT_TO_SPEECH_SETTINGS constant; this is the settings action. */
private const val ACTION_TEXT_TO_SPEECH_SETTINGS = "com.android.settings.TTS_SETTINGS"
private val ArchiveGreen = Color(0xFF22C55E)

/** D-11 fallback chain: TTS settings, then install-TTS-data, then generic settings. */
internal fun openTtsSettings(context: Context) {
    val candidates = listOf(
        ACTION_TEXT_TO_SPEECH_SETTINGS,
        android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA,
        android.provider.Settings.ACTION_SETTINGS,
    )
    for (action in candidates) {
        try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreenContent(
    state: ReaderUiState,
    gallery: ImageGalleryState?,
    highlights: List<PaintedHighlight>,
    highlightCount: Int,
    highlightsLoaded: Boolean,
    focusHighlightId: String,
    loggedIn: Boolean,
    canSave: Boolean,
    inLibrary: Boolean,
    archived: Boolean,
    author: Profile?,
    eventRefs: Map<String, ResolvedEventRef>,
    settings: UserSettings,
    rssFeedSuggestion: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenHighlight: (url: String, highlightId: String, quote: String) -> Unit = { url, _, _ ->
        onOpenArticle(url)
    },
    onOpenProfile: (String) -> Unit,
    onOpenReaderSettings: () -> Unit = {},
    onOpenHighlightSettings: () -> Unit = {},
    onOpenBrowser: (String) -> Unit,
    onOpenGallery: (List<String>, Int) -> Unit,
    onCloseGallery: () -> Unit,
    onGalleryPage: (Int) -> Unit,
    onHighlight: (quote: String, ownerText: String, ownerOffset: Int) -> Unit,
    onSave: (privateBookmark: Boolean) -> Unit,
    onArchive: (closeAfterSuccess: Boolean) -> Unit,
    onAddRssFeed: (String) -> Unit,
    onDismissRssFeed: () -> Unit,
    canDeleteHighlight: (String?) -> Boolean = { false },
    onDeleteHighlight: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val articleUrl = when (state) {
        is ReaderUiState.Ready -> state.content.url
        is ReaderUiState.Highlight -> state.eventUrl
        is ReaderUiState.Error -> state.url
        is ReaderUiState.Loading -> state.url.takeIf { it.isNotBlank() }
        is ReaderUiState.ImageOnly -> state.url
    }
    val galleryUrls = remember(state) {
        (state as? ReaderUiState.Ready)?.let { ArticleImages.urlsFor(it.content) }.orEmpty()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val readerScope = rememberCoroutineScope()
    val articleScrollState = rememberScrollState()
    val hideBar = settings.hideTopBarOnScroll
    val barOffsetPx = remember { mutableFloatStateOf(0f) }
    val barHeightPx = remember { mutableIntStateOf(0) }
    val hideBarConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val height = barHeightPx.intValue.toFloat()
                if (height <= 0f) return Offset.Zero
                barOffsetPx.floatValue =
                    (barOffsetPx.floatValue + consumed.y).coerceIn(-height, 0f)
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(articleUrl) {
        articleScrollState.scrollTo(0)
        barOffsetPx.floatValue = 0f
    }
    LaunchedEffect(hideBar) {
        if (!hideBar) barOffsetPx.floatValue = 0f
    }
    TtsReaderError(
        articleUrl = articleUrl,
        snackbarHostState = snackbarHostState,
    )

    fun openOriginal() {
        val url = articleUrl ?: return
        val target = InAppBrowser.targetUrl(url)
        if (target != null) onOpenBrowser(target) else openOriginalArticle(context, url)
    }

    fun openWayback() {
        val url = articleUrl ?: return
        InAppBrowser.waybackUrl(url)?.let(onOpenBrowser)
    }

    fun openArchivePh() {
        val url = articleUrl ?: return
        InAppBrowser.archivePhUrl(url)?.let(onOpenBrowser)
    }

    val canOpenArchive = articleUrl?.let { InAppBrowser.waybackUrl(it) } != null

    fun openNative() {
        val url = articleUrl ?: return
        val nostrUri = NostrLink.parse(url)?.uri ?: return
        openExternalUri(context, nostrUri)
    }

    fun shareArticle() {
        val url = articleUrl ?: return
        val title = (state as? ReaderUiState.Ready)?.content?.title
        shareArticleLink(context, title, url)
    }

    var findOpen by remember { mutableStateOf(false) }
    var outlineOpen by remember { mutableStateOf(false) }
    var highlightsOpen by remember { mutableStateOf(false) }
    val readyBody = (state as? ReaderUiState.Ready)?.content?.body
    var outlineItems by remember(readyBody) {
        mutableStateOf(readyBody?.let { ArticleOutline.parse(it) }.orEmpty())
    }
    LaunchedEffect(findOpen) {
        if (findOpen) outlineOpen = false
    }
    LaunchedEffect(articleUrl) {
        outlineOpen = false
        highlightsOpen = false
    }
    var rssConfirmFeed by remember { mutableStateOf<String?>(null) }
    rssConfirmFeed?.let { feedUrl ->
        AlertDialog(
            onDismissRequest = {
                rssConfirmFeed = null
                onDismissRssFeed()
            },
            title = { Text(stringResource(R.string.reader_rss_confirm_title)) },
            text = { Text(stringResource(R.string.reader_rss_confirm_body, feedUrl)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        rssConfirmFeed = null
                        onAddRssFeed(feedUrl)
                    },
                ) {
                    Text(stringResource(R.string.reader_rss_confirm_add))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        rssConfirmFeed = null
                        onDismissRssFeed()
                    },
                ) {
                    Text(stringResource(R.string.reader_rss_confirm_cancel))
                }
            },
        )
    }
    val chromeBar: @Composable (Modifier) -> Unit = { barModifier ->
            TopAppBar(
                modifier = barModifier,
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
                                readerScope.launch {
                                    articleScrollState.animateScrollTo(0)
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
                                onClick = {
                                    findOpen = false
                                    outlineOpen = true
                                },
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
                                onClick = { highlightsOpen = true },
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
                                readerScope.launch {
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
                        var menuOpen by remember { mutableStateOf(false) }
                        var menuPage by remember { mutableStateOf(ReaderOverflowPage.Root) }
                        fun dismissMenu() {
                            menuOpen = false
                            menuPage = ReaderOverflowPage.Root
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { dismissMenu() },
                            ) {
                                // Read inside the menu so progress saves (which
                                // bump the store version on every scroll settle)
                                // do not recompose the whole scaffold.
                                val progressVersion by ReadingPositionStore.version
                                    .collectAsStateWithLifecycle()
                                val hasProgress = remember(articleUrl, progressVersion) {
                                    articleUrl != null &&
                                        ReadingPositionStore.fraction(articleUrl) > 0f
                                }
                                val copyHasExtras = remember(articleUrl) {
                                    hasAlternateCopyLinks(articleUrl)
                                }
                                val articleReady = state is ReaderUiState.Ready
                                val showArticleActions = articleReady || hasProgress
                                val showMarkAsRead =
                                    loggedIn && articleReady && !archived
                                when (menuPage) {
                                    ReaderOverflowPage.Root -> {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.reader_share)) },
                                            leadingIcon = {
                                                Icon(Icons.Filled.Share, contentDescription = null)
                                            },
                                            onClick = {
                                                dismissMenu()
                                                shareArticle()
                                            },
                                        )
                                        if (copyHasExtras) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.reader_copy)) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Filled.ContentCopy,
                                                        contentDescription = null,
                                                    )
                                                },
                                                trailingIcon = {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    menuPage = ReaderOverflowPage.Copy
                                                },
                                            )
                                        } else {
                                            ArticleCopyMenuItems(
                                                url = articleUrl,
                                                onDismiss = { dismissMenu() },
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.reader_open)) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Language,
                                                    contentDescription = null,
                                                )
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                menuPage = ReaderOverflowPage.Open
                                            },
                                        )
                                        if (articleReady) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.reader_find)) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Search,
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    dismissMenu()
                                                    findOpen = true
                                                },
                                            )
                                            if (galleryUrls.isNotEmpty()) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(stringResource(R.string.reader_open_gallery))
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Outlined.PhotoLibrary,
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    onClick = {
                                                        dismissMenu()
                                                        onOpenGallery(galleryUrls, 0)
                                                    },
                                                )
                                            }
                                        }
                                        if (showArticleActions) {
                                            HorizontalDivider()
                                            if (articleReady) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(stringResource(R.string.reader_refresh))
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Outlined.Refresh,
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    onClick = {
                                                        dismissMenu()
                                                        onRefresh()
                                                    },
                                                )
                                            }
                                            if (hasProgress) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(stringResource(R.string.reader_reset_progress))
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Outlined.RestartAlt,
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    onClick = {
                                                        val url = articleUrl
                                                            ?: return@DropdownMenuItem
                                                        dismissMenu()
                                                        ReadingPositionStore.reset(url)
                                                        readerScope.launch {
                                                            articleScrollState.animateScrollTo(0)
                                                        }
                                                    },
                                                )
                                            }
                                            if (showMarkAsRead) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(stringResource(R.string.reader_mark_as_read))
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Filled.CheckCircle,
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    onClick = {
                                                        dismissMenu()
                                                        onArchive(false)
                                                    },
                                                )
                                            }
                                        }
                                        if (loggedIn) {
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = {
                                                    Text(stringResource(R.string.reader_settings))
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Settings,
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    dismissMenu()
                                                    onOpenReaderSettings()
                                                },
                                            )
                                        }
                                    }
                                    ReaderOverflowPage.Copy -> {
                                        ReaderOverflowBackItem(
                                            label = stringResource(R.string.reader_copy),
                                            onClick = { menuPage = ReaderOverflowPage.Root },
                                        )
                                        ArticleCopyMenuItems(
                                            url = articleUrl,
                                            onDismiss = { dismissMenu() },
                                        )
                                    }
                                    ReaderOverflowPage.Open -> {
                                        ReaderOverflowBackItem(
                                            label = stringResource(R.string.reader_open),
                                            onClick = { menuPage = ReaderOverflowPage.Root },
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(stringResource(R.string.reader_open_in_browser))
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Language,
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                dismissMenu()
                                                openOriginal()
                                            },
                                        )
                                        if (nativeUri != null) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(stringResource(R.string.reader_open_native))
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Smartphone,
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    dismissMenu()
                                                    openNative()
                                                },
                                            )
                                        }
                                        if (canOpenArchive) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(stringResource(R.string.reader_open_wayback))
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.History,
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    dismissMenu()
                                                    openWayback()
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(stringResource(R.string.reader_open_archive_ph))
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Public,
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    dismissMenu()
                                                    openArchivePh()
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
    }
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = if (hideBar) {
            Modifier.nestedScroll(hideBarConnection)
        } else {
            Modifier
        },
        topBar = { if (!hideBar) chromeBar(Modifier) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val fallbackBarPx = WindowInsets.statusBars.getTop(density) + with(density) { 64.dp.roundToPx() }
        val overlayBarPx = barHeightPx.intValue.takeIf { it > 0 } ?: fallbackBarPx
        val overlayBarDp = with(density) { overlayBarPx.toDp() }
        val sidePad = Modifier.padding(
            start = innerPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection),
            bottom = innerPadding.calculateBottomPadding(),
        )
        val pinnedPad = if (hideBar) {
            sidePad.padding(top = overlayBarDp)
        } else {
            Modifier.padding(innerPadding)
        }
        when (state) {
            is ReaderUiState.Loading -> {
                val hasPreview = !state.title.isNullOrBlank() || !state.imageUrl.isNullOrBlank()
                if (!hasPreview) {
                    Box(
                        modifier = Modifier.fillMaxSize().then(pinnedPad),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(pinnedPad)
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
            is ReaderUiState.Error -> {
                var detailsOpen by remember(state.detail) { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(pinnedPad)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
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
                            onClick = ::openOriginal,
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
                            TextButton(onClick = ::openWayback) {
                                Text(stringResource(R.string.reader_open_wayback))
                            }
                            TextButton(onClick = ::openArchivePh) {
                                Text(stringResource(R.string.reader_open_archive_ph))
                            }
                        }
                    }
                }
            }
            is ReaderUiState.ImageOnly -> Unit
            is ReaderUiState.Highlight -> {
                OpenedHighlightPane(
                    state = state,
                    author = author,
                    settings = settings,
                    onOpenHighlight = onOpenHighlight,
                    onOpenProfile = onOpenProfile,
                    canDeleteHighlight = canDeleteHighlight,
                    onDeleteHighlight = onDeleteHighlight,
                    modifier = pinnedPad,
                )
            }
            is ReaderUiState.Ready -> {
                val warning = remember(state.content.url, state.content.title, state.content.tags) {
                    SensitiveContent.classify(state.content)
                }
                var revealed by remember(state.content.url) { mutableStateOf(false) }
                if (warning != null && settings.nsfwWarnInReader && !revealed) {
                    SensitiveContentGate(
                        warning = warning,
                        onShow = { revealed = true },
                        modifier = if (hideBar) pinnedPad else Modifier.padding(innerPadding),
                    )
                } else {
                ArticleBody(
                    content = state.content,
                    highlights = highlights,
                    highlightCount = highlightCount,
                    highlightsLoaded = highlightsLoaded,
                    focusHighlightId = focusHighlightId,
                    loggedIn = loggedIn,
                    archived = archived,
                    author = author,
                    eventRefs = eventRefs,
                    settings = settings,
                    rssFeedSuggestion = rssFeedSuggestion,
                    // D-19: while TTS is speaking, volume keys change volume, not scroll.
                    volumeScroll = gallery == null,
                    findOpen = findOpen,
                    onFindOpenChange = { findOpen = it },
                    outlineOpen = outlineOpen,
                    onOutlineOpenChange = { outlineOpen = it },
                    highlightsOpen = highlightsOpen,
                    onHighlightsOpenChange = { highlightsOpen = it },
                    outlineItems = outlineItems,
                    onOutlineItems = { outlineItems = it },
                    onOpenArticle = onOpenArticle,
                    onOpenProfile = onOpenProfile,
                    onAddRssFeed = { feed -> rssConfirmFeed = feed },
                    onOpenHighlightSettings = onOpenHighlightSettings,
                    onOpenGallery = onOpenGallery,
                    onHighlight = onHighlight,
                    onArchive = onArchive,
                    canDeleteHighlight = canDeleteHighlight,
                    onDeleteHighlight = onDeleteHighlight,
                    scrollState = articleScrollState,
                    topScrollInsetPx = if (hideBar) overlayBarPx else 0,
                    jumpChromePx = remember(hideBar, overlayBarPx, barOffsetPx) {
                        {
                            if (!hideBar) {
                                0
                            } else {
                                HighlightJump.visibleOverlayPx(overlayBarPx, barOffsetPx.floatValue)
                            }
                        }
                    },
                    modifier = if (hideBar) sidePad else Modifier.padding(innerPadding),
                )
                }
            }
        }
    }
    if (hideBar) {
        chromeBar(
            Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { barHeightPx.intValue = it.height }
                .graphicsLayer { translationY = barOffsetPx.floatValue }
                .zIndex(1f),
        )
    }
    gallery?.let { open ->
        ImageGallery(
            state = open,
            onDismiss = onCloseGallery,
            onPageChange = onGalleryPage,
        )
    }
    }
}

@Composable
private fun OpenedHighlightPane(
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

@Composable
private fun ArticleBody(
    content: ReadableContent,
    highlights: List<PaintedHighlight>,
    highlightCount: Int,
    highlightsLoaded: Boolean,
    focusHighlightId: String,
    loggedIn: Boolean,
    archived: Boolean,
    author: Profile?,
    eventRefs: Map<String, ResolvedEventRef>,
    settings: UserSettings,
    rssFeedSuggestion: String?,
    onOpenArticle: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onAddRssFeed: (String) -> Unit,
    onOpenHighlightSettings: () -> Unit = {},
    onOpenGallery: (List<String>, Int) -> Unit,
    onHighlight: (quote: String, ownerText: String, ownerOffset: Int) -> Unit,
    onArchive: (closeAfterSuccess: Boolean) -> Unit,
    canDeleteHighlight: (String?) -> Boolean = { false },
    onDeleteHighlight: (String) -> Unit = {},
    findOpen: Boolean,
    onFindOpenChange: (Boolean) -> Unit,
    outlineOpen: Boolean,
    onOutlineOpenChange: (Boolean) -> Unit,
    highlightsOpen: Boolean,
    onHighlightsOpenChange: (Boolean) -> Unit,
    outlineItems: List<ArticleOutlineItem>,
    onOutlineItems: (List<ArticleOutlineItem>) -> Unit,
    scrollState: ScrollState,
    topScrollInsetPx: Int = 0,
    jumpChromePx: () -> Int = { topScrollInsetPx },
    volumeScroll: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val readingTime = readingTimeLabel(content.body)
    val rootUrl = ArticleUrl.root(content.url)
    val domain = rootUrl?.let { ArticleUrl.host(content.url) }
    val published = content.publishedAt?.let { PublishedTime.label(it) }
    val highlightsLabel = highlightCountLabel(highlightCount)
    val defaultUriHandler = LocalUriHandler.current
    val openLinksInReader = settings.openLinksInReader
    val ttsContext = LocalContext.current
    val ttsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val uriHandler = remember(content.url, onOpenArticle, onOpenProfile, defaultUriHandler, openLinksInReader) {
        object : UriHandler {
            override fun openUri(uri: String) {
                when (val action = readerLinkAction(uri, content.url, openLinksInReader)) {
                    ReaderLinkAction.Ignore -> Unit
                    is ReaderLinkAction.OpenInReader -> onOpenArticle(action.url)
                    is ReaderLinkAction.OpenExternal -> defaultUriHandler.openUri(action.url)
                    is ReaderLinkAction.OpenProfile -> onOpenProfile(action.pubkeyHex)
                }
            }
        }
    }
    val fullWidthImages = settings.fullWidthImages
    val maxImageHeight = (LocalConfiguration.current.screenHeightDp * 0.7f).dp
    val onImageClick = remember(content.url, content.body, content.imageUrl, onOpenGallery) {
        { link: String ->
            val opened = UrlExtractor.articleUrl(link, content.url)
                ?.let(UrlExtractor::preferHttps)
            if (opened != null) {
                val urls = ArticleImages.urlsFor(content).toMutableList()
                if (opened !in urls) urls.add(opened)
                onOpenGallery(urls, urls.indexOf(opened).coerceAtLeast(0))
            }
        }
    }
    // Remembered so Markdown sees an equal argument and can skip its whole
    // node tree on unrelated recompositions; a fresh instance per pass forced
    // a full re-walk of every markdown node and made long articles lag (#131).
    val imageTransformer = remember(fullWidthImages, maxImageHeight, onImageClick) {
        ClickableCoilImageTransformer(
            fullWidth = fullWidthImages,
            maxHeight = maxImageHeight,
            onImageClick = onImageClick,
        )
    }
    // Captured once: the focus target is cleared after the jump, and re-reading
    // it on the next highlights update made a feed-opened highlight that the
    // relays did not return vanish right after it appeared (#132).
    val focusedQuote = remember(focusHighlightId) {
        ReaderFocus.peek()
            ?.takeIf { it.highlightId.equals(focusHighlightId, ignoreCase = true) }
            ?.quote
            ?.takeIf { it.isNotBlank() }
    }
    val focusQuote = focusedQuote
        ?: highlights.firstOrNull { it.id.equals(focusHighlightId, ignoreCase = true) }?.quote
    var findQuery by remember { mutableStateOf("") }
    var findIndex by remember { mutableIntStateOf(0) }
    var findJump by remember { mutableIntStateOf(0) }
    val findHaystack = remember(content.title, content.body) {
        listOfNotNull(content.title?.takeIf { it.isNotBlank() }, content.body)
            .joinToString("\n\n")
    }
    val findHits = remember(findHaystack, findQuery) {
        ArticleFind.hits(findHaystack, findQuery)
    }
    // D-12/D-13: spoken mark lives on SpokenMarkState so sentence ticks and
    // follow-along pause do not rebuild this NIP-84/find list (or remasure
    // the article) while the user is scrolling.
    val spokenMark = remember { SpokenMarkState() }
    val painted = HighlightJump.withFocus(
        highlights.visibleFor(settings),
        focusHighlightId,
        focusQuote,
    ) + listOfNotNull(
        ArticleFind.painted(findQuery),
    )
    val family = ReadingFonts.family(settings.readingFont)
    val bodySize = settings.fontSize.sp
    val bodyLine = (settings.fontSize * 36f / 21f).sp
    val align = if (settings.justifyParagraphs) TextAlign.Justify else TextAlign.Start
    val look = rememberDisplayLook(settings)
    val mineColor = look.mine
    val friendsColor = look.friends
    val foafColor = look.foaf
    val otherColor = look.nostrverse
    val underline = look.underline
    val linkColor = look.link
    val body = typography.bodyLarge.copy(
        fontFamily = family,
        fontSize = bodySize,
        lineHeight = bodyLine,
        textAlign = align,
        letterSpacing = 0.sp,
    )
    val headingFamily = typography.headlineLarge.copy(fontFamily = family)
    val clipboard = LocalClipboardManager.current
    val density = LocalDensity.current
    // Deferred: jumpChromePx reads the top-bar slide offset, which changes on
    // every scroll frame. Calling it here would recompose this whole
    // composable, and with it the entire markdown tree, per frame (#131).
    val paneTopPadding = remember(density, jumpChromePx) {
        { with(density) { jumpChromePx().toDp() } }
    }
    val selection = remember { ReaderSelectionState() }
    val scope = rememberCoroutineScope()
    var titleTtsIndex by remember(content.url) { mutableStateOf<Int?>(null) }
    LaunchedEffect(content.title, content.summary, content.body) {
        titleTtsIndex = withContext(Dispatchers.Default) {
            content.title?.let {
                TtsText.startIndexForSelection(content, ownerText = it, selectedText = it)
            }
        }
    }
    fun startTtsFromSelection() {
        val selected = selection.selectedText
        val ownerText = selection.text
        val ownerOffset = selection.range.min
        val explicitStartIndex = selection.ttsStartIndex
        if (selected.isBlank()) return
        selection.clear()
        requestTtsNotificationPermissionOnce(ttsContext) {
            ttsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        scope.launch {
            val startIndex = withContext(Dispatchers.Default) {
                if (TtsText.paragraphs(content).isEmpty()) return@withContext null
                explicitStartIndex ?: TtsText.startIndexForSelection(
                    content = content,
                    ownerText = ownerText,
                    selectedText = selected,
                )
            } ?: return@launch
            val authorName = content.authorPubkey?.trim()
                ?.takeIf { it.length == 64 }
                ?.let { Profile.displayName(it, author) }
            TtsPlayback.playFrom(
                context = ttsContext,
                content = content,
                startIndex = startIndex,
                author = authorName,
                selectedText = selected,
                ownerText = ownerText,
                ownerOffset = ownerOffset,
            )
        }
    }
    val navigator = remember { HighlightNavigator() }
    val activeOutlineId = remember { mutableStateOf<String?>(null) }
    val paintedHolder = remember { mutableStateOf(painted) }
    paintedHolder.value = painted
    var viewportHeight by remember { mutableIntStateOf(0) }
    var scrollViewport by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val jumpState = rememberUpdatedState<(HighlightStop) -> Unit> { stop ->
        val coords = navigator.coordinates(stop.owner) ?: return@rememberUpdatedState
        val viewport = scrollViewport ?: return@rememberUpdatedState
        if (!coords.isAttached || !viewport.isAttached) return@rememberUpdatedState
        val y = viewport.localPositionOf(coords, Offset(0f, stop.localTop)).y
        val pad = HighlightJump.chromePadding(jumpChromePx(), with(density) { 48.dp.toPx() })
        val target = HighlightJump.scrollTarget(scrollState.value, scrollState.maxValue, y, pad)
        if (target != scrollState.value) {
            scope.launch { scrollState.animateScrollTo(target) }
        }
    }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var pendingJumpId by remember {
        mutableStateOf(focusHighlightId.takeIf { it.isNotBlank() })
    }
    LaunchedEffect(highlightsOpen) {
        if (!highlightsOpen) return@LaunchedEffect
        onOutlineOpenChange(false)
        if (findOpen) {
            findQuery = ""
            findIndex = 0
            onFindOpenChange(false)
        }
    }
    LaunchedEffect(findOpen) {
        if (findOpen) {
            onHighlightsOpenChange(false)
            onOutlineOpenChange(false)
        }
    }
    LaunchedEffect(outlineOpen) {
        if (outlineOpen) {
            onHighlightsOpenChange(false)
            if (findOpen) {
                findQuery = ""
                findIndex = 0
                onFindOpenChange(false)
            }
        }
    }
    LaunchedEffect(findJump, findQuery, painted) {
        if (findQuery.isBlank()) return@LaunchedEffect
        val stop = withTimeoutOrNull(10_000L) {
            snapshotFlow {
                if (scrollViewport?.isAttached != true) return@snapshotFlow null
                navigator.nthStop(ArticleFind.HIGHLIGHT_ID, findIndex)
            }.filterNotNull().first()
        } ?: return@LaunchedEffect
        jumpState.value(navigator.select(stop))
        selectedId = ArticleFind.HIGHLIGHT_ID
    }
    LaunchedEffect(pendingJumpId, painted, highlightsLoaded) {
        val id = pendingJumpId ?: return@LaunchedEffect
        if (!ArticleOutline.isId(id) &&
            painted.none { it.id.equals(id, ignoreCase = true) } &&
            !highlightsLoaded
        ) {
            return@LaunchedEffect
        }
        val stop = HighlightJump.awaitStop(navigator, id) {
            scrollViewport?.isAttached == true
        } ?: return@LaunchedEffect
        jumpState.value(navigator.select(stop))
        selectedId = id
        pendingJumpId = null
        ReaderFocus.clear()
    }
    val appContext = LocalContext.current.applicationContext
    LaunchedEffect(content.url, content.body) {
        if (!ArticleImages.enabled()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            ArticleImages.ensure(appContext, ArticleImages.urlsFor(content))
        }
    }
    var positionRestored by remember(content.url) { mutableStateOf(false) }
    // True once the markdown body has parsed and rendered. Before that the
    // scroll range only covers title/summary chrome, so any progress computed
    // against it is garbage: scrolling a still-loading article could save
    // 100% and even auto-archive it (issue #131).
    var articleReady by remember(content.url) { mutableStateOf(false) }
    LaunchedEffect(content.url) {
        if (positionRestored) return@LaunchedEffect
        if (focusHighlightId.isNotBlank()) {
            positionRestored = true
            return@LaunchedEffect
        }
        // Throttled no-op most of the time; first call pulls positions from relays.
        withContext(Dispatchers.IO) { ReadingPositionSync.refresh(appContext) }
        val saved = ReadingPositionStore.fraction(content.url)
        val max = snapshotFlow { if (articleReady) scrollState.maxValue else 0 }.first { it > 0 }
        if (settings.autoScrollToReadingPosition && scrollState.value == 0) {
            ReadingProgress.restoreOffset(saved, max)?.let { scrollState.scrollTo(it) }
        }
        positionRestored = true
    }
    // Issue #86: while the user explores far away from the saved reading
    // position, keep that position frozen and offer a jump back. The tracker
    // only adopts the new spot after sustained reading-like movement there.
    val readingTracker = remember(content.url) { ReadingTracker() }
    var drifting by remember(content.url) { mutableStateOf(false) }
    LaunchedEffect(content.url) {
        snapshotFlow { scrollState.value }.collectLatest { value ->
            if (!positionRestored || !articleReady) return@collectLatest
            val max = scrollState.maxValue
            if (max <= 0) return@collectLatest
            delay(400)
            val saved = ReadingPositionStore.fraction(content.url)
            val reading = readingTracker.onSettle(
                offset = value,
                savedOffset = (saved * max).roundToInt(),
                viewportHeight = viewportHeight,
                nowMs = System.currentTimeMillis(),
            )
            if (reading) {
                ReadingPositionStore.save(content.url, ReadingProgress.fraction(value, max))
            }
            drifting = readingTracker.drifting
        }
    }
    // The saved fraction is only shown while drifted (jump-back pill) or while
    // the body is still parsing (loading progress bar). Only subscribe to the
    // store then: every save bumps version, and an unconditional read here
    // would recompose the whole article on each scroll-settle save.
    val savedFraction = if (drifting || !articleReady) {
        val progressVersion by ReadingPositionStore.version.collectAsStateWithLifecycle()
        remember(content.url, progressVersion) {
            ReadingPositionStore.fraction(content.url)
        }
    } else {
        0f
    }
    val driftFraction = savedFraction.takeIf { drifting }
    val jumpBackFraction = driftFraction?.takeIf { ReadingProgress.showsJumpBack(it) }
    TtsSpokenSync(
        url = content.url,
        spoken = spokenMark,
        scrollState = scrollState,
        navigator = navigator,
        scrollViewport = scrollViewport,
        positionRestored = positionRestored,
        settingsFollowAlong = settings.ttsFollowAlong,
        jumpChromePx = jumpChromePx,
    )
    DisposableEffect(content.url) {
        onDispose { ReadingPositionSync.publishAsync(appContext, content.url) }
    }
    val autoArchive = settings.autoMarkAsReadOnCompletion
    LaunchedEffect(content.url, loggedIn, archived, autoArchive) {
        snapshotFlow { ReadingProgress.percent(scrollState.value, scrollState.maxValue) }
            .collectLatest { percent ->
                if (percent < 100 || !positionRestored || !articleReady) return@collectLatest
                // Mirrors the webapp: complete only after holding 100% for 2s.
                delay(2000)
                ReadingPositionSync.publishAsync(appContext, content.url)
                if (autoArchive && loggedIn && !archived) onArchive(false)
            }
    }
    val openHighlights = rememberUpdatedState(onHighlightsOpenChange)
    val openFromStop = remember<(HighlightStop) -> Unit> {
        { stop ->
            jumpState.value(navigator.select(stop))
            selectedId = stop.highlightId
            onFindOpenChange(false)
            openHighlights.value(true)
        }
    }
    fun stepFind(delta: Int) {
        val count = findHits.size
        if (count <= 0) return
        findIndex = Math.floorMod(findIndex + delta, count)
        findJump++
    }
    fun goFind(index: Int) {
        if (index !in findHits.indices) return
        findIndex = index
        findJump++
    }
    val noteByAuthor = stringResource(R.string.reader_note_by)
    var markdownBody by remember(content.url) { mutableStateOf<String?>(null) }
    var ttsOffsetIndex by remember(content.url) {
        mutableStateOf<TtsText.MarkdownOffsetIndex?>(null)
    }
    LaunchedEffect(content.url, content.body, eventRefs, noteByAuthor) {
        val body = withContext(Dispatchers.Default) {
            LongParagraphs.split(
                NostrMentions.rewrite(
                    NostrEventRefs.rewrite(Footnotes.expand(content.body), eventRefs) { name ->
                        noteByAuthor.format(name)
                    },
                ),
            )
        }
        markdownBody = body
        ttsOffsetIndex = withContext(Dispatchers.Default) {
            TtsText.markdownOffsetIndex(content, body)
        }
    }
    LaunchedEffect(markdownBody) {
        val body = markdownBody ?: return@LaunchedEffect
        onOutlineItems(withContext(Dispatchers.Default) { ArticleOutline.parse(body) })
    }
    // Cheap range lookup; the regex-heavy walk happens once, off the main thread.
    fun ttsStartIndexForRenderedMarkdown(markdownOffset: Int): Int? =
        ttsOffsetIndex?.startIndexFor(markdownOffset)
    val highlightedComponents = remember(
        mineColor,
        friendsColor,
        foafColor,
        otherColor,
        underline,
        selection,
        navigator,
        openFromStop,
        spokenMark,
        content.url,
        content.title,
        content.summary,
        content.body,
        markdownBody,
        ttsOffsetIndex,
        fullWidthImages,
        maxImageHeight,
        onImageClick,
        defaultUriHandler,
        eventRefs,
        onOpenArticle,
        outlineItems,
    ) {
        markdownComponents(
            text = {
                HighlightedMarkdownNode(
                    it,
                    it.typography.text,
                    paintedHolder.value,
                    mineColor,
                    friendsColor,
                    foafColor,
                    otherColor,
                    underline,
                    selection,
                    navigator,
                    openFromStop,
                    spokenMark,
                    ttsStartIndexForRenderedMarkdown(it.node.startOffset),
                )
            },
            paragraph = { model ->
                val imageUrls = standaloneMarkdownImageUrls(model.content, model.node)
                val youtube = standaloneYoutubePreview(model.content, model.node)
                val eventRef = standaloneEventRef(model.content, model.node)
                val eventEmbed = eventRef?.eventId?.let { eventRefs[it] }
                when {
                    imageUrls.isNotEmpty() -> {
                        imageUrls.forEach { imageUrl ->
                            ArticleImage(
                                url = UrlExtractor.articleUrl(imageUrl, content.url) ?: imageUrl,
                                fullWidth = fullWidthImages,
                                maxHeight = maxImageHeight,
                                onClick = onImageClick,
                            )
                        }
                    }
                    youtube != null -> {
                        ArticleImage(
                            url = youtube.thumbnailUrl,
                            fullWidth = fullWidthImages,
                            maxHeight = maxImageHeight,
                            onClick = { defaultUriHandler.openUri(youtube.watchUrl) },
                        )
                    }
                    eventRef != null && eventEmbed != null -> {
                        EventRefCard(
                            ref = eventRef,
                            resolved = eventEmbed,
                            onOpen = onOpenArticle,
                        )
                    }
                    else -> {
                        HighlightedMarkdownNode(
                            model,
                            model.typography.paragraph,
                            paintedHolder.value,
                            mineColor,
                            friendsColor,
                            foafColor,
                            otherColor,
                            underline,
                            selection,
                            navigator,
                            openFromStop,
                            spokenMark,
                            ttsStartIndexForRenderedMarkdown(model.node.startOffset),
                        )
                    }
                }
            },
            image = { model ->
                val imageUrl = markdownImageDestination(model.content, model.node)
                if (imageUrl != null) {
                    ArticleImage(
                        url = UrlExtractor.articleUrl(imageUrl, content.url) ?: imageUrl,
                        fullWidth = fullWidthImages,
                        maxHeight = maxImageHeight,
                        onClick = onImageClick,
                    )
                }
            },
            heading1 = {
                HighlightedMarkdownNode(
                    it,
                    it.typography.h1,
                    paintedHolder.value,
                    mineColor,
                    friendsColor,
                    foafColor,
                    otherColor,
                    underline,
                    selection,
                    navigator,
                    openFromStop,
                    spokenMark,
                    ttsStartIndexForRenderedMarkdown(it.node.startOffset),
                    outlineItems,
                    it.node.startOffset,
                    MarkdownTokenTypes.ATX_CONTENT,
                    isHeading = true,
                )
            },
            heading2 = {
                HighlightedMarkdownNode(
                    it,
                    it.typography.h2,
                    paintedHolder.value,
                    mineColor,
                    friendsColor,
                    foafColor,
                    otherColor,
                    underline,
                    selection,
                    navigator,
                    openFromStop,
                    spokenMark,
                    ttsStartIndexForRenderedMarkdown(it.node.startOffset),
                    outlineItems,
                    it.node.startOffset,
                    MarkdownTokenTypes.ATX_CONTENT,
                    isHeading = true,
                )
            },
            heading3 = {
                HighlightedMarkdownNode(
                    it,
                    it.typography.h3,
                    paintedHolder.value,
                    mineColor,
                    friendsColor,
                    foafColor,
                    otherColor,
                    underline,
                    selection,
                    navigator,
                    openFromStop,
                    spokenMark,
                    ttsStartIndexForRenderedMarkdown(it.node.startOffset),
                    outlineItems,
                    it.node.startOffset,
                    MarkdownTokenTypes.ATX_CONTENT,
                    isHeading = true,
                )
            },
            heading4 = {
                HighlightedMarkdownNode(
                    it,
                    it.typography.h4,
                    paintedHolder.value,
                    mineColor,
                    friendsColor,
                    foafColor,
                    otherColor,
                    underline,
                    selection,
                    navigator,
                    openFromStop,
                    spokenMark,
                    ttsStartIndexForRenderedMarkdown(it.node.startOffset),
                    outlineItems,
                    it.node.startOffset,
                    MarkdownTokenTypes.ATX_CONTENT,
                    isHeading = true,
                )
            },
            heading5 = {
                HighlightedMarkdownNode(
                    it,
                    it.typography.h5,
                    paintedHolder.value,
                    mineColor,
                    friendsColor,
                    foafColor,
                    otherColor,
                    underline,
                    selection,
                    navigator,
                    openFromStop,
                    spokenMark,
                    ttsStartIndexForRenderedMarkdown(it.node.startOffset),
                    outlineItems,
                    it.node.startOffset,
                    MarkdownTokenTypes.ATX_CONTENT,
                    isHeading = true,
                )
            },
            heading6 = {
                HighlightedMarkdownNode(
                    it,
                    it.typography.h6,
                    paintedHolder.value,
                    mineColor,
                    friendsColor,
                    foafColor,
                    otherColor,
                    underline,
                    selection,
                    navigator,
                    openFromStop,
                    spokenMark,
                    ttsStartIndexForRenderedMarkdown(it.node.startOffset),
                    outlineItems,
                    it.node.startOffset,
                    MarkdownTokenTypes.ATX_CONTENT,
                    isHeading = true,
                )
            },
            setextHeading1 = {
                HighlightedMarkdownNode(
                    it,
                    it.typography.h1,
                    paintedHolder.value,
                    mineColor,
                    friendsColor,
                    foafColor,
                    otherColor,
                    underline,
                    selection,
                    navigator,
                    openFromStop,
                    spokenMark,
                    ttsStartIndexForRenderedMarkdown(it.node.startOffset),
                    outlineItems,
                    it.node.startOffset,
                    MarkdownTokenTypes.SETEXT_CONTENT,
                    isHeading = true,
                )
            },
            setextHeading2 = {
                HighlightedMarkdownNode(
                    it,
                    it.typography.h2,
                    paintedHolder.value,
                    mineColor,
                    friendsColor,
                    foafColor,
                    otherColor,
                    underline,
                    selection,
                    navigator,
                    openFromStop,
                    spokenMark,
                    ttsStartIndexForRenderedMarkdown(it.node.startOffset),
                    outlineItems,
                    it.node.startOffset,
                    MarkdownTokenTypes.SETEXT_CONTENT,
                    isHeading = true,
                )
            },
        )
    }
    val ttsSpeaking by remember {
        TtsPlayback.session.map { it?.playing == true && it.started == true }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(false)
    VolumeKeys.Handle(enabled = volumeScroll && !ttsSpeaking && settings.volumeButtonScroll) { up ->
        val page = VolumeKeys.pageSize(viewportHeight, settings.volumeButtonScrollPercent)
        val target = VolumeKeys.nextOffset(scrollState.value, scrollState.maxValue, page, up)
        if (target != scrollState.value) {
            scope.launch { scrollState.animateScrollTo(target) }
        }
        true
    }
    // Rewrite and GFM parse are off the first Ready frame. Until the first
    // Success, keep a spinner so fetch-complete does not flash an empty body.
    val flavour = remember { GFMFlavourDescriptor() }
    val parser = remember(flavour) { MarkdownParser(flavour) }
    val referenceLinkHandler = remember { ReferenceLinkHandlerImpl() }
    val markdownState = rememberMarkdownState(
        content = markdownBody.orEmpty(),
        flavour = flavour,
        parser = parser,
        referenceLinkHandler = referenceLinkHandler,
    )
    val markdownRender by markdownState.state.collectAsState()
    val parsedSuccess = markdownRender as? MarkdownParseState.Success
    val parsedNow = markdownBody != null && parsedSuccess?.content == markdownBody
    SideEffect {
        if (parsedNow) articleReady = true
    }
    val showArticle = articleReady || parsedNow
    val ttsMiniPlayerVisible by remember {
        TtsPlayback.session.map { it?.url?.isNotBlank() == true }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(false)
    val headingChrome = if (outlineItems.isNotEmpty()) 14.dp else 0.dp
    val bottomChromePadding =
        (if (ttsMiniPlayerVisible) 104.dp else 48.dp) + headingChrome
    SelectionBackHandler(selection)
    fun closeFindPane() {
        findQuery = ""
        findIndex = 0
        onFindOpenChange(false)
    }
    fun openHighlightsPane() {
        onOutlineOpenChange(false)
        if (findOpen) closeFindPane()
        onHighlightsOpenChange(true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { scrollViewport = it },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewportHeight = it.height }
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        if (topScrollInsetPx > 0) {
            Spacer(Modifier.height(with(LocalDensity.current) { topScrollInsetPx.toDp() }))
        }
        val coverUrl = content.imageUrl?.takeIf { it.isNotBlank() }
        val overlaySummary = content.summary?.takeIf { it.isNotBlank() && it.length <= 150 }
        val belowSummary = content.summary?.takeIf { it.isNotBlank() && it.length > 150 }
        if (coverUrl != null) {
            ArticleHero(
                imageUrl = coverUrl,
                title = content.title,
                summary = overlaySummary,
                selection = selection,
                ttsStartIndex = titleTtsIndex,
                onClick = {
                    onOpenGallery(ArticleImages.urlsFor(content), 0)
                },
            )
        }
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = bottomChromePadding),
        ) {
            if (coverUrl == null && !content.title.isNullOrBlank()) {
                HighlightedArticleTitle(
                    title = content.title,
                    painted = painted,
                    spoken = spokenMark,
                    style = headingFamily,
                    color = colors.onBackground,
                    mineColor = mineColor,
                    friendsColor = friendsColor,
                    foafColor = foafColor,
                    otherColor = otherColor,
                    underline = underline,
                    selection = selection,
                    navigator = navigator,
                    ttsStartIndex = titleTtsIndex,
                    onHighlightTap = openFromStop,
                )
            }
            if (coverUrl == null && !content.summary.isNullOrBlank()) {
                Text(
                    text = content.summary,
                    style = typography.titleMedium.copy(
                        fontFamily = family,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = colors.onBackground.copy(alpha = 0.75f),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            if (belowSummary != null) {
                Text(
                    text = belowSummary,
                    style = typography.titleMedium.copy(
                        fontFamily = family,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = colors.onBackground.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
            }
                val authorPubkey = content.authorPubkey?.trim()?.takeIf { it.length == 64 }
                ArticleMetaRow(
                    authorName = authorPubkey?.let { Profile.displayName(it, author) },
                    authorPicture = author?.picture,
                    onAuthorClick = authorPubkey?.let { hex -> { onOpenProfile(hex) } },
                    domain = domain,
                    readingTime = readingTime,
                    nsfwWarning = remember(content.url, content.title, content.tags) {
                        SensitiveContent.classify(content)
                    },
                    highlightsLabel = highlightsLabel,
                    highlightsColor = highlightPillColor(highlights, mineColor, friendsColor, foafColor, otherColor),
                    published = published,
                    rssFeedUrl = rssFeedSuggestion,
                    onDomainClick = rootUrl?.let { root -> { defaultUriHandler.openUri(root) } },
                    onRssClick = rssFeedSuggestion?.let { feed -> { onAddRssFeed(feed) } },
                    onHighlightsClick = {
                        openHighlightsPane()
                    },
                )
                CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                    if (!showArticle) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp, bottom = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Markdown(
                            markdownState = markdownState,
                            colors = markdownColor(
                                text = colors.onBackground,
                                codeBackground = colors.surfaceVariant,
                                inlineCodeBackground = colors.surfaceVariant,
                                dividerColor = colors.outline,
                                tableBackground = colors.surfaceVariant.copy(alpha = 0.4f),
                            ),
                            typography = markdownTypography(
                                h1 = headingFamily,
                                h2 = typography.headlineMedium.copy(fontFamily = family),
                                h3 = typography.headlineSmall.copy(fontFamily = family),
                                h4 = typography.titleLarge.copy(fontFamily = family),
                                h5 = typography.titleLarge.copy(fontFamily = family, fontSize = 18.sp),
                                h6 = typography.titleLarge.copy(fontFamily = family, fontSize = 16.sp),
                                text = body,
                                paragraph = body,
                                quote = body.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                ordered = body,
                                bullet = body,
                                list = body,
                                code = typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, textAlign = TextAlign.Left),
                                inlineCode = body.copy(fontFamily = FontFamily.Monospace),
                                table = typography.bodyMedium.copy(fontFamily = family, textAlign = TextAlign.Left),
                                textLink = TextLinkStyles(
                                    style = body.copy(color = linkColor).toSpanStyle(),
                                ),
                            ),
                            padding = markdownPadding(
                                block = 12.dp,
                                list = 8.dp,
                                listItemTop = 4.dp,
                                listItemBottom = 4.dp,
                                codeBlock = PaddingValues(16.dp),
                                blockQuote = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            ),
                            imageTransformer = imageTransformer,
                            components = highlightedComponents,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                val showAuthorFooter = showArticle && showNostrAuthorFooterCard(content)
                if (showAuthorFooter && authorPubkey != null) {
                    AuthorCard(
                        displayName = Profile.displayName(authorPubkey, author),
                        about = author?.about,
                        pictureUrl = author?.picture,
                        onClick = { onOpenProfile(authorPubkey) },
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
                // No archive controls while the body is still parsing (issue #78).
                if (loggedIn && showArticle) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ArchiveButton(
                            archived = archived,
                            closeAfterArchive = settings.archiveClosesReader,
                            onClick = { onArchive(settings.archiveClosesReader && !archived) },
                        )
                    }
                }
            }
        }
        jumpBackFraction?.let { fraction ->
            JumpBackPill(
                percent = (fraction * 100f).roundToInt(),
                onClick = {
                    readingTracker.onPositionSet()
                    drifting = false
                    scope.launch {
                        val max = scrollState.maxValue
                        if (max > 0) scrollState.animateScrollTo((fraction * max).roundToInt())
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomChromePadding + 16.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background),
        ) {
            TtsMiniPlayerHost(
                currentArticleUrl = content.url,
                onOpenArticle = onOpenArticle,
                showCurrentArticle = true,
            )
            // Until the body is rendered the scroll range is meaningless, so
            // the bar shows the saved position instead (issue #131).
            ArticleScrollProgress(
                scrollState = scrollState,
                driftFraction = if (showArticle) driftFraction else savedFraction,
                scrollLive = showArticle,
                outlineItems = outlineItems,
                activeOutlineId = activeOutlineId,
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars),
            )
        }
        HighlightTextToolbar(
            selection = selection,
            showHighlight = loggedIn,
            onCopy = {
                clipboard.setText(AnnotatedString(selection.selectedText))
                selection.clear()
            },
            onHighlight = {
                val quote = selection.selectedText
                val ownerText = selection.text
                val ownerOffset = selection.range.min
                selection.clear()
                onHighlight(quote, ownerText, ownerOffset)
            },
            onTtsFromHere = ::startTtsFromSelection,
            onSetProgress = {
                val max = scrollState.maxValue
                val viewport = scrollViewport
                if (max > 0) {
                    // Selection rect is in window coordinates; map its top into
                    // the scroll viewport to get the content offset.
                    val localY = viewport?.takeIf { it.isAttached }
                        ?.windowToLocal(selection.toolbarRect.topLeft)?.y ?: 0f
                    val offset = (scrollState.value + localY).roundToInt().coerceIn(0, max)
                    ReadingPositionStore.save(content.url, ReadingProgress.fraction(offset, max))
                    readingTracker.onPositionSet()
                    drifting = false
                }
                selection.clear()
            },
            onSelectAll = {
                val owner = selection.owner ?: return@HighlightTextToolbar
                selection.selectAll(owner, selection.text, selection.ttsStartIndex)
            },
        )
        HighlightsPane(
            open = highlightsOpen,
            highlights = highlights,
            selectedId = selectedId,
            loggedIn = loggedIn,
            settings = settings,
            mineColor = mineColor,
            friendsColor = friendsColor,
            foafColor = foafColor,
            otherColor = otherColor,
            onDismiss = { onHighlightsOpenChange(false) },
            onSelect = { item ->
                selectedId = item.id
                pendingJumpId = item.id
                onHighlightsOpenChange(false)
            },
            onOpenHighlightSettings = {
                onHighlightsOpenChange(false)
                onOpenHighlightSettings()
            },
            onToggleMarks = {
                SettingsSync.apply(settings.withBoolean("showHighlights", !settings.showHighlights))
            },
            articleUrl = content.url,
            articleTexts = listOfNotNull(
                content.title?.takeIf { it.isNotBlank() },
                content.body,
            ),
            topPadding = paneTopPadding,
            menuFor = { item ->
                HighlightCardMenu(
                    highlightId = item.id,
                    authorHex = item.pubkey.ifBlank { null },
                    onGoToQuote = {
                        selectedId = item.id
                        pendingJumpId = item.id
                        onHighlightsOpenChange(false)
                    },
                    onViewProfile = item.pubkey.takeIf { it.isNotBlank() }?.let { hex ->
                        {
                            onHighlightsOpenChange(false)
                            onOpenProfile(hex)
                        }
                    },
                    onDelete = if (canDeleteHighlight(item.pubkey)) {
                        { onDeleteHighlight(item.id) }
                    } else {
                        null
                    },
                )
            },
        )
        FindPane(
            open = findOpen,
            query = findQuery,
            hits = findHits,
            activeIndex = findIndex,
            matchCount = findHits.size,
            topPadding = paneTopPadding,
            onQueryChange = { next ->
                findQuery = next
                findIndex = 0
                findJump++
            },
            onDismiss = {
                closeFindPane()
            },
            onPrevious = { stepFind(-1) },
            onNext = { stepFind(1) },
            onSelect = { index ->
                goFind(index)
                onFindOpenChange(false)
            },
        )
        LaunchedEffect(outlineItems, topScrollInsetPx) {
            if (outlineItems.isEmpty()) {
                activeOutlineId.value = null
                return@LaunchedEffect
            }
            snapshotFlow {
                scrollState.value
                navigator.stops
                val viewport = scrollViewport
                val pad = HighlightJump.chromePadding(jumpChromePx(), with(density) { 48.dp.toPx() })
                ArticleOutline.activeId(outlineItems, { id ->
                    val stop = navigator.firstStop(id) ?: return@activeId null
                    val coords = navigator.coordinates(stop.owner) ?: return@activeId null
                    if (viewport == null || !coords.isAttached || !viewport.isAttached) return@activeId null
                    viewport.localPositionOf(coords, Offset(0f, stop.localTop)).y
                }, pad)
            }.distinctUntilChanged().collect { activeOutlineId.value = it }
        }
        OutlinePaneHost(
            open = outlineOpen,
            items = outlineItems,
            activeOutlineId = activeOutlineId,
            topPadding = paneTopPadding,
            onDismiss = { onOutlineOpenChange(false) },
            onSelect = { item ->
                pendingJumpId = item.id
                selectedId = item.id
                onOutlineOpenChange(false)
            },
        )
    }
}

internal fun showNostrAuthorFooterCard(content: ReadableContent): Boolean {
    val authorPubkey = content.authorPubkey
        ?.trim()
        ?.lowercase()
        ?.takeIf { nostrPubkeyRegex.matches(it) }
        ?: return false
    val article = content.articleCoordinate
        ?.trim()
        ?.let { NostrArticle.fromCoordinate(it) }
    if (article != null) {
        return article.pointer.pubkey.lowercase() == authorPubkey
    }
    return content.url.startsWith("http")
}

private val nostrPubkeyRegex = Regex("[0-9a-f]{64}")

@Stable
private class SpokenMarkState {
    var sentence by mutableStateOf<String?>(null)
    var paragraph by mutableStateOf<String?>(null)
    var paragraphIndex by mutableStateOf<Int?>(null)
}

@Composable
private fun rememberHighlightMarks(
    displayed: String,
    painted: List<PaintedHighlight>,
    spoken: SpokenMarkState,
    ttsStartIndex: Int?,
): List<HighlightSpan> {
    val base = remember(displayed, painted) {
        matchHighlightSpans(displayed, painted)
    }
    // Derived, not remembered on spoken.* keys: reading those states directly
    // here subscribed every node in the article, so each TTS sentence tick
    // recomposed the whole tree. The derived value only changes for the
    // paragraph being spoken; all other nodes keep emptyList and stay still.
    val spokenSpans by remember(displayed, ttsStartIndex) {
        derivedStateOf(structuralEqualityPolicy()) {
            matchSpokenSpansForParagraph(
                displayed = displayed,
                sentence = spoken.sentence,
                paragraph = spoken.paragraph,
                displayedTtsIndex = ttsStartIndex,
                spokenTtsIndex = spoken.paragraphIndex,
            )
        }
    }
    return if (spokenSpans.isEmpty()) base else base + spokenSpans
}

@Composable
private fun TtsReaderError(
    articleUrl: String?,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val error by remember(articleUrl) {
        TtsPlayback.session
            .map { session -> session?.takeIf { it.url == articleUrl }?.errorMessage }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(null)
    LaunchedEffect(error) {
        val key = error ?: return@LaunchedEffect
        val text = context.getString(
            if (key == TtsPlayback.ERROR_LANGUAGE) {
                R.string.tts_error_language
            } else {
                R.string.tts_error_engine
            },
        )
        val result = snackbarHostState.showSnackbar(
            message = text,
            actionLabel = context.getString(R.string.tts_open_settings),
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) openTtsSettings(context)
    }
}

@Composable
private fun TtsSpokenSync(
    url: String,
    spoken: SpokenMarkState,
    scrollState: ScrollState,
    navigator: HighlightNavigator,
    scrollViewport: LayoutCoordinates?,
    positionRestored: Boolean,
    settingsFollowAlong: Boolean,
    jumpChromePx: () -> Int = { 0 },
) {
    val density = LocalDensity.current
    val session by TtsPlayback.session.collectAsStateWithLifecycle()
    val spokenSession = session?.takeIf {
        it.url == url && settingsFollowAlong && it.followAlongEnabled
    }
    val spokenParagraph = spokenSession?.paragraphs?.getOrNull(spokenSession.index)
    val spokenSentence = spokenSession?.let { current ->
        current.spokenText ?: spokenParagraph?.let { paragraph ->
            TtsText.sentences(paragraph).getOrNull(current.sentenceIndex) ?: paragraph
        }
    }
    spoken.sentence = spokenSentence
    spoken.paragraph = spokenParagraph
    spoken.paragraphIndex = spokenSession?.index
    var followAlongScrolling by remember { mutableStateOf(false) }
    val followAlongPosition = spokenSession
        ?.takeIf { it.playing && !it.followAlongPaused }
        ?.let { it.index to it.sentenceIndex }
    LaunchedEffect(followAlongPosition, url) {
        if (followAlongPosition == null) return@LaunchedEffect
        val stop = HighlightJump.awaitStop(navigator, ArticleFind.SPOKEN_ID) {
            scrollViewport?.isAttached == true
        } ?: return@LaunchedEffect
        val coords = navigator.coordinates(stop.owner) ?: return@LaunchedEffect
        val viewport = scrollViewport ?: return@LaunchedEffect
        if (!coords.isAttached || !viewport.isAttached) return@LaunchedEffect
        val y = viewport.localPositionOf(coords, Offset(0f, stop.localTop)).y
        val pad = HighlightJump.chromePadding(jumpChromePx(), with(density) { 48.dp.toPx() })
        val target = HighlightJump.scrollTarget(scrollState.value, scrollState.maxValue, y, pad)
        if (target != scrollState.value) {
            followAlongScrolling = true
            try {
                scrollState.animateScrollTo(target)
                snapshotFlow { scrollState.isScrollInProgress }.first { !it }
            } finally {
                followAlongScrolling = false
            }
        }
    }
    LaunchedEffect(url) {
        snapshotFlow { scrollState.isScrollInProgress }.collect { inProgress ->
            if (!inProgress || followAlongScrolling || !positionRestored) return@collect
            val current = TtsPlayback.session.value
            if (current != null && current.url == url &&
                settingsFollowAlong && current.followAlongEnabled &&
                current.playing && !current.followAlongPaused
            ) {
                TtsPlayback.setFollowAlongPaused(true)
            }
        }
    }
}

@Composable
private fun HighlightedArticleTitle(
    title: String,
    painted: List<PaintedHighlight>,
    spoken: SpokenMarkState,
    style: TextStyle,
    color: Color,
    mineColor: Color,
    friendsColor: Color,
    foafColor: Color,
    otherColor: Color,
    underline: Boolean,
    selection: ReaderSelectionState,
    navigator: HighlightNavigator,
    ttsStartIndex: Int?,
    onHighlightTap: (HighlightStop) -> Unit,
) {
    var titleLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var titleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val titleOwner = remember { Any() }
    val titleSpans = rememberHighlightMarks(title, painted, spoken, ttsStartIndex)
    Text(
        text = title,
        style = style,
        color = color,
        onTextLayout = { titleLayout = it },
        modifier = Modifier
            .padding(top = 8.dp, bottom = 12.dp)
            .drawHighlightMarks(
                titleLayout,
                titleSpans,
                mineColor,
                friendsColor,
                otherColor,
                underline,
                foafColor = foafColor,
            )
            .highlightAnchors(
                owner = titleOwner,
                spans = titleSpans,
                layout = titleLayout,
                coordinates = titleCoords,
                navigator = navigator,
            )
            .readerSelectable(
                owner = titleOwner,
                text = title,
                layout = titleLayout,
                coordinates = titleCoords,
                state = selection,
                onCoordinates = { titleCoords = it },
                ttsStartIndex = ttsStartIndex,
                onTap = { offset ->
                    val laid = titleLayout ?: return@readerSelectable false
                    val stop = navigator.hit(titleOwner, laid, offset) ?: return@readerSelectable false
                    onHighlightTap(stop)
                    true
                },
            ),
    )
}

@Composable
private fun HighlightedMarkdownNode(
    model: MarkdownComponentModel,
    style: TextStyle,
    highlights: List<PaintedHighlight>,
    mineColor: Color,
    friendsColor: Color,
    foafColor: Color,
    otherColor: Color,
    underline: Boolean,
    selection: ReaderSelectionState,
    navigator: HighlightNavigator,
    onHighlightTap: (HighlightStop) -> Unit,
    spoken: SpokenMarkState,
    ttsStartIndex: Int?,
    outlineItems: List<ArticleOutlineItem> = emptyList(),
    outlineStartOffset: Int? = null,
    contentChildType: IElementType? = null,
    isHeading: Boolean = false,
) {
    val annotator = annotatorSettings()
    val textNode = remember(model.node, contentChildType) {
        contentChildType?.let { model.node.findChildOfType(it) } ?: model.node
    }
    val styledText = remember(textNode, style) {
        model.content.buildMarkdownAnnotatedString(
            textNode,
            style,
            annotator,
        )
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val owner = remember { Any() }
    // NIP-84/find match once per (text, highlights). Spoken rematches alone.
    val marks = rememberHighlightMarks(styledText.text, highlights, spoken, ttsStartIndex)
    val outlineId = remember(outlineItems, outlineStartOffset, styledText.text) {
        outlineStartOffset?.let { ArticleOutline.idForHeading(outlineItems, it, styledText.text) }
    }
    val spans = remember(marks, outlineId, styledText.text) {
        val extra = outlineId?.let { ArticleOutline.painted(it, styledText.text) }?.let { item ->
            HighlightSpan(item, 0, styledText.text.length)
        }
        if (extra == null) marks else marks + extra
    }
    val textModifier = Modifier
        .fillMaxWidth()
        .then(if (isHeading) Modifier.semantics { heading() } else Modifier)
    MarkdownText(
        content = styledText,
        style = style,
        modifier = textModifier
            .drawHighlightMarks(
                layout,
                spans,
                mineColor,
                friendsColor,
                otherColor,
                underline,
                foafColor = foafColor,
            )
            .highlightAnchors(
                owner = owner,
                spans = spans,
                layout = layout,
                coordinates = coords,
                navigator = navigator,
            )
            .readerSelectable(
                owner = owner,
                text = styledText.text,
                layout = layout,
                coordinates = coords,
                state = selection,
                onCoordinates = { coords = it },
                ttsStartIndex = ttsStartIndex,
                onTap = { offset ->
                    val laid = layout ?: return@readerSelectable false
                    val stop = navigator.hit(owner, laid, offset) ?: return@readerSelectable false
                    onHighlightTap(stop)
                    true
                },
            ),
        onTextLayout = { result, _ -> layout = result },
    )
}

@Composable
private fun ArticleScrollProgress(
    scrollState: ScrollState,
    driftFraction: Float?,
    scrollLive: Boolean = true,
    outlineItems: List<ArticleOutlineItem> = emptyList(),
    activeOutlineId: MutableState<String?>? = null,
) {
    val heading = activeOutlineId?.value?.let { id ->
        outlineItems.firstOrNull { it.id == id }?.title
    }
    val scrollPercent = ReadingProgress.percent(scrollState.value, scrollState.maxValue)
    if (driftFraction != null) {
        // Drifted: the fill keeps the saved reading position, the dot marks
        // where the viewport currently is.
        ReadingProgressBar(
            percent = (driftFraction * 100f).roundToInt(),
            scrollPercent = scrollPercent.takeIf { scrollLive },
            heading = heading,
        )
    } else {
        ReadingProgressBar(
            percent = if (scrollLive) scrollPercent else 0,
            heading = heading,
        )
    }
}

@Composable
private fun OutlinePaneHost(
    open: Boolean,
    items: List<ArticleOutlineItem>,
    activeOutlineId: MutableState<String?>,
    onDismiss: () -> Unit,
    onSelect: (ArticleOutlineItem) -> Unit,
    topPadding: () -> Dp,
) {
    OutlinePane(
        open = open,
        items = items,
        activeId = activeOutlineId.value,
        onDismiss = onDismiss,
        onSelect = onSelect,
        topPadding = topPadding,
    )
}

@Composable
private fun JumpBackPill(
    percent: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.reader_jump_back, percent),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SensitiveContentGate(
    warning: SensitiveContent.Warning,
    onShow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.nsfw_reader_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (warning.confirmed) R.string.nsfw_reader_body else R.string.nsfw_reader_body_maybe,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        warning.reason?.let { reason ->
            Text(
                text = stringResource(R.string.nsfw_reader_reason, reason),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Button(
            onClick = onShow,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(stringResource(R.string.nsfw_reader_show))
        }
    }
}

@Composable
private fun ArchiveButton(
    archived: Boolean,
    closeAfterArchive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = if (archived) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = ArchiveGreen.copy(alpha = 0.14f),
                contentColor = ArchiveGreen,
            )
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
        border = BorderStroke(
            1.dp,
            if (archived) ArchiveGreen else MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (archived) Icons.Filled.CheckCircle else BorisIcons.Books,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(
                when {
                    archived -> R.string.reader_archived
                    closeAfterArchive -> R.string.reader_archive_close
                    else -> R.string.reader_archive
                },
            ),
        )
    }
}

private fun isArchiveFailureMessage(context: Context, message: String): Boolean =
    message == context.getString(R.string.reader_archive_cancelled) ||
        message == context.getString(R.string.reader_archive_rejected) ||
        message == context.getString(R.string.reader_archive_failed)

internal fun readingTimeLabel(text: String): String? = ReadingTime.labelFor(text)

internal fun highlightCountLabel(count: Int): String? {
    if (count <= 0) return null
    return if (count == 1) "1 highlight" else "$count highlights"
}

internal fun highlightPillColor(
    highlights: List<PaintedHighlight>,
    mine: Color,
    friends: Color,
    foaf: Color,
    other: Color,
): Color = when {
    highlights.any { it.mine } -> mine
    highlights.any { it.friend } -> friends
    highlights.any { it.foaf } -> foaf
    else -> other
}

@Composable
private fun ArticleHero(
    imageUrl: String,
    title: String?,
    summary: String?,
    selection: ReaderSelectionState? = null,
    ttsStartIndex: Int? = null,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val height = (screenHeight * 0.42f).dp.coerceIn(240.dp, 420.dp)
    val widthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val heightPx = with(density) { height.roundToPx() }
    val request = remember(imageUrl, widthPx, heightPx) {
        articleImageRequest(context, imageUrl, widthPx, heightPx)
    }
    var titleLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var titleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val titleOwner = remember { Any() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = request,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.82f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            if (!title.isNullOrBlank()) {
                val selectableModifier = if (selection == null) {
                    Modifier
                } else {
                    Modifier.readerSelectable(
                        owner = titleOwner,
                        text = title,
                        layout = titleLayout,
                        coordinates = titleCoords,
                        state = selection,
                        onCoordinates = { titleCoords = it },
                        ttsStartIndex = ttsStartIndex,
                        onTap = {
                            onClick()
                            true
                        },
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp,
                    ),
                    color = Color.White,
                    onTextLayout = { titleLayout = it },
                    modifier = Modifier.padding(
                        bottom = if (summary.isNullOrBlank()) 0.dp else 8.dp,
                    ).then(selectableModifier),
                )
            }
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp,
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArticleMetaRow(
    authorName: String?,
    authorPicture: String?,
    onAuthorClick: (() -> Unit)?,
    domain: String?,
    readingTime: String?,
    nsfwWarning: SensitiveContent.Warning? = null,
    highlightsLabel: String?,
    highlightsColor: Color,
    published: String?,
    rssFeedUrl: String?,
    onDomainClick: (() -> Unit)? = null,
    onRssClick: (() -> Unit)? = null,
    onHighlightsClick: (() -> Unit)? = null,
) {
    if (
        authorName == null &&
        domain == null &&
        readingTime == null &&
        nsfwWarning == null &&
        highlightsLabel == null &&
        rssFeedUrl == null &&
        published == null
    ) {
        return
    }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (authorName != null) {
            AuthorMetaChip(
                name = authorName,
                pictureUrl = authorPicture,
                onClick = onAuthorClick,
            )
        }
        if (domain != null) {
            MetaChip(text = domain, icon = Icons.Outlined.Language, onClick = onDomainClick)
        }
        if (rssFeedUrl != null) {
            MetaChip(
                text = stringResource(R.string.reader_add_rss),
                icon = Icons.Outlined.RssFeed,
                accent = MaterialTheme.colorScheme.primary,
                onClick = onRssClick,
            )
        }
        if (readingTime != null) {
            MetaChip(text = readingTime, icon = Icons.Outlined.Schedule)
        }
        if (nsfwWarning != null) {
            NsfwBadge(nsfwWarning)
        }
        if (highlightsLabel != null) {
            MetaChip(
                text = highlightsLabel,
                icon = BorisIcons.Highlighter,
                accent = highlightsColor,
                onClick = onHighlightsClick,
            )
        }
        if (published != null) {
            MetaChip(text = published, icon = Icons.Outlined.CalendarMonth)
        }
    }
}

@Composable
private fun AuthorMetaChip(
    name: String,
    pictureUrl: String?,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val fg = MaterialTheme.colorScheme.onSurfaceVariant
    val fallback = rememberVectorPainter(Icons.Outlined.AccountCircle)
    Row(
        modifier = Modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AsyncImage(
            model = pictureUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = fallback,
            error = fallback,
            fallback = fallback,
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetaChip(
    text: String,
    icon: ImageVector,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val border = accent?.copy(alpha = 0.55f) ?: MaterialTheme.colorScheme.outline
    val fg = if (accent != null) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconTint = accent ?: fg
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .border(1.dp, border, shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EventRefCard(
    ref: NostrEventRef,
    resolved: ResolvedEventRef,
    onOpen: (String) -> Unit,
) {
    val event = resolved.event
    val note = event.kind == Nip01Event.KIND_TEXT_NOTE
    ArticleRow(
        title = NostrEventRefs.cardTitle(resolved),
        summary = if (note) null else Nip23.summary(event),
        imageUrl = if (note) NoteCover.image(event) else Nip23.image(event) ?: NoteCover.image(event),
        imageFallbackIcon = if (note) {
            Icons.AutoMirrored.Outlined.StickyNote2
        } else {
            Icons.AutoMirrored.Outlined.Article
        },
        byline = Profile.displayName(event.pubkey, resolved.profile),
        bylinePicture = resolved.profile?.picture,
        bylineFallbackIcon = Icons.Outlined.AccountCircle,
        publishedAt = if (note) event.createdAt else Nip23.publishedAt(event),
        url = ref.uri,
        showReadingProgress = !note,
        onClick = { onOpen(ref.uri) },
    )
}

private class ClickableCoilImageTransformer(
    private val fullWidth: Boolean,
    private val maxHeight: Dp,
    private val onImageClick: (String) -> Unit,
) : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData {
        val https = UrlExtractor.preferHttps(link)
        val data = Coil3ImageTransformerImpl.transform(https)
        return data.copy(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .clipToBounds()
                .clip(RoundedCornerShape(6.dp))
                .clickable { onImageClick(https) },
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
        )
    }

    override fun placeholderConfig(
        density: Density,
        containerSize: Size,
        intrinsicImageSize: Size,
    ): PlaceholderConfig {
        val box = markdownImageBox(
            container = containerSize,
            intrinsic = intrinsicImageSize,
            fullWidth = fullWidth,
            maxHeightPx = with(density) { maxHeight.toPx() },
        )
        return PlaceholderConfig(
            size = with(density) { Size(box.width.toSp().value, box.height.toSp().value) },
            animate = false,
        )
    }

    @Composable
    override fun intrinsicSize(painter: Painter): Size =
        Coil3ImageTransformerImpl.intrinsicSize(painter)
}
