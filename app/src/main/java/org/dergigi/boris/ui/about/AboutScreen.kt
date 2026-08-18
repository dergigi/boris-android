package org.dergigi.boris.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Lightbulb
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import kotlinx.coroutines.launch
import org.dergigi.boris.R
import org.dergigi.boris.data.HomeOnboardingStore
import org.dergigi.boris.ui.openExternalUri
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.SourceSerif

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { ABOUT_PAGES.size })
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page == ABOUT_PAGES.lastIndex) {
                HomeOnboardingStore.dismissFirstTime(context)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
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
                when (val item = ABOUT_PAGES[page]) {
                    AboutPage.Intro -> IntroPage()
                    is AboutPage.Feature -> FeaturePage(item.feature)
                    AboutPage.Cta -> CtaPage(onStartReading = onBack)
                }
            }
            PageDots(
                count = ABOUT_PAGES.size,
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
private fun IntroPage() {
    AboutPageColumn {
        Image(
            painter = painterResource(R.drawable.ic_boris_logo),
            contentDescription = stringResource(R.string.about_title),
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
    val uriHandler = LocalUriHandler.current
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
                    uriHandler.openUri(AboutLinks.VALUE)
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
private fun CtaPage(onStartReading: () -> Unit) {
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
