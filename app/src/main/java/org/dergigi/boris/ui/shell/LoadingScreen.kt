package org.dergigi.boris.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import org.dergigi.boris.R

private val SplashPaper = Color(0xFFE6E6E4)

private val SplashQuotes = intArrayOf(
    R.drawable.splash_quote_01,
    R.drawable.splash_quote_02,
    R.drawable.splash_quote_03,
    R.drawable.splash_quote_04,
    R.drawable.splash_quote_05,
    R.drawable.splash_quote_06,
    R.drawable.splash_quote_07,
    R.drawable.splash_quote_08,
)

/**
 * Full-bleed cold-start screen. Picks one quote photo at random per show.
 */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    val quote = remember { SplashQuotes.random() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashPaper),
    ) {
        Image(
            painter = painterResource(quote),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
