package org.dergigi.boris.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    secondary = Sky400,
    onSecondary = Zinc900,
    background = Zinc900,
    onBackground = Zinc200,
    surface = Zinc900,
    onSurface = Zinc200,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc400,
    outline = Zinc700,
    outlineVariant = Zinc700,
)

private val LightColors = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    secondary = Blue500,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Gray900,
    surface = Paper,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    outline = Gray200,
    outlineVariant = Gray200,
)

@Composable
fun BorisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = BorisTypography,
        content = content,
    )
}
