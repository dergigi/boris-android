package org.dergigi.boris.ui.about

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import kotlinx.coroutines.launch
import org.dergigi.boris.R
import org.dergigi.boris.data.HomeOnboardingStore
import org.dergigi.boris.ui.openExternalUri
import org.dergigi.boris.ui.openLightningAddress
import org.dergigi.boris.ui.support.SupportStore
import org.dergigi.boris.ui.support.SupportUiState
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.SourceSerif

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    mode: AboutScreenMode = AboutScreenMode.Tutorial,
    onBack: () -> Unit,
    onOpenSupport: () -> Unit = {},
) {
    val context = LocalContext.current
    val pages = when (mode) {
        AboutScreenMode.Tutorial -> TUTORIAL_PAGES
        AboutScreenMode.Features -> FEATURE_PAGES
    }
    val titleRes = when (mode) {
        AboutScreenMode.Tutorial -> R.string.about_title
        AboutScreenMode.Features -> R.string.features_title
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        SupportStore.ensureLoaded()
    }
    LaunchedEffect(mode, pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (mode == AboutScreenMode.Tutorial && page == pages.lastIndex) {
                HomeOnboardingStore.dismissFirstTimeEverywhere(context)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.about_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (val item = pages[page]) {
                    AboutPage.FeatureIntro -> FeatureIntroPage(contentDescriptionRes = titleRes)
                    is AboutPage.Feature -> FeaturePage(item.feature)
                    AboutPage.FeatureCta -> FeatureCtaPage(
                        onStartReading = onBack,
                        onOpenSupport = onOpenSupport,
                    )
                    is AboutPage.Tutorial -> TutorialStepPage(item.step)
                    AboutPage.TutorialCta -> TutorialCtaPage(onStartReading = onBack)
                }
            }
            PageDots(
                count = pages.size,
                selected = pagerState.currentPage,
                onSelect = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun FeatureIntroPage(@StringRes contentDescriptionRes: Int) {
    AboutPageColumn {
        Image(
            painter = painterResource(R.drawable.ic_boris_logo),
            contentDescription = stringResource(contentDescriptionRes),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(140.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.about_intro_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = SourceSerif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.about_intro_body),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeaturePage(feature: AboutFeature) {
    val context = LocalContext.current
    AboutPageColumn {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/features/${feature.asset}")
                .decoderFactory(SvgDecoder.Factory())
                .build(),
            contentDescription = stringResource(feature.title),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(feature.title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = SourceSerif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            feature.paragraphs.forEach { paragraph ->
                if (paragraph == R.string.about_free_2_before) {
                    FreeAsInBeerParagraph()
                } else {
                    Text(
                        text = stringResource(paragraph),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FreeAsInBeerParagraph() {
    val context = LocalContext.current
    val supportState by SupportStore.state.collectAsStateWithLifecycle()
    val lightningAddress = (supportState as? SupportUiState.Ready)?.lightningAddress
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.about_free_2_before))
            withLink(
                LinkAnnotation.Clickable(tag = "sats", styles = linkStyle) {
                    if (lightningAddress != null) {
                        openLightningAddress(context, lightningAddress)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.support_lightning_unavailable),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            ) {
                append(stringResource(R.string.about_free_2_link))
            }
            append(stringResource(R.string.about_free_2_after))
        },
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FeatureCtaPage(
    onStartReading: () -> Unit,
    onOpenSupport: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    AboutPageColumn {
        Text(
            text = stringResource(R.string.about_cta_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = SourceSerif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.about_cta_body),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        CtaOutlinedButton(
            label = stringResource(R.string.about_cta_nostr),
            icon = painterResource(R.drawable.ic_nostr),
            onClick = { uriHandler.openUri(AboutLinks.nostrUrl) },
        )
        Spacer(Modifier.height(12.dp))
        CtaOutlinedButton(
            label = stringResource(R.string.about_cta_bug),
            icon = rememberVectorPainter(Icons.Outlined.BugReport),
            onClick = { openExternalUri(context, AboutLinks.BUG_REPORT) },
        )
        Spacer(Modifier.height(12.dp))
        CtaOutlinedButton(
            label = stringResource(R.string.about_cta_feature),
            icon = rememberVectorPainter(Icons.Outlined.Lightbulb),
            onClick = { openExternalUri(context, AboutLinks.FEATURE_REQUEST) },
        )
        Spacer(Modifier.height(12.dp))
        CtaOutlinedButton(
            label = stringResource(R.string.about_cta_thanks),
            icon = rememberVectorPainter(Icons.Filled.Favorite),
            onClick = onOpenSupport,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStartReading,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Icon(
                imageVector = BorisIcons.Highlighter,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.about_cta_start),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TutorialStepPage(step: TutorialStep) {
    AboutPageColumn {
        TutorialIllustration(step.visual)
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(step.title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = SourceSerif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(step.body),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TutorialCtaPage(onStartReading: () -> Unit) {
    AboutPageColumn {
        TutorialIllustration(TutorialVisual.Keep)
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.tutorial_cta_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = SourceSerif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.tutorial_cta_body),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onStartReading,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Icon(
                imageVector = BorisIcons.Highlighter,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.tutorial_cta_start),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TutorialIllustration(visual: TutorialVisual) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 228.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (visual) {
            TutorialVisual.Add -> ShareSheetVisual()
            TutorialVisual.Read -> ReaderVisual()
            TutorialVisual.Highlight -> HighlightVisual()
            TutorialVisual.Discover -> DiscoverVisual()
            TutorialVisual.Keep -> LibraryVisual()
        }
    }
}

@Composable
private fun ShareSheetVisual() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        MiniPanel {
            Text(
                text = stringResource(R.string.tutorial_visual_share_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniIconLabel(
                    label = stringResource(R.string.tutorial_visual_share_copy),
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    modifier = Modifier.weight(1f),
                )
                MiniIconLabel(
                    label = stringResource(R.string.tutorial_visual_share_boris),
                    icon = BorisIcons.Highlighter,
                    selected = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ReaderVisual() {
    MiniPanel {
        ArticleLines(highlighted = false)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniIconLabel(
                label = stringResource(R.string.tutorial_visual_read_clean),
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                modifier = Modifier.weight(1f),
            )
            MiniIconLabel(
                label = stringResource(R.string.tutorial_visual_read_listen),
                icon = Icons.Outlined.VolumeUp,
                selected = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HighlightVisual() {
    MiniPanel {
        ArticleLines(highlighted = true)
        Spacer(Modifier.height(14.dp))
        MiniIconLabel(
            label = stringResource(R.string.tutorial_visual_highlight),
            icon = Icons.Outlined.FormatQuote,
            selected = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DiscoverVisual() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MiniHighlightCard(widthFraction = 0.86f)
            MiniHighlightCard(widthFraction = 1f)
            MiniHighlightCard(widthFraction = 0.72f)
        }
        Column(
            modifier = Modifier.width(86.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(Icons.Outlined.Search)
            IconBadge(Icons.Outlined.Public)
        }
    }
}

@Composable
private fun LibraryVisual() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniShelf(
            title = stringResource(R.string.tutorial_visual_library_bookmarks),
            progress = 0.72f,
            modifier = Modifier.weight(1f),
        )
        MiniShelf(
            title = stringResource(R.string.tutorial_visual_library_offline),
            progress = 0.46f,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MiniPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun MiniIconLabel(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                },
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun ArticleLines(highlighted: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { index ->
            val widthFraction = when (index) {
                0 -> 0.92f
                1 -> 1f
                2 -> 0.78f
                else -> 0.64f
            }
            val isHighlighted = highlighted && index == 1
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(if (isHighlighted) 22.dp else 9.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isHighlighted) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun MiniHighlightCard(widthFraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.74f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
        )
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun MiniShelf(
    title: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(126.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Icon(
                imageVector = BorisIcons.Books,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun CtaOutlinedButton(
    label: String,
    icon: Painter,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun AboutPageColumn(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun PageDots(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.about_page_indicator, selected + 1, count)
    Row(
        modifier = modifier.semantics { contentDescription = label },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (active) 8.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    )
                    .clickable { onSelect(index) },
            )
        }
    }
}
