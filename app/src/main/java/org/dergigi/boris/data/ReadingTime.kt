package org.dergigi.boris.data

import kotlin.math.max
import kotlin.math.roundToInt

object ReadingTime {
    const val WORDS_PER_MINUTE = 200
    const val SHORT_MAX_MINUTES = 5
    const val LONG_MIN_MINUTES = 15

    fun minutes(text: String): Int? {
        val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
        if (words == 0) return null
        return max(1, (words / WORDS_PER_MINUTE.toDouble()).roundToInt())
    }

    fun label(minutes: Int): String =
        if (minutes == 1) "1 min read" else "$minutes min read"

    fun labelFor(text: String): String? = minutes(text)?.let(::label)
}
