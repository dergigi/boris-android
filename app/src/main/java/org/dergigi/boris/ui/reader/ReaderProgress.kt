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
internal fun ArticleScrollProgress(
    scrollState: ScrollState,
    driftFraction: Float?,
    scrollLive: Boolean = true,
    outlineItems: List<ArticleOutlineItem> = emptyList(),
    activeOutlineId: MutableState<String?>? = null,
    showHeading: Boolean = true,
) {
    val heading = activeOutlineId?.value?.takeIf { showHeading }?.let { id ->
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
internal fun JumpBackPill(
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

internal class ReadingProgressUi(
    val positionRestored: Boolean,
    val articleReady: Boolean,
    val markArticleReady: () -> Unit,
    val drifting: Boolean,
    val clearDrift: () -> Unit,
    val readingTracker: ReadingTracker,
    val savedFraction: Float,
    val driftFraction: Float?,
    val jumpBackFraction: Float?,
)

@Composable
internal fun ReadingProgressEffects(
    url: String,
    focusHighlightId: String,
    scrollState: ScrollState,
    viewportHeight: Int,
    settings: UserSettings,
    loggedIn: Boolean,
    archived: Boolean,
    onArchive: (Boolean) -> Unit,
): ReadingProgressUi {
    val appContext = LocalContext.current.applicationContext
    var positionRestored by remember(url) { mutableStateOf(false) }
    // True once the markdown body has parsed and rendered. Before that the
    // scroll range only covers title/summary chrome, so any progress computed
    // against it is garbage: scrolling a still-loading article could save
    // 100% and even auto-archive it (issue #131).
    var articleReady by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url) {
        if (positionRestored) return@LaunchedEffect
        if (focusHighlightId.isNotBlank()) {
            positionRestored = true
            return@LaunchedEffect
        }
        // Throttled no-op most of the time; first call pulls positions from relays.
        withContext(Dispatchers.IO) { ReadingPositionSync.refresh(appContext) }
        val saved = ReadingPositionStore.fraction(url)
        val max = snapshotFlow { if (articleReady) scrollState.maxValue else 0 }.first { it > 0 }
        if (settings.autoScrollToReadingPosition && scrollState.value == 0) {
            ReadingProgress.restoreOffset(saved, max)?.let { scrollState.scrollTo(it) }
        }
        positionRestored = true
    }
    // Issue #86: while the user explores far away from the saved reading
    // position, keep that position frozen and offer a jump back. The tracker
    // only adopts the new spot after sustained reading-like movement there.
    val readingTracker = remember(url) { ReadingTracker() }
    var drifting by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url) {
        snapshotFlow { scrollState.value }.collectLatest { value ->
            if (!positionRestored || !articleReady) return@collectLatest
            val max = scrollState.maxValue
            if (max <= 0) return@collectLatest
            delay(400)
            val saved = ReadingPositionStore.fraction(url)
            val reading = readingTracker.onSettle(
                offset = value,
                savedOffset = (saved * max).roundToInt(),
                viewportHeight = viewportHeight,
                nowMs = System.currentTimeMillis(),
            )
            if (reading) {
                ReadingPositionStore.save(url, ReadingProgress.fraction(value, max))
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
        remember(url, progressVersion) {
            ReadingPositionStore.fraction(url)
        }
    } else {
        0f
    }
    val driftFraction = savedFraction.takeIf { drifting }
    val jumpBackFraction = driftFraction?.takeIf { ReadingProgress.showsJumpBack(it) }
    DisposableEffect(url) {
        onDispose { ReadingPositionSync.publishAsync(appContext, url) }
    }
    val autoArchive = settings.autoMarkAsReadOnCompletion
    LaunchedEffect(url, loggedIn, archived, autoArchive) {
        snapshotFlow { ReadingProgress.percent(scrollState.value, scrollState.maxValue) }
            .collectLatest { percent ->
                if (percent < 100 || !positionRestored || !articleReady) return@collectLatest
                // Mirrors the webapp: complete only after holding 100% for 2s.
                delay(2000)
                ReadingPositionSync.publishAsync(appContext, url)
                if (autoArchive && loggedIn && !archived) onArchive(false)
            }
    }
    return ReadingProgressUi(
        positionRestored = positionRestored,
        articleReady = articleReady,
        markArticleReady = { articleReady = true },
        drifting = drifting,
        clearDrift = { drifting = false },
        readingTracker = readingTracker,
        savedFraction = savedFraction,
        driftFraction = driftFraction,
        jumpBackFraction = jumpBackFraction,
    )
}
