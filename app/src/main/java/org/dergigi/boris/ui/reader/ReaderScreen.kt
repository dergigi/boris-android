package org.dergigi.boris.ui.reader

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.mikepenz.markdown.model.markdownPadding
import com.mikepenz.markdown.model.rememberMarkdownState
import org.dergigi.boris.R
import org.dergigi.boris.data.ArticleUrl
import org.dergigi.boris.data.Footnotes
import org.dergigi.boris.data.HexColor
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.PublishedTime
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.ui.AuthorCard
import org.dergigi.boris.ui.settings.ReadingFonts
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther
import coil3.compose.AsyncImage
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: ReaderViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gallery by viewModel.gallery.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val highlightCount by viewModel.highlightCount.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()
    val archived by viewModel.archived.collectAsStateWithLifecycle()
    val author by viewModel.author.collectAsStateWithLifecycle()
    val signIntent by viewModel.signIntent.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onSignerResult(result.resultCode, result.data)
    }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }
    LaunchedEffect(signIntent) {
        val intent = signIntent ?: return@LaunchedEffect
        viewModel.consumeSignIntent()
        launcher.launch(intent)
    }
    ReaderScreenContent(
        state = state,
        gallery = gallery,
        highlights = highlights,
        highlightCount = highlightCount,
        loggedIn = loggedIn,
        canSave = canSave,
        archived = archived,
        author = author,
        settings = settings,
        onBack = onBack,
        onRetry = viewModel::load,
        onOpenArticle = onOpenArticle,
        onOpenProfile = onOpenProfile,
        onOpenGallery = viewModel::openGallery,
        onCloseGallery = viewModel::closeGallery,
        onGalleryPage = viewModel::setGalleryIndex,
        onHighlight = { quote ->
            viewModel.highlight(quote)?.let(launcher::launch)
        },
        onSave = {
            viewModel.saveToLibrary()?.let(launcher::launch)
        },
        onArchive = {
            viewModel.archive()?.let(launcher::launch)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreenContent(
    state: ReaderUiState,
    gallery: ImageGalleryState?,
    highlights: List<PaintedHighlight>,
    highlightCount: Int,
    loggedIn: Boolean,
    canSave: Boolean,
    archived: Boolean,
    author: Profile?,
    settings: UserSettings,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenGallery: (List<String>, Int) -> Unit,
    onCloseGallery: () -> Unit,
    onGalleryPage: (Int) -> Unit,
    onHighlight: (String) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val articleUrl = when (state) {
        is ReaderUiState.Ready -> state.content.url
        is ReaderUiState.Error -> state.url
        ReaderUiState.Loading -> null
    }

    fun openOriginal() {
        val url = articleUrl ?: return
        val target = NostrLink.parse(url)?.publicUrl ?: url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
        context.startActivity(intent)
    }

    fun shareArticle() {
        val url = articleUrl ?: return
        val shareUrl = NostrLink.parse(url)?.publicUrl ?: url
        val title = (state as? ReaderUiState.Ready)?.content?.title
        val text = if (title.isNullOrBlank()) shareUrl else "$title\n$shareUrl"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (!title.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
        }
        context.startActivity(Intent.createChooser(intent, "Share article"))
    }

    fun copyLink() {
        val url = articleUrl ?: return
        clipboard.setText(AnnotatedString(NostrLink.copyText(url)))
        Toast.makeText(context, "Copied.", Toast.LENGTH_SHORT).show()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (state as? ReaderUiState.Ready)?.content?.title
                    Text(
                        text = title.orEmpty(),
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
                    if (canSave) {
                        IconButton(onClick = onSave) {
                            Icon(
                                Icons.Outlined.AddCircle,
                                contentDescription = stringResource(R.string.reader_save_library),
                            )
                        }
                    }
                    if (articleUrl != null) {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Share, contentDescription = null)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        shareArticle()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy link") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        copyLink()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Open original") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        openOriginal()
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when (state) {
            ReaderUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is ReaderUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                    )
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
                            Text("Open original")
                        }
                    }
                }
            }
            is ReaderUiState.Ready -> {
                ArticleBody(
                    content = state.content,
                    highlights = highlights,
                    highlightCount = highlightCount,
                    loggedIn = loggedIn,
                    archived = archived,
                    author = author,
                    settings = settings,
                    volumeScroll = gallery == null,
                    onOpenArticle = onOpenArticle,
                    onOpenProfile = onOpenProfile,
                    onOpenGallery = onOpenGallery,
                    onHighlight = onHighlight,
                    onArchive = onArchive,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
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
private fun ArticleBody(
    content: ReadableContent,
    highlights: List<PaintedHighlight>,
    highlightCount: Int,
    loggedIn: Boolean,
    archived: Boolean,
    author: Profile?,
    settings: UserSettings,
    onOpenArticle: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenGallery: (List<String>, Int) -> Unit,
    onHighlight: (String) -> Unit,
    onArchive: () -> Unit,
    volumeScroll: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val readingTime = readingTimeLabel(content.body)
    val domain = ArticleUrl.host(content.url)
    val published = content.publishedAt?.let { PublishedTime.label(it) }
    val highlightsLabel = highlightCountLabel(highlightCount)
    val defaultUriHandler = LocalUriHandler.current
    val uriHandler = remember(content.url, onOpenArticle, defaultUriHandler) {
        object : UriHandler {
            override fun openUri(uri: String) {
                val article = UrlExtractor.articleUrl(uri, content.url)
                if (article != null && article != content.url) {
                    onOpenArticle(article)
                } else if (article == null) {
                    defaultUriHandler.openUri(uri)
                }
            }
        }
    }
    val imageTransformer = ClickableCoilImageTransformer(
        fullWidth = settings.fullWidthImages,
        maxHeight = (LocalConfiguration.current.screenHeightDp * 0.7f).dp,
    ) { link ->
        val opened = UrlExtractor.articleUrl(link, content.url) ?: return@ClickableCoilImageTransformer
        val urls = UrlExtractor.imageUrls(content.body, content.url).toMutableList()
        if (opened !in urls) urls.add(0, opened)
        onOpenGallery(urls, urls.indexOf(opened).coerceAtLeast(0))
    }
    val painted = highlights.visibleFor(settings)
    val family = ReadingFonts.family(settings.readingFont)
    val bodySize = settings.fontSize.sp
    val bodyLine = (settings.fontSize * 36f / 21f).sp
    val align = if (settings.justifyParagraphs) TextAlign.Justify else TextAlign.Start
    val mineColor = readingColor(settings.highlightColorMine, HighlightMine)
    val friendsColor = readingColor(settings.highlightColorFriends, HighlightFriends)
    val otherColor = readingColor(settings.highlightColorNostrverse, HighlightOther)
    val underline = !settings.markerStyle
    val dark = settings.isDark(isSystemInDarkTheme())
    val linkColor = readingColor(
        if (dark) settings.linkColorDark else settings.linkColorLight,
        colors.secondary,
    )
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
    val selection = remember { ReaderSelectionState() }
    val navigator = remember { HighlightNavigator() }
    val paintedHolder = remember { mutableStateOf(painted) }
    paintedHolder.value = painted
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var viewportHeight by remember { mutableIntStateOf(0) }
    var scrollViewport by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val jumpState = rememberUpdatedState<(HighlightStop) -> Unit> { stop ->
        val coords = navigator.coordinates(stop.owner) ?: return@rememberUpdatedState
        val viewport = scrollViewport ?: return@rememberUpdatedState
        if (!coords.isAttached || !viewport.isAttached) return@rememberUpdatedState
        val y = viewport.localPositionOf(coords, Offset(0f, stop.localTop)).y
        val pad = with(density) { 48.dp.toPx() }
        val target = HighlightJump.scrollTarget(scrollState.value, scrollState.maxValue, y, pad)
        if (target != scrollState.value) {
            scope.launch { scrollState.animateScrollTo(target) }
        }
    }
    var paneOpen by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val openFromStop = remember<(HighlightStop) -> Unit> {
        { stop ->
            jumpState.value(navigator.select(stop))
            selectedId = stop.highlightId
            paneOpen = true
        }
    }
    val highlightedComponents = remember(mineColor, friendsColor, otherColor, underline, selection, navigator, openFromStop) {
        markdownComponents(
            text = { HighlightedMarkdownNode(it, it.typography.text, paintedHolder.value, mineColor, friendsColor, otherColor, underline, selection, navigator, openFromStop) },
            paragraph = { HighlightedMarkdownNode(it, it.typography.paragraph, paintedHolder.value, mineColor, friendsColor, otherColor, underline, selection, navigator, openFromStop) },
            heading1 = { HighlightedMarkdownNode(it, it.typography.h1, paintedHolder.value, mineColor, friendsColor, otherColor, underline, selection, navigator, openFromStop) },
            heading2 = { HighlightedMarkdownNode(it, it.typography.h2, paintedHolder.value, mineColor, friendsColor, otherColor, underline, selection, navigator, openFromStop) },
            heading3 = { HighlightedMarkdownNode(it, it.typography.h3, paintedHolder.value, mineColor, friendsColor, otherColor, underline, selection, navigator, openFromStop) },
            heading4 = { HighlightedMarkdownNode(it, it.typography.h4, paintedHolder.value, mineColor, friendsColor, otherColor, underline, selection, navigator, openFromStop) },
            heading5 = { HighlightedMarkdownNode(it, it.typography.h5, paintedHolder.value, mineColor, friendsColor, otherColor, underline, selection, navigator, openFromStop) },
            heading6 = { HighlightedMarkdownNode(it, it.typography.h6, paintedHolder.value, mineColor, friendsColor, otherColor, underline, selection, navigator, openFromStop) },
        )
    }
    VolumeKeys.Handle(enabled = volumeScroll && settings.volumeButtonScroll) { up ->
        val page = VolumeKeys.pageSize(viewportHeight, settings.volumeButtonScrollPercent)
        val target = VolumeKeys.nextOffset(scrollState.value, scrollState.maxValue, page, up)
        if (target != scrollState.value) {
            scope.launch { scrollState.animateScrollTo(target) }
        }
        true
    }
    // The markdown parse state must survive recomposition. Fresh parser inputs would
    // re-parse asynchronously, collapse the article to an empty box, clamp the scroll
    // to zero, and remount every paragraph, killing the active selection.
    val flavour = remember { GFMFlavourDescriptor() }
    val parser = remember(flavour) { MarkdownParser(flavour) }
    val referenceLinkHandler = remember { ReferenceLinkHandlerImpl() }
    val markdownBody = remember(content.body) { Footnotes.expand(content.body) }
    val markdownState = rememberMarkdownState(
        content = markdownBody,
        flavour = flavour,
        parser = parser,
        referenceLinkHandler = referenceLinkHandler,
    )
    BackHandler(enabled = selection.hasSelection) { selection.clear() }

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
        val coverUrl = content.imageUrl?.takeIf { it.isNotBlank() }
        val overlaySummary = content.summary?.takeIf { it.isNotBlank() && it.length <= 150 }
        val belowSummary = content.summary?.takeIf { it.isNotBlank() && it.length > 150 }
        if (coverUrl != null) {
            ArticleHero(
                imageUrl = coverUrl,
                title = content.title,
                summary = overlaySummary,
                published = published,
                onClick = {
                    val urls = buildList {
                        add(coverUrl)
                        addAll(UrlExtractor.imageUrls(content.body, content.url))
                    }.distinct()
                    onOpenGallery(urls, 0)
                },
            )
        }
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 48.dp),
        ) {
            if (coverUrl == null && !content.title.isNullOrBlank()) {
                var titleLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                var titleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                val titleOwner = remember { Any() }
                Text(
                    text = content.title,
                    style = headingFamily,
                    color = colors.onBackground,
                    onTextLayout = { titleLayout = it },
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 12.dp)
                        .drawHighlightMarks(
                            titleLayout,
                            content.title,
                            painted,
                            mineColor,
                            friendsColor,
                            otherColor,
                            underline,
                        )
                        .highlightAnchors(
                            owner = titleOwner,
                            text = content.title,
                            layout = titleLayout,
                            coordinates = titleCoords,
                            highlights = painted,
                            navigator = navigator,
                        )
                        .readerSelectable(
                            owner = titleOwner,
                            text = content.title,
                            layout = titleLayout,
                            coordinates = titleCoords,
                            state = selection,
                            onCoordinates = { titleCoords = it },
                            onTap = { offset ->
                                val laid = titleLayout ?: return@readerSelectable false
                                val stop = navigator.hit(titleOwner, laid, offset) ?: return@readerSelectable false
                                openFromStop(stop)
                                true
                            },
                        ),
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
                ArticleMetaRow(
                    domain = domain,
                    readingTime = readingTime,
                    highlightsLabel = highlightsLabel,
                    published = if (coverUrl == null) published else null,
                    onHighlightsClick = { paneOpen = true },
                )
                CompositionLocalProvider(LocalUriHandler provides uriHandler) {
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
                            blockQuote = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        ),
                        imageTransformer = imageTransformer,
                        components = highlightedComponents,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (loggedIn) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ArchiveButton(
                            archived = archived,
                            onClick = onArchive,
                        )
                    }
                }
                val authorPubkey = content.authorPubkey?.trim()?.takeIf { it.length == 64 }
                if (authorPubkey != null) {
                    AuthorCard(
                        displayName = Profile.displayName(authorPubkey, author),
                        about = author?.about,
                        pictureUrl = author?.picture,
                        modifier = Modifier.padding(top = 32.dp),
                        onClick = { onOpenProfile(authorPubkey) },
                    )
                }
            }
        }
        ReadingProgressBar(
            percent = ReadingProgress.percent(scrollState.value, scrollState.maxValue),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        HighlightTextToolbar(
            selection = selection,
            showHighlight = loggedIn,
            onCopy = {
                clipboard.setText(AnnotatedString(selection.selectedText))
                selection.clear()
            },
            onHighlight = {
                val quote = selection.selectedText
                selection.clear()
                onHighlight(quote)
            },
            onSelectAll = {
                val owner = selection.owner ?: return@HighlightTextToolbar
                selection.selectAll(owner, selection.text)
            },
        )
        HighlightsPane(
            open = paneOpen,
            highlights = highlights,
            selectedId = selectedId,
            loggedIn = loggedIn,
            settings = settings,
            mineColor = mineColor,
            friendsColor = friendsColor,
            otherColor = otherColor,
            onDismiss = { paneOpen = false },
            onSelect = { item ->
                navigator.firstStop(item.id)?.let { jumpState.value(navigator.select(it)) }
                selectedId = item.id
                paneOpen = false
            },
            onOpenProfile = { pubkey ->
                paneOpen = false
                onOpenProfile(pubkey)
            },
            onToggleMarks = {
                SettingsSync.apply(settings.withBoolean("showHighlights", !settings.showHighlights))
            },
        )
    }
}

