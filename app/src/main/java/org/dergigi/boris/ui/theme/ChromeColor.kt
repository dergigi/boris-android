package org.dergigi.boris.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

object ChromeColor {
    fun of(accent: Color, surface: Color, minRatio: Float = 3f): Color {
        if (contrast(accent, surface) >= minRatio) return accent
        val toward = if (surface.luminance() > 0.5f) Color.Black else Color.White
        var t = 0.08f
        var best = accent
        while (t <= 0.85f) {
            val candidate = lerp(accent, toward, t)
            best = candidate
            if (contrast(candidate, surface) >= minRatio) return candidate
            t += 0.08f
        }
        return best
    }

    fun contrast(a: Color, b: Color): Float {
        val high = maxOf(a.luminance(), b.luminance())
        val low = minOf(a.luminance(), b.luminance())
        return (high + 0.05f) / (low + 0.05f)
    }
}
