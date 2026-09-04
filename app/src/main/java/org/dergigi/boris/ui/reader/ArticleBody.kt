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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import org.dergigi.boris.ui.copyPlainLink
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
internal fun ArticleBody(
    content: ReadableContent,
    highlights: List<PaintedHighlight>,
    highlightCount: Int,
    highlightsLoaded: Boolean,
    focusHighlightId: String,
    loggedIn: Boolean,
    archived: Boolean,
    reaction: ArticleReaction?,
    canReact: Boolean,
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
    onReact: (ArticleReaction?) -> Unit,
    canDeleteHighlight: (String?) -> Boolean = { false },
    onDeleteHighlight: (String) -> Unit = {},
    pane: ReaderPaneState,
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
    var linkMenu by remember(content.url) { mutableStateOf<ReaderLinkMenuState?>(null) }
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
    LaunchedEffect(pane.highlightsOpen) {
        if (!pane.highlightsOpen) return@LaunchedEffect
        pane.closeOutline()
        if (pane.findOpen) {
            findQuery = ""
            findIndex = 0
            pane.closeFind()
        }
    }
    LaunchedEffect(pane.findOpen) {
        if (pane.findOpen) {
            pane.closeHighlights()
            pane.closeOutline()
        }
    }
    LaunchedEffect(pane.outlineOpen) {
        if (pane.outlineOpen) {
            pane.closeHighlights()
            if (pane.findOpen) {
                findQuery = ""
                findIndex = 0
                pane.closeFind()
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
    val progress = ReadingProgressEffects(
        url = content.url,
        focusHighlightId = focusHighlightId,
        scrollState = scrollState,
        viewportHeight = viewportHeight,
        settings = settings,
        loggedIn = loggedIn,
        archived = archived,
        onArchive = onArchive,
    )
    val positionRestored = progress.positionRestored
    val articleReady = progress.articleReady
    val readingTracker = progress.readingTracker
    val savedFraction = progress.savedFraction
    val driftFraction = progress.driftFraction
    val jumpBackFraction = progress.jumpBackFraction
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
    val openFromStop = remember<(HighlightStop) -> Unit> {
        { stop ->
            jumpState.value(navigator.select(stop))
            selectedId = stop.highlightId
            pane.closeFind()
            pane.openHighlights()
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
    val showLinkMenu = remember(content.url, selection) {
        { uri: String, position: Offset, coords: LayoutCoordinates ->
            val viewport = scrollViewport
            val rootOrigin = viewport?.takeIf { it.isAttached }?.localToWindow(Offset.Zero) ?: Offset.Zero
            val windowPosition = coords.localToWindow(position)
            linkMenu = ReaderLinkMenuState(
                uri = uri,
                target = readerLinkContextTarget(uri, content.url),
                offset = IntOffset(
                    x = (windowPosition.x - rootOrigin.x).toInt(),
                    y = (windowPosition.y - rootOrigin.y).toInt(),
                ),
            )
            selection.clear()
        }
    }
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
        showLinkMenu,
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
                    onLinkLongPress = showLinkMenu,
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
                            onLinkLongPress = showLinkMenu,
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
                    onLinkLongPress = showLinkMenu,
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
                    onLinkLongPress = showLinkMenu,
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
                    onLinkLongPress = showLinkMenu,
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
                    onLinkLongPress = showLinkMenu,
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
                    onLinkLongPress = showLinkMenu,
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
                    onLinkLongPress = showLinkMenu,
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
                    onLinkLongPress = showLinkMenu,
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
                    onLinkLongPress = showLinkMenu,
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
        if (parsedNow) progress.markArticleReady()
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
        pane.closeFind()
    }
    fun openHighlightsPane() {
        pane.closeOutline()
        if (pane.findOpen) closeFindPane()
        pane.openHighlights()
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArchiveButton(
                            archived = archived,
                            closeAfterArchive = settings.archiveClosesReader,
                            onClick = { onArchive(settings.archiveClosesReader && !archived) },
                        )
                        if (canReact) {
                            ReactionButton(reaction = reaction, onReact = onReact)
                        }
                    }
                }
            }
        }
        jumpBackFraction?.let { fraction ->
            JumpBackPill(
                percent = (fraction * 100f).roundToInt(),
                onClick = {
                    readingTracker.onPositionSet()
                    progress.clearDrift()
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
                    progress.clearDrift()
                }
                selection.clear()
            },
            onSelectAll = {
                val owner = selection.owner ?: return@HighlightTextToolbar
                selection.selectAll(owner, selection.text, selection.ttsStartIndex)
            },
        )
        HighlightsPane(
            open = pane.highlightsOpen,
            highlights = highlights,
            selectedId = selectedId,
            loggedIn = loggedIn,
            settings = settings,
            mineColor = mineColor,
            friendsColor = friendsColor,
            foafColor = foafColor,
            otherColor = otherColor,
            onDismiss = { pane.closeHighlights() },
            onSelect = { item ->
                selectedId = item.id
                pendingJumpId = item.id
                pane.closeHighlights()
            },
            onOpenHighlightSettings = {
                pane.closeHighlights()
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
                        pane.closeHighlights()
                    },
                    onViewProfile = item.pubkey.takeIf { it.isNotBlank() }?.let { hex ->
                        {
                            pane.closeHighlights()
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
            open = pane.findOpen,
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
                pane.closeFind()
            },
        )
        LinkContextMenu(
            state = linkMenu,
            onDismiss = { linkMenu = null },
            onOpen = { uriHandler.openUri(it) },
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
            open = pane.outlineOpen,
            items = outlineItems,
            activeOutlineId = activeOutlineId,
            topPadding = paneTopPadding,
            onDismiss = { pane.closeOutline() },
            onSelect = { item ->
                pendingJumpId = item.id
                selectedId = item.id
                pane.closeOutline()
            },
        )
    }
}

@Composable
internal fun rememberHighlightMarks(
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
internal fun HighlightedArticleTitle(
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
internal fun HighlightedMarkdownNode(
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
    onLinkLongPress: ((String, Offset, LayoutCoordinates) -> Unit)? = null,
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
                onLongPress = { position, coords ->
                    val laid = layout ?: return@readerSelectable false
                    val index = JustifiedLayout.offsetAt(laid, position)
                    val url = styledText.linkUrlAt(index) ?: return@readerSelectable false
                    onLinkLongPress?.invoke(url, position, coords)
                    onLinkLongPress != null
                },
            ),
        onTextLayout = { result, _ -> layout = result },
    )
}

private data class ReaderLinkMenuState(
    val uri: String,
    val target: String,
    val offset: IntOffset,
)

@Composable
private fun LinkContextMenu(
    state: ReaderLinkMenuState?,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
) {
    if (state == null) return
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Box(modifier = Modifier.offset { state.offset }) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_copy_link)) },
                leadingIcon = {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                },
                onClick = {
                    onDismiss()
                    copyPlainLink(context, clipboard, state.target)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_open_link)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Language, contentDescription = null)
                },
                onClick = {
                    onDismiss()
                    onOpen(state.uri)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_share_link)) },
                leadingIcon = {
                    Icon(Icons.Filled.Share, contentDescription = null)
                },
                onClick = {
                    onDismiss()
                    shareArticleLink(context, null, state.target)
                },
            )
        }
    }
}

private fun AnnotatedString.linkUrlAt(offset: Int): String? =
    getLinkAnnotations(offset, offset).firstOrNull()?.item?.let { link ->
        when (link) {
            is LinkAnnotation.Url -> link.url
            is LinkAnnotation.Clickable -> link.tag
            else -> null
        }
    }

@Composable
internal fun OutlinePaneHost(
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

internal class ClickableCoilImageTransformer(
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
