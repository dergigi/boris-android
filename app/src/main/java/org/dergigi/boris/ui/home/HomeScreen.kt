package org.dergigi.boris.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.dergigi.boris.R
import org.dergigi.boris.data.ArchivedArticles
import org.dergigi.boris.data.ClipboardLink
import org.dergigi.boris.data.HighlightedArticle
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
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightMine
import org.dergigi.boris.ui.theme.HighlightOther

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRead: (String) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenLogin: () -> Unit = {},
    onOpenHomeSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
) {
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val loggedIn = authState is AuthUiState.LoggedIn
    val context = LocalContext.current
    var showFirstTime by remember {
        mutableStateOf(!HomeOnboardingStore.isFirstTimeDismissed(context))
    }
    var loginPromptDismissed by remember {
        mutableStateOf(HomeOnboardingStore.isLoginDismissed(context))
    }
    val showLoginPrompt = !loggedIn && !loginPromptDismissed
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
        if (HomeOnboardingStore.isFirstTimeDismissed(context)) {
            showFirstTime = false
        }
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
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = stringResource(R.string.home_about),
                        )
                    }
                    TopBarMoreMenu(
                        items = buildList {
                            if (loggedIn) {
                                add(
                                    TopBarMenuItem(
                                        label = stringResource(
                                            if (settings.hideArchivedOnHome) {
                                                R.string.home_show_archived
                                            } else {
                                                R.string.home_hide_archived
                                            },
                                        ),
                                        icon = BorisIcons.Books,
                                        onClick = {
                                            SettingsSync.apply(
                                                settings.withBoolean(
                                                    "hideArchivedOnHome",
                                                    !settings.hideArchivedOnHome,
                                                ),
                                            )
                                        },
                                    ),
                                )
                            }
                            add(
                                TopBarMenuItem(
                                    label = stringResource(R.string.home_settings),
                                    icon = Icons.Outlined.Settings,
                                    onClick = onOpenHomeSettings,
                                ),
                            )
                        },
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
                sectionOrder = HomeSections.order(settings.homeSectionOrder),
                mineColor = hexColor(settings.highlightColorMine, HighlightMine),
                friendsColor = hexColor(settings.highlightColorFriends, HighlightFriends),
                nostrverseColor = hexColor(settings.highlightColorNostrverse, HighlightOther),
                showFirstTime = showFirstTime,
                onDismissFirstTime = {
                    HomeOnboardingStore.dismissFirstTime(context)
                    showFirstTime = false
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
                text = url.removePrefix("https://").removePrefix("http://"),
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
    mineColor: Color,
    friendsColor: Color,
    nostrverseColor: Color,
    onRefresh: () -> Unit,
    onRead: (String) -> Unit,
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
                        CircularProgressIndicator()
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
                val yours = ArchivedArticles.visible(
                    highlights.yours,
                    highlights.archivedKeys,
                    hideArchived,
                )
                val friends = ArchivedArticles.visible(
                    highlights.friends,
                    highlights.archivedKeys,
                    hideArchived,
                )
                val others = ArchivedArticles.visible(
                    highlights.others,
                    highlights.archivedKeys,
                    hideArchived,
                )
                val continueReading = ArchivedArticles.visible(
                    highlights.continueReading,
                    highlights.archivedKeys,
                    hideArchived,
                )
                val mostHighlighted = ArchivedArticles.visible(
                    highlights.mostHighlighted,
                    highlights.archivedKeys,
                    hideArchived,
                )
                val randomArticles = ArchivedArticles.visible(
                    highlights.randomArticles,
                    highlights.archivedKeys,
                    hideArchived,
                )
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val empty = yours.isEmpty() && friends.isEmpty() && others.isEmpty() &&
                        continueReading.isEmpty() && mostHighlighted.isEmpty() &&
                        randomArticles.isEmpty()
                    if (empty && !hasPrompts) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            StatusMessage(
                                text = stringResource(
                                    if (hideArchived && highlights.archivedKeys.isNotEmpty()) {
                                        R.string.home_empty_archived
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
                                            if (hideArchived && highlights.archivedKeys.isNotEmpty()) {
                                                R.string.home_empty_archived
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
                                                onRead = onRead,
                                            )
                                        }
                                        HomeSections.YOURS -> if (yours.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(R.string.home_recently_highlighted_by_you),
                                                items = yours,
                                                rowKey = "you",
                                                tint = mineColor,
                                                onRead = onRead,
                                            )
                                        }
                                        HomeSections.FRIENDS -> if (friends.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(R.string.home_recently_highlighted_by_friends),
                                                items = friends,
                                                rowKey = "friends",
                                                tint = friendsColor,
                                                onRead = onRead,
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
                                                onRead = onRead,
                                            )
                                        }
                                        HomeSections.MOST -> if (mostHighlighted.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(R.string.home_most_highlighted),
                                                items = mostHighlighted,
                                                rowKey = "most",
                                                tint = nostrverseColor,
                                                onRead = onRead,
                                            )
                                        }
                                        HomeSections.RANDOM -> if (randomArticles.isNotEmpty()) {
                                            HighlightedRow(
                                                title = stringResource(R.string.home_random_articles),
                                                items = randomArticles,
                                                rowKey = "random",
                                                tint = MaterialTheme.colorScheme.primary,
                                                icon = Icons.Outlined.Shuffle,
                                                onRead = onRead,
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
    onRead: (String) -> Unit,
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
                    onOpen = { onRead(article.url) },
                )
            }
        }
    }
}

@Composable
private fun HighlightedArticleCard(
    article: HighlightedArticle,
    fallbackTint: Color,
    onOpen: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .width(CardWidth)
            .clickable(onClick = onOpen),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(CardImageSize)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (article.imageUrl.isNullOrBlank()) {
                val note = NostrLink.parse(article.url) is NostrTarget.Note
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
                    modifier = Modifier.fillMaxSize(),
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
        CardReadingProgress(url = article.url)
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

private val CardWidth = 140.dp
private val CardImageSize = 140.dp
