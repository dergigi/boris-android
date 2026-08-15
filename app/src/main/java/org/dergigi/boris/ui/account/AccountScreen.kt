package org.dergigi.boris.ui.account

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.R
import org.dergigi.boris.ui.auth.AuthBar
import org.dergigi.boris.ui.auth.AuthUiState
import org.dergigi.boris.ui.auth.AuthViewModel
import org.dergigi.boris.ui.auth.NstartFooter
import org.dergigi.boris.ui.reader.HighlightMarks
import org.dergigi.boris.ui.settings.ReadingDisplaySection
import org.dergigi.boris.ui.settings.SettingsViewModel
import org.dergigi.boris.ui.settings.ThemeSection
import org.dergigi.boris.ui.theme.HighlightMine

@Composable
fun AccountScreen(
    incomingBunker: String? = null,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    var bunkerUri by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(incomingBunker) {
        if (!incomingBunker.isNullOrBlank()) {
            bunkerUri = incomingBunker
        }
    }
    val authState by viewModel.state.collectAsStateWithLifecycle()
    val authMessage by viewModel.message.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val settingsMessage by settingsViewModel.message.collectAsStateWithLifecycle()
    val signIntent by settingsViewModel.signIntent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onSignerResult(result.resultCode, result.data)
    }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        settingsViewModel.onSignerResult(result.resultCode, result.data)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    LaunchedEffect(signIntent) {
        val intent = signIntent ?: return@LaunchedEffect
        settingsViewModel.consumeSignIntent()
        settingsLauncher.launch(intent)
    }
    LaunchedEffect(settingsMessage) {
        val text = settingsMessage ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        settingsViewModel.consumeMessage()
    }

    val loggedIn = authState is AuthUiState.LoggedIn
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .align(if (loggedIn) Alignment.TopCenter else Alignment.Center)
                .widthIn(max = if (loggedIn) 720.dp else 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = if (loggedIn) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (loggedIn) {
                ThemeSection(
                    settings = settings,
                    onUpdate = { next -> settingsViewModel.update { next } },
                )
                ReadingDisplaySection(
                    settings = settings,
                    darkTheme = settings.isDark(isSystemInDarkTheme()),
                    onUpdate = { next -> settingsViewModel.update { next } },
                )
            } else {
                LoginCopy()
            }
            AuthBar(
                state = authState,
                message = authMessage,
                bunkerUri = bunkerUri,
                onBunkerUriChange = { bunkerUri = it },
                onConnect = {
                    viewModel.connectIntent()?.let(authLauncher::launch)
                },
                onConnectBunker = { viewModel.connectBunker(bunkerUri) },
                onSignOut = {
                    settingsViewModel.cancelPending()
                    viewModel.signOut()
                },
            )
            if (!loggedIn) {
                NstartFooter()
            }
        }
    }
}

@Composable
private fun LoginCopy() {
    val copy = stringResource(R.string.home_copy)
    val npub = stringResource(R.string.home_mark_npub)
    val highlights = stringResource(R.string.home_mark_highlights)
    val mark = SpanStyle(
        color = Color.Black,
        fontWeight = FontWeight.Medium,
    )
    val annotated = buildAnnotatedString {
        var cursor = 0
        listOf(npub, highlights).forEach { token ->
            val index = copy.indexOf(token, cursor)
            if (index < 0) return@forEach
            append(copy.substring(cursor, index))
            val start = length
            withStyle(mark) { append(token) }
            addStringAnnotation(HIGHLIGHT_TAG, token, start, length)
            cursor = index + token.length
        }
        if (cursor < copy.length) append(copy.substring(cursor))
        if (length == 0) append(copy)
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val padX = 5.dp
    val padY = 3.dp
    val radius = 3.dp
    Text(
        text = annotated,
        onTextLayout = { layout = it },
        modifier = Modifier.drawBehind {
            val result = layout ?: return@drawBehind
            val padXPx = padX.toPx()
            val padYPx = padY.toPx()
            val corner = CornerRadius(radius.toPx())
            annotated.getStringAnnotations(HIGHLIGHT_TAG, 0, annotated.length).forEach { range ->
                HighlightMarks.highlightRects(result, range.start, range.end).forEach { box ->
                    drawRoundRect(
                        color = HighlightMine.copy(alpha = HighlightMarks.HighlightMarkAlpha),
                        topLeft = Offset(box.left - padXPx, box.top - padYPx),
                        size = Size(box.width + padXPx * 2, box.height + padYPx * 2),
                        cornerRadius = corner,
                    )
                }
            }
        },
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp,
            lineHeight = 32.sp,
            textAlign = TextAlign.Center,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val HIGHLIGHT_TAG = "highlight"
