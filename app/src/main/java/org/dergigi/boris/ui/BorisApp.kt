package org.dergigi.boris.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.dergigi.boris.ui.home.HomeScreen
import org.dergigi.boris.ui.reader.ReaderScreen
import org.dergigi.boris.ui.reader.ReaderViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val HOME = "home"
    const val READER = "reader?url={${ReaderViewModel.URL_ARG}}"

    fun reader(url: String): String {
        val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.name())
        return "reader?url=$encoded"
    }
}

@Composable
fun BorisApp(incomingUrl: String? = null) {
    val navController = rememberNavController()

    LaunchedEffect(incomingUrl) {
        if (!incomingUrl.isNullOrBlank()) {
            navController.navigate(Routes.reader(incomingUrl)) {
                launchSingleTop = true
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onRead = { url -> navController.navigate(Routes.reader(url)) },
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
