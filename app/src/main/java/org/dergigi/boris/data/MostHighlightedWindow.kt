package org.dergigi.boris.data

enum class MostHighlightedWindow(
    val id: String,
    val seconds: Long,
    val fetchLimit: Int,
) {
    Day("24h", 24L * 60 * 60, fetchLimit = 120),
    Week("7d", 7L * 24 * 60 * 60, fetchLimit = 240),
    Month("30d", 30L * 24 * 60 * 60, fetchLimit = 400),
    ;

    fun since(nowSeconds: Long = System.currentTimeMillis() / 1000): Long = nowSeconds - seconds

    companion object {
        val DEFAULT = Week

        fun fromId(id: String?): MostHighlightedWindow =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
