package org.dergigi.boris.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.R
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.theme.BorisIcons

enum class SettingsCategory(
    val titleRes: Int,
    val subtitleRes: Int,
    val tint: Color,
) {
    Appearance(R.string.settings_appearance, R.string.settings_appearance_summary, SettingsTints.Look),
    Reading(R.string.settings_reading, R.string.settings_reading_summary, SettingsTints.Look),
    Tts(R.string.tts_section_title, R.string.settings_tts_summary, SettingsTints.Look),
    Media(R.string.settings_media, R.string.settings_media_summary, SettingsTints.Look),
    Highlights(R.string.settings_highlights, R.string.settings_highlights_summary, SettingsTints.Look),
    ZapSplits(R.string.settings_zap_splits, R.string.settings_zap_summary, SettingsTints.Look),
    Home(R.string.settings_home, R.string.settings_home_summary, SettingsTints.Places),
    Library(R.string.settings_library, R.string.settings_library_summary, SettingsTints.Places),
    Feed(R.string.feed_title, R.string.settings_feed_summary, SettingsTints.Places),
    Scroll(R.string.settings_scroll_behaviour, R.string.settings_scroll_summary, SettingsTints.Places),
    Relays(R.string.settings_relays, R.string.settings_relays_summary, SettingsTints.Places),
    Airplane(R.string.settings_airplane_mode, R.string.settings_airplane_summary, SettingsTints.Places),
    About(R.string.settings_about, R.string.settings_about_summary, SettingsTints.About),
}

private val SettingsCategory.icon: ImageVector
    @Composable get() = when (this) {
        SettingsCategory.Appearance -> Icons.Outlined.Palette
        SettingsCategory.Reading -> Icons.AutoMirrored.Outlined.MenuBook
        SettingsCategory.Tts -> Icons.Outlined.VolumeUp
        SettingsCategory.Media -> Icons.Outlined.Image
        SettingsCategory.Highlights -> BorisIcons.Highlighter
        SettingsCategory.ZapSplits -> Icons.Outlined.Bolt
        SettingsCategory.Home -> Icons.Outlined.Home
        SettingsCategory.Library -> Icons.Outlined.LocalLibrary
        SettingsCategory.Feed -> Icons.Outlined.RssFeed
        SettingsCategory.Scroll -> Icons.Outlined.SwapVert
        SettingsCategory.Relays -> Icons.Outlined.CellTower
        SettingsCategory.Airplane -> Icons.Outlined.Flight
        SettingsCategory.About -> Icons.Outlined.Info
    }

private val CATEGORY_GROUPS = listOf(
    listOf(
        SettingsCategory.Appearance,
        SettingsCategory.Reading,
        SettingsCategory.Tts,
        SettingsCategory.Media,
        SettingsCategory.Highlights,
        SettingsCategory.ZapSplits,
    ),
    listOf(
        SettingsCategory.Home,
        SettingsCategory.Library,
        SettingsCategory.Feed,
        SettingsCategory.Scroll,
        SettingsCategory.Relays,
        SettingsCategory.Airplane,
    ),
    listOf(SettingsCategory.About),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAuthorProfile: () -> Unit,
    initialCategory: String? = null,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val settingsMessage by settingsViewModel.message.collectAsStateWithLifecycle()
    val signIntent by settingsViewModel.signIntent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        settingsViewModel.onSignerResult(result.resultCode, result.data)
    }
    var openCategory by rememberSaveable { mutableStateOf(initialCategory) }
    val category = openCategory?.let { name -> SettingsCategory.entries.firstOrNull { it.name == name } }

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
    BackHandler(enabled = category != null) { openCategory = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(category?.titleRes ?: R.string.settings_title))
                },
                navigationIcon = {
                    IconButton(
                        onClick = { if (category != null) openCategory = null else onBack() },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
            ) {
                if (category == null) {
                    SettingsCategoryList(
                        onOpen = { openCategory = it.name },
                        openInBoris = settings.openLinksInReader,
                        onOpenArticle = onOpenArticle,
                    )
                } else {
                    SettingsCategoryDetail(
                        category = category,
                        settings = settings,
                        onUpdate = { next -> settingsViewModel.update { next } },
                        onOpenArticle = onOpenArticle,
                        onOpenTutorial = onOpenTutorial,
                        onOpenFaq = onOpenFaq,
                        onOpenSupport = onOpenSupport,
                        onOpenAuthorProfile = onOpenAuthorProfile,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryList(
    onOpen: (SettingsCategory) -> Unit,
    openInBoris: Boolean,
    onOpenArticle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CATEGORY_GROUPS.forEach { group ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            ) {
                group.forEach { entry ->
                    SettingsCategoryRow(
                        category = entry,
                        onClick = { onOpen(entry) },
                    )
                }
            }
        }
        SettingsVersionFooter(
            openInBoris = openInBoris,
            onOpenArticle = onOpenArticle,
        )
    }
}

@Composable
private fun SettingsCategoryRow(
    category: SettingsCategory,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(category.tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = category.tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(category.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsCategoryDetail(
    category: SettingsCategory,
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAuthorProfile: () -> Unit,
) {
    val darkTheme = settings.isDark(isSystemInDarkTheme())
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        when (category) {
            SettingsCategory.Appearance -> {
                ThemeSection(settings = settings, onUpdate = onUpdate)
                ReadingPreview(settings = settings, darkTheme = darkTheme)
            }
            SettingsCategory.Reading -> {
                ReadingSection(settings = settings, darkTheme = darkTheme, onUpdate = onUpdate)
                ReadingPreview(settings = settings, darkTheme = darkTheme)
            }
            SettingsCategory.Tts -> TtsSection(settings = settings, onUpdate = onUpdate)
            SettingsCategory.Media -> MediaSection(settings = settings, onUpdate = onUpdate)
            SettingsCategory.Highlights -> {
                HighlightsSection(settings = settings, onUpdate = onUpdate)
                ReadingPreview(settings = settings, darkTheme = darkTheme)
            }
            SettingsCategory.ZapSplits -> ZapSplitsSection(settings = settings, onUpdate = onUpdate)
            SettingsCategory.Home -> HomeSettingsSection(settings = settings, onUpdate = onUpdate)
            SettingsCategory.Library -> LibrarySettingsSection(settings = settings, onUpdate = onUpdate)
            SettingsCategory.Feed -> {
                FeedSettingsSection(settings = settings, onUpdate = onUpdate)
                RssFeedsSection(settings = settings, onUpdate = onUpdate)
            }
            SettingsCategory.Scroll -> ScrollBehaviourSection(settings = settings, onUpdate = onUpdate)
            SettingsCategory.Relays -> RelaysSection()
            SettingsCategory.Airplane -> {
                AirplaneModeSection(
                    settings = settings,
                    onUpdate = onUpdate,
                    onOpenArticle = onOpenArticle,
                )
                OfflineSection(settings = settings, onUpdate = onUpdate)
            }
            SettingsCategory.About -> AboutSettingsSection(
                openInBoris = settings.openLinksInReader,
                onOpenArticle = onOpenArticle,
                onOpenTutorial = onOpenTutorial,
                onOpenFaq = onOpenFaq,
                onOpenSupport = onOpenSupport,
                onOpenAuthorProfile = onOpenAuthorProfile,
            )
        }
    }
}
