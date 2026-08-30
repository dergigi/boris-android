package org.dergigi.boris.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dergigi.boris.data.DisplayType
import org.dergigi.boris.data.DisplayTypeStore
import org.dergigi.boris.data.HexColor
import org.dergigi.boris.data.UserSettings

data class DisplayLook(
    val type: DisplayType,
    val mine: Color,
    val friends: Color,
    val foaf: Color,
    val nostrverse: Color,
    val link: Color,
    val underline: Boolean,
) {
    val eink: Boolean get() = type.eink
}

fun resolveDisplayLook(
    settings: UserSettings,
    type: DisplayType,
    dark: Boolean,
): DisplayLook {
    if (type.eink) {
        return if (dark) {
            DisplayLook(
                type = type,
                mine = EinkInkDark,
                friends = EinkFriendsDark,
                foaf = EinkFoafDark,
                nostrverse = EinkNostrverseDark,
                link = Color.White,
                underline = true,
            )
        } else {
            DisplayLook(
                type = type,
                mine = EinkInk,
                friends = EinkFriends,
                foaf = EinkFoaf,
                nostrverse = EinkNostrverse,
                link = Color.Black,
                underline = true,
            )
        }
    }
    return DisplayLook(
        type = type,
        mine = parseColor(settings.highlightColorMine, HighlightMine),
        friends = parseColor(settings.highlightColorFriends, HighlightFriends),
        foaf = parseColor(settings.highlightColorFoaf, HighlightFoaf),
        nostrverse = parseColor(settings.highlightColorNostrverse, HighlightOther),
        link = parseColor(
            if (dark) settings.linkColorDark else settings.linkColorLight,
            if (dark) Sky400 else Blue500,
        ),
        underline = !settings.markerStyle,
    )
}

@Composable
fun rememberDisplayLook(settings: UserSettings): DisplayLook {
    val type by DisplayTypeStore.type.collectAsStateWithLifecycle()
    val dark = settings.isDark(isSystemInDarkTheme())
    return remember(
        type,
        dark,
        settings.highlightColorMine,
        settings.highlightColorFriends,
        settings.highlightColorFoaf,
        settings.highlightColorNostrverse,
        settings.highlightStyle,
        settings.linkColorDark,
        settings.linkColorLight,
    ) {
        resolveDisplayLook(settings, type, dark)
    }
}

private fun parseColor(hex: String, fallback: Color): Color {
    val argb = HexColor.argb(hex) ?: return fallback
    return Color(argb)
}
