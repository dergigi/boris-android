package org.dergigi.boris.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.data.IncomingShare
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.ui.reader.PendingHighlight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.NostrTarget
import org.dergigi.boris.nostr.HintedRelays
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.ui.about.AboutLinks
import org.dergigi.boris.ui.about.AboutScreen
import org.dergigi.boris.ui.account.AccountScreen
import org.dergigi.boris.ui.auth.AuthUiState
import org.dergigi.boris.ui.auth.AuthViewModel
import org.dergigi.boris.ui.feed.FeedScreen
import org.dergigi.boris.ui.home.HomeScreen
import org.dergigi.boris.ui.home.HomeViewModel
import org.dergigi.boris.ui.library.LibraryScreen
import org.dergigi.boris.ui.reader.ReaderFocus
import org.dergigi.boris.ui.reader.ReaderScreen
import org.dergigi.boris.ui.reader.ReaderViewModel
import org.dergigi.boris.ui.search.SearchScreen
import org.dergigi.boris.ui.settings.SettingsCategory
import org.dergigi.boris.ui.settings.SettingsScreen
import org.dergigi.boris.ui.shell.BorisBottomBar
import org.dergigi.boris.ui.shell.MainTab
import org.dergigi.boris.ui.shell.TtsMiniPlayerHost
import org.dergigi.boris.ui.support.SupportScreen
import org.dergigi.boris.ui.you.ProfileScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val FEED = "feed"
    const val SEARCH = "search"
    const val YOU = "you"
    const val SETTINGS_CATEGORY_ARG = "category"
    const val SETTINGS = "settings?category={$SETTINGS_CATEGORY_ARG}"
    const val ABOUT = "about"
    const val SUPPORT = "support"
    const val NPUB_ARG = "npub"
    const val PROFILE = "profile/{$NPUB_ARG}"
    const val READER = "reader?url={${ReaderViewModel.URL_ARG}}&highlight={${ReaderViewModel.HIGHLIGHT_ARG}}"

    fun reader(url: String, highlightId: String? = null, quote: String? = null): String {
        if (!highlightId.isNullOrBlank()) {
            ReaderFocus.offer(highlightId, quote.orEmpty())
        }
        val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.name())
        val hid = URLEncoder.encode(highlightId.orEmpty(), StandardCharsets.UTF_8.name())
        return "reader?url=$encoded&highlight=$hid"
    }

    fun profile(npub: String): String = "profile/$npub"

    fun open(url: String): String =
        when (val target = NostrLink.parse(url)) {
            is NostrTarget.Profile -> {
                HintedRelays.remember(target.pubkeyHex, target.relays)
                profile(Nip19.npubEncode(target.pubkeyHex))
            }
            else -> reader(url)
        }

    fun settings(category: SettingsCategory? = null): String =
        if (category == null) "settings" else "settings?category=${category.name}"
}