@Composable
private fun HighlightedMarkdownNode(
    model: MarkdownComponentModel,
    style: TextStyle,
    highlights: List<PaintedHighlight>,
    mineColor: Color,
    friendsColor: Color,
    otherColor: Color,
    underline: Boolean,
    selection: ReaderSelectionState,
    navigator: HighlightNavigator,
    onHighlightTap: (HighlightStop) -> Unit,
) {
    val styledText = model.content.buildMarkdownAnnotatedString(
        model.node,
        style,
        annotatorSettings(),
    )
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val owner = remember { Any() }
    MarkdownText(
        content = styledText,
        style = style,
        modifier = Modifier
            .fillMaxWidth()
            .drawHighlightMarks(
                layout,
                styledText.text,
                highlights,
                mineColor,
                friendsColor,
                otherColor,
                underline,
            )
            .highlightAnchors(
                owner = owner,
                text = styledText.text,
                layout = layout,
                coordinates = coords,
                highlights = highlights,
                navigator = navigator,
            )
            .readerSelectable(
                owner = owner,
                text = styledText.text,
                layout = layout,
                coordinates = coords,
                state = selection,
                onCoordinates = { coords = it },
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
private fun ArchiveButton(
    archived: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (archived) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.LibraryBooks,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(
                if (archived) R.string.reader_archived else R.string.reader_archive,
            ),
        )
    }
}

private fun readingColor(hex: String, fallback: Color): Color {
    val argb = HexColor.argb(hex) ?: return fallback
    return Color(argb)
}

internal fun readingTimeLabel(text: String): String? {
    val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
    if (words == 0) return null
    val minutes = max(1, (words / 200.0).roundToInt())
    return if (minutes == 1) "1 min read" else "$minutes min read"
}

internal fun highlightCountLabel(count: Int): String? {
    if (count <= 0) return null
    return if (count == 1) "1 highlight" else "$count highlights"
}

@Composable
private fun ArticleHero(
    imageUrl: String,
    title: String?,
    summary: String?,
    published: String?,
    onClick: () -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val height = (screenHeight * 0.42f).dp.coerceIn(240.dp, 420.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = imageUrl,
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
        if (published != null) {
            Text(
                text = published,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.SansSerif),
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp,
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = if (summary.isNullOrBlank()) 0.dp else 8.dp),
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
    domain: String?,
    readingTime: String?,
    highlightsLabel: String?,
    published: String?,
    onHighlightsClick: (() -> Unit)? = null,
) {
    if (domain == null && readingTime == null && highlightsLabel == null && published == null) return
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (domain != null) {
            MetaChip(text = domain, icon = Icons.Outlined.Language)
        }
        if (readingTime != null) {
            MetaChip(text = readingTime, icon = Icons.Outlined.Schedule)
        }
        if (highlightsLabel != null) {
            MetaChip(
                text = highlightsLabel,
                icon = BorisIcons.Highlighter,
                highlight = true,
                onClick = onHighlightsClick,
            )
        }
        if (published != null) {
            MetaChip(text = published, icon = Icons.Outlined.CalendarMonth)
        }
    }
}

@Composable
private fun MetaChip(
    text: String,
    icon: ImageVector,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val border = if (highlight) {
        HighlightMine.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outline
    }
    val fg = if (highlight) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconTint = if (highlight) HighlightMine else fg
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

private class ClickableCoilImageTransformer(
    private val fullWidth: Boolean,
    private val maxHeight: Dp,
    private val onImageClick: (String) -> Unit,
) : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData {
        val data = Coil3ImageTransformerImpl.transform(link)
        val sized = if (fullWidth) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.heightIn(max = maxHeight)
        }
        return data.copy(
            modifier = sized
                .clip(RoundedCornerShape(6.dp))
                .clickable { onImageClick(link) },
            contentScale = if (fullWidth) ContentScale.FillWidth else ContentScale.Fit,
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
            with(density) { Size(box.width.toSp().value, box.height.toSp().value) },
        )
    }

    @Composable
    override fun intrinsicSize(painter: Painter): Size =
        Coil3ImageTransformerImpl.intrinsicSize(painter)
}
