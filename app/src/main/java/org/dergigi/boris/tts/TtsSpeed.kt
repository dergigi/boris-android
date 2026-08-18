package org.dergigi.boris.tts

import kotlin.math.abs

object TtsSpeed {
    val PRESETS = listOf(0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.1, 2.4, 2.8, 3.0)
    const val DEFAULT = 2.1

    fun snap(current: Double): Double =
        PRESETS.minByOrNull { abs(it - current) } ?: DEFAULT

    fun cycle(current: Double): Double {
        val index = PRESETS.indexOf(snap(current))
        return PRESETS[(index + 1) % PRESETS.size]
    }
}
