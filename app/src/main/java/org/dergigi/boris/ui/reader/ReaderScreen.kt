package org.dergigi.boris.ui.reader

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.mikepenz.markdown.model.markdownPadding
import org.dergigi.boris.data.ArticleUrl
import org.dergigi.boris.data.PublishedTime
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.SourceSerif
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: ReaderViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gallery by viewModel.gallery.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val highlightCount by viewModel.highlightCount.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
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
    ReaderScreenContent(
        state = state,
        gallery = gallery,
        highlights = highlights,
        highlightCount = highlightCount,
        loggedIn = loggedIn,
        onBack = onBack,
        onRetry = viewModel::load,
        onOpenArticle = onOpenArticle,
        onOpenGallery = viewModel::openGallery,
        onCloseGallery = viewModel::closeGallery,
        onGalleryPage = viewModel::setGalleryIndex,
        onHighlight = { quote ->
            viewModel.highlight(quote)?.let(launcher::launch)
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
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenGallery: (List<String>, Int) -> Unit,
    onCloseGallery: () -> Unit,
    onGalleryPage: (Int) -> Unit,
    onHighlight: (String) -> Unit,
) {
    val context = LocalContext.current
    val articleUrl = when (state) {
        is ReaderUiState.Ready -> state.content.url
        is ReaderUiState.Error -> state.url
        ReaderUiState.Loading -> null
    }

    fun openOriginal() {
        val url = articleUrl ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    fun shareArticle() {
        val url = articleUrl ?: return
        val title = (state as? ReaderUiState.Ready)?.content?.title
        val text = if (title.isNullOrBlank()) url else "$title\n$url"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (!title.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
        }
        context.startActivity(Intent.createChooser(intent, "Share article"))
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
                    if (articleUrl != null) {
                        IconButton(onClick = ::shareArticle) {
                            Icon(Icons.Filled.Share, contentDescription = "Share article")
                        }
                        IconButton(onClick = ::openOriginal) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open original")
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
                    onOpenArticle = onOpenArticle,
                    onOpenGallery = onOpenGallery,
                    onHighlight = onHighlight,
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
    onOpenArticle: (String) -> Unit,
    onOpenGallery: (List<String>, Int) -> Unit,
    onHighlight: (String) -> Unit,
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
    val imageTransformer = ClickableCoilImageTransformer { link ->
        val opened = UrlExtractor.articleUrl(link, content.url) ?: return@ClickableCoilImageTransformer
        val urls = UrlExtractor.imageUrls(content.body, content.url).toMutableList()
        if (opened !in urls) urls.add(0, opened)
        onOpenGallery(urls, urls.indexOf(opened).coerceAtLeast(0))
    }
    val highlightedComponents = remember(highlights) {
        markdownComponents(
            text = { HighlightedMarkdownNode(it, it.typography.text, highlights) },
            paragraph = { HighlightedMarkdownNode(it, it.typography.paragraph, highlights) },
            heading1 = { HighlightedMarkdownNode(it, it.typography.h1, highlights) },
            heading2 = { HighlightedMarkdownNode(it, it.typography.h2, highlights) },
            heading3 = { HighlightedMarkdownNode(it, it.typography.h3, highlights) },
            heading4 = { HighlightedMarkdownNode(it, it.typography.h4, highlights) },
            heading5 = { HighlightedMarkdownNode(it, it.typography.h5, highlights) },
            heading6 = { HighlightedMarkdownNode(it, it.typography.h6, highlights) },
        )
    }
    val view = LocalView.current
    val clipboard = LocalClipboardManager.current
    val toolbar = remember(view, clipboard, loggedIn, onHighlight) {
        HighlightTextToolbar(
            view = view,
            showHighlight = loggedIn,
            clipboard = clipboard,
            onHighlight = onHighlight,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalTextToolbar provides toolbar) {
        SelectionContainer(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
        ) {
            Column {
                if (!content.title.isNullOrBlank()) {
                    var titleLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                    Text(
                        text = content.title,
                        style = typography.headlineLarge,
                        color = colors.onBackground,
                        onTextLayout = { titleLayout = it },
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 12.dp)
                            .drawHighlightMarks(titleLayout, content.title, highlights),
                    )
                }
                ArticleMetaRow(
                    domain = domain,
                    readingTime = readingTime,
                    highlightsLabel = highlightsLabel,
                    published = published,
                )
                CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                    Markdown(
                        content = content.body,
                        colors = markdownColor(
                            text = colors.onBackground,
                            codeBackground = colors.surfaceVariant,
                            inlineCodeBackground = colors.surfaceVariant,
                            dividerColor = colors.outline,
                            tableBackground = colors.surfaceVariant.copy(alpha = 0.4f),
                        ),
                        typography = markdownTypography(
                            h1 = typography.headlineLarge,
                            h2 = typography.headlineMedium,
                            h3 = typography.headlineSmall,
                            h4 = typography.titleLarge,
                            h5 = typography.titleLarge.copy(fontSize = 18.sp),
                            h6 = typography.titleLarge.copy(fontSize = 16.sp),
                            text = typography.bodyLarge,
                            paragraph = typography.bodyLarge,
                            quote = typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            ordered = typography.bodyLarge,
                            bullet = typography.bodyLarge,
                            list = typography.bodyLarge,
                            code = typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, textAlign = TextAlign.Left),
                            inlineCode = typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            table = typography.bodyMedium.copy(fontFamily = SourceSerif, textAlign = TextAlign.Left),
                            textLink = TextLinkStyles(
                                style = typography.bodyLarge.copy(color = colors.secondary).toSpanStyle(),
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
            }
        }
        }
    }
}

@Composable
private fun HighlightedMarkdownNode(
    model: MarkdownComponentModel,
    style: TextStyle,
    highlights: List<PaintedHighlight>,
) {
    val styledText = model.content.buildMarkdownAnnotatedString(
        model.node,
        style,
        annotatorSettings(),
    )
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    MarkdownText(
        content = styledText,
        style = style,
        modifier = Modifier.drawHighlightMarks(layout, styledText.text, highlights),
        onTextLayout = { result, _ -> layout = result },
    )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArticleMetaRow(
    domain: String?,
    readingTime: String?,
    highlightsLabel: String?,
    published: String?,
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
                icon = Icons.Outlined.Highlight,
                highlight = true,
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
    Row(
        modifier = Modifier
            .border(1.dp, border, RoundedCornerShape(8.dp))
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
    private val onImageClick: (String) -> Unit,
) : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData {
        val data = Coil3ImageTransformerImpl.transform(link)
        return data.copy(
            modifier = data.modifier.clickable { onImageClick(link) },
        )
    }

    @Composable
    override fun intrinsicSize(painter: Painter): Size =
        Coil3ImageTransformerImpl.intrinsicSize(painter)
}
