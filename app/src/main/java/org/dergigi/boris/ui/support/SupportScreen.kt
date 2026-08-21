package org.dergigi.boris.ui.support

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import org.dergigi.boris.R
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.ZapSplits
import org.dergigi.boris.nostr.ZapSupporter
import org.dergigi.boris.ui.theme.SourceSerif
import java.text.NumberFormat

private const val PRICING_URL = "https://www.readwithboris.com/#pricing"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: SupportViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.support_title)) },
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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ThankYouHeader()
                Spacer(Modifier.height(32.dp))
                when (val current = state) {
                    SupportUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.padding(vertical = 48.dp))
                    }
                    is SupportUiState.Ready -> {
                        SupporterSections(
                            state = current,
                            onOpenProfile = onOpenProfile,
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(24.dp))
                SupportFooter(
                    state = state as? SupportUiState.Ready,
                    onOpenProfile = onOpenProfile,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ThankYouHeader() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/thank-you.svg")
            .decoderFactory(SvgDecoder.Factory())
            .build(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 8.dp),
    )
    Spacer(Modifier.height(28.dp))
    Text(
        text = stringResource(R.string.support_thank_you),
        style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = SourceSerif,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.support_subtitle_before))
            withLink(
                LinkAnnotation.Clickable(tag = "zaps", styles = supportLinkStyle()) {
                    uriHandler.openUri(PRICING_URL)
                },
            ) {
                append(stringResource(R.string.support_subtitle_link))
            }
            append(stringResource(R.string.support_subtitle_after))
        },
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SupporterSections(
    state: SupportUiState.Ready,
    onOpenProfile: (String) -> Unit,
) {
    val legends = state.supporters.filter { it.legend }
    val others = state.supporters.filterNot { it.legend }
    val quiet = SupportAvatars.quiet(state.supporters, state.avatarSupporters)
    if (legends.isNotEmpty()) {
        SupporterSection(
            title = stringResource(R.string.support_legends),
            supporters = legends,
            profiles = state.profiles,
            onOpenProfile = onOpenProfile,
        )
        if (others.isNotEmpty() || quiet.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
        }
    }
    if (others.isNotEmpty()) {
        SupporterSection(
            title = stringResource(R.string.support_supporters),
            supporters = others,
            profiles = state.profiles,
            onOpenProfile = onOpenProfile,
        )
    }
    if (quiet.isNotEmpty()) {
        if (legends.isNotEmpty() || others.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(28.dp))
        }
        QuietAvatarRow(
            supporters = quiet,
            profiles = state.profiles,
            onOpenProfile = onOpenProfile,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupporterSection(
    title: String,
    supporters: List<ZapSupporter>,
    profiles: Map<String, Profile>,
    onOpenProfile: (String) -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = SourceSerif,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(20.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        supporters.forEach { supporter ->
            SupporterItem(
                supporter = supporter,
                profile = profiles[supporter.pubkey],
                onClick = { onOpenProfile(supporter.pubkey) },
            )
        }
    }
}

@Composable
private fun SupporterItem(
    supporter: ZapSupporter,
    profile: Profile?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SupporterAvatar(profile = profile, size = 56.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = Profile.displayName(supporter.pubkey, profile),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.SansSerif),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(
                R.string.support_sats,
                NumberFormat.getIntegerInstance().format(supporter.totalSats),
            ),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.SansSerif),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuietAvatarRow(
    supporters: List<ZapSupporter>,
    profiles: Map<String, Profile>,
    onOpenProfile: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        supporters.forEach { supporter ->
            SupporterAvatar(
                profile = profiles[supporter.pubkey],
                size = 36.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onOpenProfile(supporter.pubkey) },
            )
        }
    }
}

@Composable
private fun SupporterAvatar(
    profile: Profile?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    if (profile?.picture.isNullOrBlank()) {
        Icon(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(size),
        )
    } else {
        AsyncImage(
            model = profile?.picture,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    }
}

@Composable
private fun SupportFooter(
    state: SupportUiState.Ready?,
    onOpenProfile: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.support_footer_zap))
            withLink(
                LinkAnnotation.Clickable(tag = "boris", styles = supportLinkStyle()) {
                    onOpenProfile(ZapSplits.BORIS_PUBKEY)
                },
            ) {
                append(stringResource(R.string.support_footer_boris))
            }
            append(stringResource(R.string.support_footer_a))
            withLink(
                LinkAnnotation.Clickable(tag = "amount", styles = supportLinkStyle()) {
                    uriHandler.openUri(PRICING_URL)
                },
            ) {
                append(stringResource(R.string.support_footer_amount))
            }
            append(stringResource(R.string.support_footer_after))
        },
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state != null && state.avatarSupporters.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.support_totals,
                state.avatarSupporters.size,
                state.totalZaps,
            ),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.SansSerif),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun supportLinkStyle() = TextLinkStyles(
    style = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
    ),
)
