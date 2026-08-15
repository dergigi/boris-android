package org.dergigi.boris.data

object HexColor {
    fun argb(hex: String): Int? {
        val h = hex.trim().removePrefix("#")
        if (h.length != 6) return null
        val rgb = h.toIntOrNull(16) ?: return null
        return (0xFF shl 24) or rgb
    }

    fun argb(hex: String, fallback: Int): Int = argb(hex) ?: fallback
}
