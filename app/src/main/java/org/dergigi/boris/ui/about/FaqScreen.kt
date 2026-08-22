package org.dergigi.boris.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.R
import org.dergigi.boris.ui.reader.openWeblink
import org.dergigi.boris.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.faq_title)) },
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
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                FAQ_ITEMS.forEach { item ->
                    FaqEntry(
                        item = item,
                        openInBoris = settings.openLinksInReader,
                        onOpenArticle = onOpenArticle,
                    )
                }
                WebsiteFaqLink(
                    openInBoris = settings.openLinksInReader,
                    onOpenArticle = onOpenArticle,
                )
            }
        }
    }
}

@Composable
private fun FaqEntry(
    item: FaqItem,
    openInBoris: Boolean,
    onOpenArticle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(item.section),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(item.question),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        FaqAnswer(
            parts = item.answer,
            openInBoris = openInBoris,
            onOpenArticle = onOpenArticle,
        )
    }
}

@Composable
private fun FaqAnswer(
    parts: List<FaqPart>,
    openInBoris: Boolean,
    onOpenArticle: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    Text(
        text = buildAnnotatedString {
            parts.forEach { part ->
                when (part) {
                    is FaqPart.Text -> append(stringResource(part.res))
                    is FaqPart.Link -> withLink(
                        LinkAnnotation.Clickable(tag = part.url, styles = linkStyle) {
                            openWeblink(
                                part.url,
                                openInBoris,
                                onOpenArticle,
                                uriHandler::openUri,
                            )
                        },
                    ) {
                        append(stringResource(part.res))
                    }
                }
            }
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WebsiteFaqLink(
    openInBoris: Boolean,
    onOpenArticle: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    TextButton(
        onClick = {
            openWeblink(AboutLinks.FAQ, openInBoris, onOpenArticle, uriHandler::openUri)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.faq_website))
    }
}
