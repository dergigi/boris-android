package org.dergigi.boris.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dergigi.boris.data.DisplayType
import org.dergigi.boris.data.DisplayTypeStore
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.data.UserSettings

@Composable
fun BorisTheme(
    content: @Composable () -> Unit,
) {
    val settings by SettingsSync.settings.collectAsStateWithLifecycle()
    val displayType by DisplayTypeStore.type.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = settings.isDark(systemDark)
    val colors = borisColorScheme(settings, darkTheme, displayType)
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

fun borisColorScheme(
    settings: UserSettings,
    darkTheme: Boolean,
    displayType: DisplayType = DisplayType.Color,
): ColorScheme {
    if (displayType.eink) return einkScheme(darkTheme)
    return if (darkTheme) darkScheme(settings.darkColorTheme) else lightScheme(settings.lightColorTheme)
}

private fun einkScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            secondary = Color.White,
            onSecondary = Color.Black,
            background = Black,
            onBackground = Color.White,
            surface = Black,
            onSurface = Color.White,
            surfaceVariant = BlackElevated,
            onSurfaceVariant = EinkMutedDark,
            outline = EinkLineDark,
            outlineVariant = EinkLineDark,
        )
    } else {
        lightColorScheme(
            primary = Color.Black,
            onPrimary = Color.White,
            secondary = Color.Black,
            onSecondary = Color.White,
            background = Paper,
            onBackground = Color.Black,
            surface = Paper,
            onSurface = Color.Black,
            surfaceVariant = Gray100,
            onSurfaceVariant = EinkMuted,
            outline = EinkLine,
            outlineVariant = EinkLine,
        )
    }
}

private fun darkScheme(variant: String): ColorScheme {
    val background: Color
    val surface: Color
    val outline: Color
    val outlineVariant: Color
    when (variant) {
        "black" -> {
            background = Black
            surface = BlackElevated
            outline = BlackBorder
            outlineVariant = BlackBorderSubtle
        }
        "charcoal" -> {
            background = Charcoal
            surface = CharcoalElevated
            outline = CharcoalBorder
            outlineVariant = CharcoalBorderSubtle
        }
        else -> {
            background = Zinc900
            surface = Zinc800
            outline = Zinc700
            outlineVariant = Zinc700
        }
    }
    return darkColorScheme(
        primary = Indigo500,
        onPrimary = Color.White,
        secondary = Sky400,
        onSecondary = Zinc900,
        background = background,
        onBackground = Zinc200,
        surface = background,
        onSurface = Zinc200,
        surfaceVariant = surface,
        onSurfaceVariant = Zinc400,
        outline = outline,
        outlineVariant = outlineVariant,
    )
}

private fun lightScheme(variant: String): ColorScheme {
    val background: Color
    val surface: Color
    val outline: Color
    val onBackground: Color
    val onSurfaceVariant: Color
    when (variant) {
        "paper-white" -> {
            background = Paper
            surface = Gray100
            outline = Gray200
            onBackground = Gray900
            onSurfaceVariant = Gray700
        }
        "ivory" -> {
            background = Ivory
            surface = IvoryElevated
            outline = IvoryBorder
            onBackground = IvoryText
            onSurfaceVariant = IvorySecondary
        }
        else -> {
            background = Sepia
            surface = SepiaElevated
            outline = SepiaBorder
            onBackground = SepiaText
            onSurfaceVariant = SepiaSecondary
        }
    }
    return lightColorScheme(
        primary = Indigo600,
        onPrimary = Color.White,
        secondary = Blue500,
        onSecondary = Color.White,
        background = background,
        onBackground = onBackground,
        surface = background,
        onSurface = onBackground,
        surfaceVariant = surface,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outline,
    )
}
