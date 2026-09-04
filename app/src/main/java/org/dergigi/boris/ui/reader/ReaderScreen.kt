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
import org.dergigi.boris.nostr.ArticleReaction
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
    val reaction by viewModel.reaction.collectAsStateWithLifecycle()
    val canReact by viewModel.canReact.collectAsStateWithLifecycle()
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
        reaction = reaction,
        canReact = canReact,
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
        onReact = { reaction -> viewModel.react(reaction)?.let(launchSign) },
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
    reaction: ArticleReaction?,
    canReact: Boolean,
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
    onReact: (ArticleReaction?) -> Unit,
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

    val pane = remember { ReaderPaneState() }
    val readyBody = (state as? ReaderUiState.Ready)?.content?.body
    var outlineItems by remember(readyBody) {
        mutableStateOf(readyBody?.let { ArticleOutline.parse(it) }.orEmpty())
    }
    LaunchedEffect(articleUrl) {
        pane.onArticleChanged()
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
        ReaderTopBar(
            modifier = barModifier,
            state = state,
            articleUrl = articleUrl,
            galleryUrls = galleryUrls,
            outlineItems = outlineItems,
            pane = pane,
            loggedIn = loggedIn,
            highlights = highlights,
            highlightCount = highlightCount,
            settings = settings,
            inLibrary = inLibrary,
            archived = archived,
            canSave = canSave,
            author = author,
            canOpenArchive = canOpenArchive,
            scrollState = articleScrollState,
            snackbarHostState = snackbarHostState,
            onBack = onBack,
            onSave = onSave,
            onArchive = onArchive,
            onRefresh = onRefresh,
            onOpenGallery = onOpenGallery,
            onOpenReaderSettings = onOpenReaderSettings,
            onShare = ::shareArticle,
            onOpenOriginal = ::openOriginal,
            onOpenNative = ::openNative,
            onOpenWayback = ::openWayback,
            onOpenArchivePh = ::openArchivePh,
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
                ReaderLoadingPane(state = state, modifier = pinnedPad)
            }
            is ReaderUiState.Error -> {
                ReaderErrorPane(
                    state = state,
                    onRetry = onRetry,
                    onOpenOriginal = ::openOriginal,
                    onOpenWayback = ::openWayback,
                    onOpenArchivePh = ::openArchivePh,
                    modifier = pinnedPad,
                )
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
                    reaction = reaction,
                    canReact = canReact,
                    author = author,
                    eventRefs = eventRefs,
                    settings = settings,
                    rssFeedSuggestion = rssFeedSuggestion,
                    // D-19: while TTS is speaking, volume keys change volume, not scroll.
                    volumeScroll = gallery == null,
                    pane = pane,
                    outlineItems = outlineItems,
                    onOutlineItems = { outlineItems = it },
                    onOpenArticle = onOpenArticle,
                    onOpenProfile = onOpenProfile,
                    onOpenBrowser = onOpenBrowser,
                    onAddRssFeed = { feed -> rssConfirmFeed = feed },
                    onOpenHighlightSettings = onOpenHighlightSettings,
                    onOpenGallery = onOpenGallery,
                    onHighlight = onHighlight,
                    onArchive = onArchive,
                    onReact = onReact,
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