@Composable
fun BorisApp(
    incomingUrl: String? = null,
    incomingBunker: String? = null,
    incomingHighlight: IncomingShare? = null,
    onIncomingUrlConsumed: () -> Unit = {},
    onIncomingBunkerConsumed: () -> Unit = {},
    onIncomingHighlightConsumed: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val pictureUrl by authViewModel.pictureUrl.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val selectedTab = MainTab.entries.firstOrNull { it.route == currentRoute }
    val showBar = selectedTab != null
    // D-17: the speaking article's reader owns play/pause; hide the mini player there.
    val currentArticleUrl = if (currentRoute == Routes.READER) {
        backStack?.arguments?.getString(ReaderViewModel.URL_ARG)
    } else {
        null
    }

    fun openSpeakingArticle(url: String) {
        navController.navigate(Routes.reader(url)) {
            launchSingleTop = true
        }
    }

    fun openUrl(url: String, singleTop: Boolean = false) {
        navController.navigate(Routes.open(url)) {
            if (singleTop) launchSingleTop = true
        }
    }

    fun openHighlight(url: String, quote: String) {
        PendingHighlight.offer(quote)
        if (currentRoute == Routes.READER) {
            navController.popBackStack()
        }
        openUrl(url)
    }

    var highlightPromptQuote by rememberSaveable { mutableStateOf<String?>(null) }
    var highlightLoginPromptKey by rememberSaveable { mutableStateOf<String?>(null) }

    fun goToTab(tab: MainTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(incomingUrl) {
        if (!incomingUrl.isNullOrBlank() && !incomingUrl.trim().startsWith("bunker:", ignoreCase = true)) {
            openUrl(incomingUrl, singleTop = true)
            onIncomingUrlConsumed()
        }
    }
    LaunchedEffect(incomingBunker) {
        if (!incomingBunker.isNullOrBlank()) {
            goToTab(MainTab.You)
        }
    }
    LaunchedEffect(incomingHighlight, authState) {
        val incoming = incomingHighlight ?: return@LaunchedEffect
        val quote = incoming.highlightQuote?.trim().orEmpty()
        if (quote.isBlank()) return@LaunchedEffect
        val loginPromptKey = quote + "\n" + incoming.url.orEmpty()
        if (authState !is AuthUiState.LoggedIn) {
            if (highlightLoginPromptKey != loginPromptKey) {
                Toast.makeText(context, R.string.highlight_sign_in, Toast.LENGTH_SHORT).show()
                goToTab(MainTab.You)
                highlightLoginPromptKey = loginPromptKey
            }
            return@LaunchedEffect
        }
        highlightLoginPromptKey = null
        onIncomingHighlightConsumed()
        val url = incoming.url?.trim().orEmpty()
        if (url.isNotBlank()) {
            openHighlight(url, quote)
        } else {
            highlightPromptQuote = incoming.highlightQuote
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = if (showBar) {
                ScaffoldDefaults.contentWindowInsets
            } else {
                WindowInsets(0)
            },
            bottomBar = {
                if (selectedTab != null) {
                    Column {
                        TtsMiniPlayerHost(
                            currentArticleUrl = currentArticleUrl,
                            onOpenArticle = ::openSpeakingArticle,
                        )
                        BorisBottomBar(
                            selected = selectedTab,
                            pictureUrl = pictureUrl,
                            onSelect = ::goToTab,
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onRead = { url -> openUrl(url) },
                        onOpenAbout = {
                            navController.navigate(Routes.ABOUT) {
                                launchSingleTop = true
                            }
                        },
                        onOpenSupport = {
                            navController.navigate(Routes.SUPPORT) {
                                launchSingleTop = true
                            }
                        },
                        onOpenProfile = { pubkeyHex ->
                            runCatching { Nip19.npubEncode(pubkeyHex) }.getOrNull()?.let { npub ->
                                navController.navigate(Routes.profile(npub))
                            }
                        },
                        onOpenLogin = { goToTab(MainTab.You) },
                        onOpenHomeSettings = {
                            navController.navigate(Routes.settings(SettingsCategory.Home)) {
                                launchSingleTop = true
                            }
                        },
                        viewModel = homeViewModel,
                        authViewModel = authViewModel,
                    )
                }
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        onOpenArticle = { url -> navController.navigate(Routes.reader(url)) },
                        onOpenLibrarySettings = {
                            navController.navigate(Routes.settings(SettingsCategory.Library)) {
                                launchSingleTop = true
                            }
                        },
                        authViewModel = authViewModel,
                    )
                }
                composable(Routes.FEED) {
                    FeedScreen(
                        onOpenArticle = { url -> navController.navigate(Routes.reader(url)) },
                        onOpenHighlight = { url, id, quote ->
                            navController.navigate(Routes.reader(url, id, quote))
                        },
                        onOpenProfile = { pubkeyHex ->
                            runCatching { Nip19.npubEncode(pubkeyHex) }.getOrNull()?.let { npub ->
                                navController.navigate(Routes.profile(npub))
                            }
                        },
                        onOpenFeedSettings = {
                            navController.navigate(Routes.settings(SettingsCategory.Feed)) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(Routes.SEARCH) {
                    SearchScreen(
                        onOpenArticle = { url -> navController.navigate(Routes.reader(url)) },
                        onOpenHighlight = { url, id, quote ->
                            navController.navigate(Routes.reader(url, id, quote))
                        },
                        onOpenProfile = { pubkeyHex ->
                            runCatching { Nip19.npubEncode(pubkeyHex) }.getOrNull()?.let { npub ->
                                navController.navigate(Routes.profile(npub))
                            }
                        },
                    )
                }
                composable(Routes.YOU) {
                    AccountScreen(
                        incomingBunker = incomingBunker,
                        onIncomingBunkerConsumed = onIncomingBunkerConsumed,
                        viewModel = authViewModel,
                        onOpenSettings = {
                            navController.navigate(Routes.settings()) {
                                launchSingleTop = true
                            }
                        },
                        onOpenSupport = {
                            navController.navigate(Routes.SUPPORT) {
                                launchSingleTop = true
                            }
                        },
                        onOpenProfile = { pubkeyHex ->
                            runCatching { Nip19.npubEncode(pubkeyHex) }.getOrNull()?.let { npub ->
                                navController.navigate(Routes.profile(npub))
                            }
                        },
                        onOpenArticle = { url -> navController.navigate(Routes.reader(url)) },
                        onOpenHighlight = { url, id, quote ->
                            navController.navigate(Routes.reader(url, id, quote))
                        },
                    )
                }
                composable(
                    route = Routes.SETTINGS,
                    arguments = listOf(
                        navArgument(Routes.SETTINGS_CATEGORY_ARG) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenArticle = { url -> navController.navigate(Routes.reader(url)) },
                        onOpenTutorial = {
                            navController.navigate(Routes.ABOUT) {
                                launchSingleTop = true
                            }
                        },
                        onOpenSupport = {
                            navController.navigate(Routes.SUPPORT) {
                                launchSingleTop = true
                            }
                        },
                        onOpenAuthorProfile = {
                            navController.navigate(Routes.profile(AboutLinks.AUTHOR_NPUB)) {
                                launchSingleTop = true
                            }
                        },
                        initialCategory = entry.arguments?.getString(Routes.SETTINGS_CATEGORY_ARG),
                    )
                }
                composable(Routes.ABOUT) {
                    AboutScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.SUPPORT) {
                    SupportScreen(
                        onBack = { navController.popBackStack() },
                        onOpenProfile = { pubkeyHex ->
                            runCatching { Nip19.npubEncode(pubkeyHex) }.getOrNull()?.let { npub ->
                                navController.navigate(Routes.profile(npub))
                            }
                        },
                    )
                }
                composable(
                    route = Routes.PROFILE,
                    arguments = listOf(
                        navArgument(Routes.NPUB_ARG) { type = NavType.StringType },
                    ),
                ) { entry ->
                    val npub = entry.arguments?.getString(Routes.NPUB_ARG).orEmpty()
                    ProfileScreen(
                        npub = npub,
                        onBack = { navController.popBackStack() },
                        onOpenArticle = { url -> navController.navigate(Routes.reader(url)) },
                        onOpenHighlight = { url, id, quote ->
                            navController.navigate(Routes.reader(url, id, quote))
                        },
                    )
                }
                composable(
                    route = Routes.READER,
                    arguments = listOf(
                        navArgument(ReaderViewModel.URL_ARG) { type = NavType.StringType },
                        navArgument(ReaderViewModel.HIGHLIGHT_ARG) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
                ) {
                    ReaderScreen(
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.HOME) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onOpenArticle = { url -> navController.navigate(Routes.reader(url)) },
                        onOpenProfile = { pubkeyHex ->
                            runCatching { Nip19.npubEncode(pubkeyHex) }.getOrNull()?.let { npub ->
                                navController.navigate(Routes.profile(npub))
                            }
                        },
                        onOpenReaderSettings = {
                            navController.navigate(Routes.settings(SettingsCategory.Reading)) {
                                launchSingleTop = true
                            }
                        },
                        onOpenHighlightSettings = {
                            navController.navigate(Routes.settings(SettingsCategory.Highlights)) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
            // Off-tab screens without a bottom bar overlay the mini player, except
            // the reader: it stacks the player above the progress bar itself.
            if (selectedTab == null && currentRoute != Routes.READER) {
                TtsMiniPlayerHost(
                    currentArticleUrl = currentArticleUrl,
                    onOpenArticle = ::openSpeakingArticle,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                )
            }
            }
        }
        highlightPromptQuote?.let { quote ->
            HighlightUrlDialog(
                quote = quote,
                onConfirm = { url ->
                    highlightPromptQuote = null
                    openHighlight(url, quote)
                },
                onDismiss = { highlightPromptQuote = null },
            )
        }
    }
}

@Composable
private fun HighlightUrlDialog(
    quote: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by rememberSaveable { mutableStateOf("") }
    val resolved = UrlExtractor.extract(url)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.highlight_with_boris)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (quote.isNotBlank()) {
                    Text(quote)
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.highlight_url_title)) },
                    placeholder = { Text(stringResource(R.string.highlight_url_hint)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { resolved?.let(onConfirm) },
                enabled = resolved != null,
            ) {
                Text(stringResource(R.string.highlight_url_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
