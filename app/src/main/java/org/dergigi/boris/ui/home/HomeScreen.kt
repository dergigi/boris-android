package org.dergigi.boris.ui.home

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import org.dergigi.boris.R
import org.dergigi.boris.data.ArchivedArticles
import org.dergigi.boris.data.ClipboardLink
import org.dergigi.boris.data.HighlightedArticle
import org.dergigi.boris.data.HomeFilters
import org.dergigi.boris.data.ReadingPositionStore
import org.dergigi.boris.data.SensitiveContent
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.NsfwBadge
import org.dergigi.boris.tts.requestTtsNotificationPermissionOnce
import org.dergigi.boris.ui.ArticleActionsMenu
import org.dergigi.boris.data.HomeOnboardingStore
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.NostrTarget
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.ui.auth.AuthUiState
import org.dergigi.boris.ui.auth.AuthViewModel
import org.dergigi.boris.ui.reader.CardReadingProgress
import org.dergigi.boris.ui.settings.hexColor
import org.dergigi.boris.ui.TopBarMenuItem
import org.dergigi.boris.ui.TopBarMoreMenu
import org.dergigi.boris.ui.support.SupportHeart
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRead: (String) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSupport: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onOpenLogin: () -> Unit = {},
    onOpenHomeSettings: () -> Unit = {},
    onOpenAboutSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
) {
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val settingsReady by SettingsSync.ready.collectAsStateWithLifecycle()
    val hasRemoteSettings by SettingsSync.hasRemote.collectAsStateWithLifecycle()
    val loggedIn = authState is AuthUiState.LoggedIn
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onSignerResult(result.resultCode, result.data)
    }
    val ttsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }
    var loginPromptDismissed by remember {
        mutableStateOf(HomeOnboardingStore.isLoginDismissed(context))
    }
    val showLoginPrompt = !loggedIn && !loginPromptDismissed
    val showFirstTime = HomeOnboardingStore.shouldShowFirstTime(
        localDismissed = HomeOnboardingStore.isFirstTimeDismissed(context),
        settingsDismissed = settings.firstTimeDismissed,
        loggedIn = loggedIn,
        settingsReady = settingsReady,
        hasRemoteSettings = hasRemoteSettings,
    )
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    val windowInfo = LocalWindowInfo.current
    var clipboardUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { focused ->
            if (focused) clipboardUrl = ClipboardLink.read(context)
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    SupportHeart(
                        onOpenSupport = onOpenSupport,
                        onOpenProfile = onOpenProfile,
                    )
                },
                actions = {
                    HomeFilterMenu(settings = settings)
                    TopBarMoreMenu(
                        items = listOf(
                            TopBarMenuItem(
                                label = stringResource(R.string.home_help),
                                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                                onClick = onOpenAbout,
                            ),
                            TopBarMenuItem(
                                label = stringResource(R.string.settings_about),
                                icon = Icons.Outlined.Info,
                                onClick = onOpenAboutSettings,
                            ),
                            TopBarMenuItem(
                                label = stringResource(R.string.home_settings),
                                icon = Icons.Outlined.Settings,
                                onClick = onOpenHomeSettings,
                            ),
                        ),
                    )
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            clipboardUrl?.let { url ->
                ClipboardBanner(
                    url = url,
                    onOpen = {
                        ClipboardLink.markHandled(url)
                        clipboardUrl = null
                        onRead(url)
                    },
                    onDismiss = {
                        ClipboardLink.markHandled(url)
                        clipboardUrl = null
                    },
                )
            }
            HomeScreenContent(
                highlights = highlights,
                refreshing = refreshing,
                loggedIn = loggedIn,
                hideArchived = settings.hideArchivedOnHome,
                hideCompleted = settings.hideCompletedOnHome,
                hideNsfw = settings.hideNsfwOnHome,
                sectionOrder = HomeSections.order(settings.homeSectionOrder),
                mineColor = hexColor(settings.highlightColorMine, HighlightMine),
                friendsColor = hexColor(settings.highlightColorFriends, HighlightFriends),
                nostrverseColor = hexColor(settings.highlightColorNostrverse, HighlightOther),
                showFirstTime = showFirstTime,
                onDismissFirstTime = {
                    HomeOnboardingStore.dismissFirstTimeEverywhere(context)
                },
                onOpenAbout = onOpenAbout,
                showLoginPrompt = showLoginPrompt,
                onDismissLoginPrompt = {
                    HomeOnboardingStore.dismissLogin(context)
                    loginPromptDismissed = true
                },
                onOpenLogin = onOpenLogin,
                onRefresh = viewModel::refresh,
                onRead = onRead,
                onListen = { article ->
                    requestTtsNotificationPermissionOnce(context) {
                        ttsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.startListening(article.url)
                },
                onMarkAsRead = { article ->
                    viewModel.markAsRead(article)?.let(launcher::launch)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ClipboardBanner(
    url: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.ContentPaste,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_clipboard_open),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = url
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("nostr://")
                    .removePrefix("nostr:"),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.home_clipboard_dismiss),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    highlights: HomeHighlightsState,
    refreshing: Boolean,
    loggedIn: Boolean,
    hideArchived: Boolean,
    hideCompleted: Boolean = false,
    hideNsfw: Boolean = false,
    mineColor: Color,
    friendsColor: Color,
    nostrverseColor: Color,
    onRefresh: () -> Unit,
    onRead: (String) -> Unit,
    onListen: (HighlightedArticle) -> Unit = {},
    onMarkAsRead: (HighlightedArticle) -> Unit = {},
    modifier: Modifier = Modifier,
    sectionOrder: List<String> = HomeSections.DEFAULT,
    showFirstTime: Boolean = false,
    onDismissFirstTime: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    showLoginPrompt: Boolean = false,
    onDismissLoginPrompt: () -> Unit = {},
    onOpenLogin: () -> Unit = {},
) {
    val hasPrompts = showFirstTime || showLoginPrompt
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (highlights) {
            HomeHighlightsState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    HomePromptSections(
                        showFirstTime = showFirstTime,
                        onDismissFirstTime = onDismissFirstTime,
                        onOpenAbout = onOpenAbout,
                        showLoginPrompt = showLoginPrompt,
                        onDismissLoginPrompt = onDismissLoginPrompt,
                        onOpenLogin = onOpenLogin,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        HomeLoadingIndicator()
                    }
                }
            }
            HomeHighlightsState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    HomePromptSections(
                        showFirstTime = showFirstTime,
                        onDismissFirstTime = onDismissFirstTime,
                        onOpenAbout = onOpenAbout,
                        showLoginPrompt = showLoginPrompt,
                        onDismissLoginPrompt = onDismissLoginPrompt,
                        onOpenLogin = onOpenLogin,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        StatusMessage(
                            text = stringResource(R.string.feed_empty),
                            onRetry = onRefresh,
                        )
                    }
                }
            }
            HomeHighlightsState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    HomePromptSections(
                        showFirstTime = showFirstTime,
                        onDismissFirstTime = onDismissFirstTime,
                        onOpenAbout = onOpenAbout,
                        showLoginPrompt = showLoginPrompt,
                        onDismissLoginPrompt = onDismissLoginPrompt,
                        onOpenLogin = onOpenLogin,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        StatusMessage(
                            text = stringResource(R.string.feed_error),
                            onRetry = onRefresh,
                        )
                    }
                }
            }
            is HomeHighlightsState.Ready -> {
                val progressVersion by ReadingPositionStore.version.collectAsStateWithLifecycle()
                val yours = remember(
                    highlights.yours, highlights.archivedKeys,
                    hideArchived, hideCompleted, hideNsfw, progressVersion,
                ) {
                    HomeFilters.visible(
                        highlights.yours, highlights.archivedKeys,
                        hideArchived, hideCompleted, hideNsfw,
                    )
                }
                val friends = remember(
                    highlights.friends, highlights.archivedKeys,
                    hideArchived, hideCompleted, hideNsfw, progressVersion,
                ) {
                    HomeFilters.visible(
                        highlights.friends, highlights.archivedKeys,
                        hideArchived, hideCompleted, hideNsfw,
                    )
                }
                val others = remember(
                    highlights.others, highlights.archivedKeys,
                    hideArchived, hideCompleted, hideNsfw, progressVersion,
                ) {
                    HomeFilters.visible(
                        highlights.others, highlights.archivedKeys,
                        hideArchived, hideCompleted, hideNsfw,
                    )
                }
                val continueReading = remember(
                    highlights.continueReading, highlights.archivedKeys,
                    hideArchived, hideCompleted, hideNsfw, progressVersion,
                ) {
                    HomeFilters.visible(
                        highlights.continueReading, highlights.archivedKeys,
                        hideArchived, hideCompleted, hideNsfw,
                    )
                }
                val mostHighlighted = remember(
                    highlights.mostHighlighted, highlights.archivedKeys,
                    hideArchived, hideCompleted, hideNsfw, progressVersion,
                ) {
                    HomeFilters.visible(
                        highlights.mostHighlighted, highlights.archivedKeys,
                        hideArchived, hideCompleted, hideNsfw,
                    )
                }
                val shortReads = remember(
                    highlights.shortReads, highlights.archivedKeys,
                    hideArchived, hideCompleted, hideNsfw, progressVersion,
                ) {
                    HomeFilters.visible(
                        highlights.shortReads, highlights.archivedKeys,
                        hideArchived, hideCompleted, hideNsfw,
                    )
                }
                val longReads = remember(
                    highlights.longReads, highlights.archivedKeys,
                    hideArchived, hideCompleted, hideNsfw, progressVersion,
                ) {
                    HomeFilters.visible(
                        highlights.longReads, highlights.archivedKeys,
                        hideArchived, hideCompleted, hideNsfw,
                    )
                }
                val randomArticles = remember(
                    highlights.randomArticles, highlights.archivedKeys,
                    hideArchived, hideCompleted, hideNsfw, progressVersion,
                ) {
                    HomeFilters.visible(
                        highlights.randomArticles, highlights.archivedKeys,
                        hideArchived, hideCompleted, hideNsfw,
                    )
                }
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val empty = yours.isEmpty() && friends.isEmpty() && others.isEmpty() &&
                        continueReading.isEmpty() && mostHighlighted.isEmpty() &&
                        shortReads.isEmpty() && longReads.isEmpty() &&
                        randomArticles.isEmpty()
                    if (empty && !hasPrompts) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            StatusMessage(
                                text = stringResource(
                                    if (hideCompleted || hideNsfw ||
                                        (hideArchived && highlights.archivedKeys.isNotEmpty())
                                    ) {
                                        R.string.home_empty_filters
                                    } else {
                                        R.string.feed_empty
                                    },
                                ),
                                onRetry = onRefresh,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(top = 20.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(28.dp),
                        ) {
                            HomePromptSections(
                                showFirstTime = showFirstTime,
                                onDismissFirstTime = onDismissFirstTime,
                                onOpenAbout = onOpenAbout,
                                showLoginPrompt = showLoginPrompt,
                                onDismissLoginPrompt = onDismissLoginPrompt,
                                onOpenLogin = onOpenLogin,
                            )
                            if (empty) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    StatusMessage(
                                        text = stringResource(
                                            if (hideCompleted || hideNsfw ||
                                                (hideArchived && highlights.archivedKeys.isNotEmpty())
                                            ) {
                                                R.string.home_empty_filters
                                            } else {
                                                R.string.feed_empty
                                            },
                                        ),
                                        onRetry = onRefresh,
                                    )
                                }
                            } else {
                                sectionOrder.forEach { section ->
                                    when (section) {
                                        HomeSections.CONTINUE -> if (continueReading.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(R.string.home_continue_reading),
                                                items = continueReading,
                                                rowKey = "continue",
                                                tint = MaterialTheme.colorScheme.primary,
                                                icon = Icons.AutoMirrored.Outlined.MenuBook,
                                                loggedIn = loggedIn,
                                                archivedKeys = highlights.archivedKeys,
                                                onRead = onRead,
                                                onListen = onListen,
                                                onMarkAsRead = onMarkAsRead,
                                            )
                                        }
                                        HomeSections.YOURS -> if (yours.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(R.string.home_recently_highlighted_by_you),
                                                items = yours,
                                                rowKey = "you",
                                                tint = mineColor,
                                                loggedIn = loggedIn,
                                                archivedKeys = highlights.archivedKeys,
                                                onRead = onRead,
                                                onListen = onListen,
                                                onMarkAsRead = onMarkAsRead,
                                            )
                                        }
                                        HomeSections.FRIENDS -> if (friends.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(R.string.home_recently_highlighted_by_friends),
                                                items = friends,
                                                rowKey = "friends",
                                                tint = friendsColor,
                                                loggedIn = loggedIn,
                                                archivedKeys = highlights.archivedKeys,
                                                onRead = onRead,
                                                onListen = onListen,
                                                onMarkAsRead = onMarkAsRead,
                                            )
                                        }
                                        HomeSections.OTHERS -> if (others.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(
                                                    if (loggedIn || yours.isNotEmpty() || friends.isNotEmpty()) {
                                                        R.string.home_recently_highlighted_by_others
                                                    } else {
                                                        R.string.home_recently_highlighted
                                                    },
                                                ),
                                                items = others,
                                                rowKey = "others",
                                                tint = nostrverseColor,
                                                loggedIn = loggedIn,
                                                archivedKeys = highlights.archivedKeys,
                                                onRead = onRead,
                                                onListen = onListen,
                                                onMarkAsRead = onMarkAsRead,
                                            )
                                        }
                                        HomeSections.MOST -> if (mostHighlighted.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(R.string.home_most_highlighted),
                                                items = mostHighlighted,
                                                rowKey = "most",
                                                tint = nostrverseColor,
                                                loggedIn = loggedIn,
                                                archivedKeys = highlights.archivedKeys,
                                                onRead = onRead,
                                                onListen = onListen,
                                                onMarkAsRead = onMarkAsRead,
                                            )
                                        }
                                        HomeSections.SHORT -> if (shortReads.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(R.string.home_short_reads),
                                                items = shortReads,
                                                rowKey = "short",
                                                tint = MaterialTheme.colorScheme.primary,
                                                icon = Icons.Outlined.Timer,
                                                loggedIn = loggedIn,
                                                archivedKeys = highlights.archivedKeys,
                                                onRead = onRead,
                                                onListen = onListen,
                                                onMarkAsRead = onMarkAsRead,
                                            )
                                        }
                                        HomeSections.LONG -> if (longReads.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(R.string.home_long_reads),
                                                items = longReads,
                                                rowKey = "long",
                                                tint = MaterialTheme.colorScheme.primary,
                                                icon = Icons.Outlined.AutoStories,
                                                loggedIn = loggedIn,
                                                archivedKeys = highlights.archivedKeys,
                                                onRead = onRead,
                                                onListen = onListen,
                                                onMarkAsRead = onMarkAsRead,
                                            )
                                        }
                                        HomeSections.RANDOM -> if (randomArticles.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(R.string.home_random_articles),
                                                items = randomArticles,
                                                rowKey = "random",
                                                tint = MaterialTheme.colorScheme.primary,
                                                icon = Icons.Outlined.Shuffle,
                                                loggedIn = loggedIn,
                                                archivedKeys = highlights.archivedKeys,
                                                onRead = onRead,
                                                onListen = onListen,
                                                onMarkAsRead = onMarkAsRead,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeLoadingIndicator(modifier: Modifier = Modifier) {
    val messages = stringArrayResource(R.array.home_loading_status)
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(messages) {
        if (messages.size <= 1) return@LaunchedEffect
        // Connecting is quick; show it once, then loop the rest.
        delay(HOME_LOADING_CONNECT_MS)
        index = 1
        while (true) {
            delay(HOME_LOADING_STATUS_MS)
            index = if (index >= messages.lastIndex) 1 else index + 1
        }
    }
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        if (messages.isNotEmpty()) {
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    fadeIn(animationSpec = tween(280)) togetherWith
                        fadeOut(animationSpec = tween(280))
                },
                label = "homeLoadingStatus",
            ) { i ->
                Text(
                    text = messages[i],
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.SansSerif,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private const val HOME_LOADING_CONNECT_MS = 800L
private const val HOME_LOADING_STATUS_MS = 2_200L

@Composable
private fun HomePromptSections(
    showFirstTime: Boolean,
    onDismissFirstTime: () -> Unit,
    onOpenAbout: () -> Unit,
    showLoginPrompt: Boolean,
    onDismissLoginPrompt: () -> Unit,
    onOpenLogin: () -> Unit,
) {
    if (showFirstTime) {
        HomePromptSection(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            title = stringResource(R.string.home_first_time_title),
            body = stringResource(R.string.home_first_time_body),
            cta = stringResource(R.string.home_first_time_cta),
            dismissContentDescription = stringResource(R.string.home_first_time_dismiss),
            onCta = onOpenAbout,
            onDismiss = onDismissFirstTime,
        )
    }
    if (showLoginPrompt) {
        HomePromptSection(
            icon = Icons.AutoMirrored.Outlined.Login,
            title = stringResource(R.string.home_login_title),
            body = stringResource(R.string.home_login_body),
            cta = stringResource(R.string.home_login_cta),
            dismissContentDescription = stringResource(R.string.home_login_dismiss),
            onCta = onOpenLogin,
            onDismiss = onDismissLoginPrompt,
        )
    }
}

@Composable
private fun HomePromptSection(
    icon: ImageVector,
    title: String,
    body: String,
    cta: String,
    dismissContentDescription: String,
    onCta: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = dismissContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onCta,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = cta,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
    }
}

@Composable
private fun HighlightedRow(
    title: String,
    items: List<HighlightedArticle>,
    rowKey: String,
    tint: Color,
    loggedIn: Boolean,
    archivedKeys: Set<String>,
    onRead: (String) -> Unit,
    onListen: (HighlightedArticle) -> Unit,
    onMarkAsRead: (HighlightedArticle) -> Unit,
    icon: ImageVector = BorisIcons.Highlighter,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { "$rowKey:${it.url}" }) { article ->
                HighlightedArticleCard(
                    article = article,
                    fallbackTint = tint,
                    loggedIn = loggedIn,
                    archived = ArchivedArticles.isArchived(article.url, archivedKeys),
                    onOpen = { onRead(article.url) },
                    onListen = { onListen(article) },
                    onMarkAsRead = { onMarkAsRead(article) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HighlightedArticleCard(
    article: HighlightedArticle,
    fallbackTint: Color,
    loggedIn: Boolean,
    archived: Boolean,
    onOpen: () -> Unit,
    onListen: () -> Unit,
    onMarkAsRead: () -> Unit,
) {
    val target = remember(article.url) { NostrLink.parse(article.url) }
    val actionMenuLabel = stringResource(R.string.home_article_actions)
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(modifier = Modifier.width(CardWidth)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = { menuOpen = true },
                    onLongClickLabel = actionMenuLabel,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val warning = remember(article.url, article.title) {
                SensitiveContent.classify(article)
            }
            Box(
                modifier = Modifier
                    .size(CardImageSize)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (article.imageUrl.isNullOrBlank()) {
                    val note = target is NostrTarget.Note
                    Icon(
                        imageVector = if (note) {
                            Icons.AutoMirrored.Outlined.StickyNote2
                        } else {
                            Icons.AutoMirrored.Outlined.Article
                        },
                        contentDescription = null,
                        tint = fallbackTint,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = article.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (warning != null) Modifier.blur(10.dp) else Modifier),
                    )
                }
            }
            Text(
                text = article.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = article.host,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (warning != null) {
                NsfwBadge(warning)
            }
            CardReadingProgress(url = article.url)
        }
        ArticleActionsMenu(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            title = article.title,
            url = article.url,
            loggedIn = loggedIn,
            archived = archived,
            onListen = onListen,
            onMarkAsRead = onMarkAsRead,
        )
    }
}

@Composable
private fun StatusMessage(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRetry, shape = RoundedCornerShape(8.dp)) {
            Text(stringResource(R.string.feed_retry))
        }
    }
}

@Composable
private fun HomeFilterMenu(settings: UserSettings) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = stringResource(R.string.home_filters),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
        ) {
            FilterToggle(
                label = stringResource(R.string.home_hide_archived),
                checked = settings.hideArchivedOnHome,
                onCheckedChange = {
                    SettingsSync.apply(settings.withBoolean("hideArchivedOnHome", it))
                },
            )
            FilterToggle(
                label = stringResource(R.string.home_hide_completed),
                checked = settings.hideCompletedOnHome,
                onCheckedChange = {
                    SettingsSync.apply(settings.withBoolean("hideCompletedOnHome", it))
                },
            )
            FilterToggle(
                label = stringResource(R.string.home_hide_nsfw),
                checked = settings.hideNsfwOnHome,
                onCheckedChange = {
                    SettingsSync.apply(settings.withBoolean("hideNsfwOnHome", it))
                },
            )
        }
    }
}

@Composable
private fun FilterToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
        },
        onClick = { onCheckedChange(!checked) },
    )
}

private val CardWidth = 140.dp
private val CardImageSize = 140.dp
