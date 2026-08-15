package org.dergigi.boris.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.dergigi.boris.ui.account.AccountScreen
import org.dergigi.boris.ui.auth.AuthViewModel
import org.dergigi.boris.ui.home.HomeScreen
import org.dergigi.boris.ui.reader.ReaderScreen
import org.dergigi.boris.ui.reader.ReaderViewModel
import org.dergigi.boris.ui.shell.BorisBottomBar
import org.dergigi.boris.ui.shell.MainTab
import org.dergigi.boris.ui.shell.StubScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val FEED = "feed"
    const val SEARCH = "search"
    const val ACCOUNT = "account"
    const val READER = "reader?url={${ReaderViewModel.URL_ARG}}"

    fun reader(url: String): String {
        val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.name())
        return "reader?url=$encoded"
    }
}

@Composable
fun BorisApp(
    incomingUrl: String? = null,
    incomingBunker: String? = null,
    authViewModel: AuthViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val pictureUrl by authViewModel.pictureUrl.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val selectedTab = MainTab.entries.firstOrNull { it.route == currentRoute }
    val showBar = selectedTab != null

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
            navController.navigate(Routes.reader(incomingUrl)) {
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(incomingBunker) {
        if (!incomingBunker.isNullOrBlank()) {
            goToTab(MainTab.Account)
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
                    BorisBottomBar(
                        selected = selectedTab,
                        pictureUrl = pictureUrl,
                        onSelect = ::goToTab,
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onRead = { url -> navController.navigate(Routes.reader(url)) },
                    )
                }
                composable(Routes.LIBRARY) {
                    StubScreen(stringResource(MainTab.Library.labelRes))
                }
                composable(Routes.FEED) {
                    StubScreen(stringResource(MainTab.Feed.labelRes))
                }
                composable(Routes.SEARCH) {
                    StubScreen(stringResource(MainTab.Search.labelRes))
                }
                composable(Routes.ACCOUNT) {
                    AccountScreen(
                        incomingBunker = incomingBunker,
                        viewModel = authViewModel,
                    )
                }
                composable(
                    route = Routes.READER,
                    arguments = listOf(
                        navArgument(ReaderViewModel.URL_ARG) { type = NavType.StringType },
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
                    )
                }
            }
        }
    }
}
