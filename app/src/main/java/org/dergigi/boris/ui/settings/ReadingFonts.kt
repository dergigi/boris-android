package org.dergigi.boris.ui.settings

import androidx.compose.ui.text.font.FontFamily
import org.dergigi.boris.ui.theme.SourceSerif

data class ReadingFont(
    val id: String,
    val label: String,
    val serif: Boolean,
)

object ReadingFonts {
    val ALL = listOf(
        ReadingFont("system", "System Default", serif = false),
        ReadingFont("inter", "Inter", serif = false),
        ReadingFont("lora", "Lora", serif = true),
        ReadingFont("merriweather", "Merriweather", serif = true),
        ReadingFont("open-sans", "Open Sans", serif = false),
        ReadingFont("roboto", "Roboto", serif = false),
        ReadingFont("source-serif-4", "Source Serif 4", serif = true),
        ReadingFont("crimson-text", "Crimson Text", serif = true),
        ReadingFont("libre-baskerville", "Libre Baskerville", serif = true),
        ReadingFont("pt-serif", "PT Serif", serif = true),
    )

    val SIZES = listOf(16, 18, 21, 24, 28, 32)

    val HIGHLIGHT_COLORS = listOf(
        "#fde047",
        "#f97316",
        "#ec4899",
        "#22c55e",
        "#3b82f6",
        "#9333ea",
    )

    val LINK_COLORS_DARK = listOf(
        "#38bdf8",
        "#22d3ee",
        "#60a5fa",
        "#818cf8",
        "#3b82f6",
        "#9333ea",
    )

    val LINK_COLORS_LIGHT = listOf(
        "#3b82f6",
        "#6366f1",
        "#9333ea",
        "#0ea5e9",
        "#06b6d4",
        "#14b8a6",
    )

    fun family(id: String): FontFamily = when (id) {
        "source-serif-4" -> SourceSerif
        "lora", "merriweather", "crimson-text", "libre-baskerville", "pt-serif" -> FontFamily.Serif
        else -> FontFamily.SansSerif
    }

    fun label(id: String): String = ALL.firstOrNull { it.id == id }?.label ?: "Source Serif 4"
}
