package org.dergigi.boris.ui.reader

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.ui.theme.SourceSerif
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReaderScreenContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::load,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreenContent(
    state: ReaderUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val articleUrl = when (state) {
        is ReaderUiState.Ready -> state.content.url
        is ReaderUiState.Error -> state.url
        ReaderUiState.Loading -> null
    }

    fun openOriginal() {
        val url = articleUrl ?: return
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

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
                }
            }
            is ReaderUiState.Ready -> {
                ArticleBody(
                    content = state.content,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun ArticleBody(
    content: ReadableContent,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val readingTime = readingTimeLabel(content.body)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
        ) {
            if (!content.title.isNullOrBlank()) {
                Text(
                    text = content.title,
                    style = typography.headlineLarge,
                    color = colors.onBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                )
            }
            if (readingTime != null) {
                Text(
                    text = readingTime,
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
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
                imageTransformer = Coil3ImageTransformerImpl,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal fun readingTimeLabel(text: String): String? {
    val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
    if (words == 0) return null
    val minutes = max(1, (words / 200.0).roundToInt())
    return if (minutes == 1) "1 min read" else "$minutes min read"
}
