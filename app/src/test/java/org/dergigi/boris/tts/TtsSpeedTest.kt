package org.dergigi.boris.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class TtsSpeedTest {
    @Test
    fun presetsMatchWebappList() {
        assertEquals(
            listOf(0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.1, 2.4, 2.8, 3.0),
            TtsSpeed.PRESETS,
        )
        assertEquals(2.1, TtsSpeed.DEFAULT, 0.0)
    }

    @Test
    fun cycleWalksThePresetList() {
        assertEquals(1.0, TtsSpeed.cycle(0.8), 0.0)
        assertEquals(2.4, TtsSpeed.cycle(2.1), 0.0)
        assertEquals(0.8, TtsSpeed.cycle(3.0), 0.0)
    }

    @Test
    fun cycleOfIntegerLookingOneAdvancesToNextPreset() {
        assertEquals(1.2, TtsSpeed.cycle(1.0), 0.0)
    }

    @Test
    fun cycleSnapsUnknownValueThenAdvances() {
        // 2.3 snaps to 2.4 (nearest preset), then advances to 2.8.
        assertEquals(2.8, TtsSpeed.cycle(2.3), 0.0)
    }

    @Test
    fun snapReturnsNearestPreset() {
        assertEquals(2.1, TtsSpeed.snap(2.1), 0.0)
        assertEquals(2.1, TtsSpeed.snap(2.15), 0.0)
        assertEquals(0.8, TtsSpeed.snap(0.1), 0.0)
        assertEquals(3.0, TtsSpeed.snap(9.9), 0.0)
    }
}
