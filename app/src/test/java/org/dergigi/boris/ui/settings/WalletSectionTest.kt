package org.dergigi.boris.ui.settings

import org.dergigi.boris.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletSectionTest {
    @Test
    fun parseZapPresetInputKeepsDistinctValidAmounts() {
        assertEquals(
            listOf(21L, 100L, 1_000L, 5_000L),
            parseZapPresetInput("21, 100, 1k, 100, 5_000"),
        )
    }

    @Test
    fun parseZapPresetInputRejectsOverflowBeforeMultiplying() {
        assertEquals(emptyList<Long>(), parseZapPresetInput("18446744073709552k"))
    }

    @Test
    fun validateZapPresetsRejectsEmptyAndTooManyPresets() {
        assertEquals(R.string.settings_wallet_presets_invalid, validateZapPresets(emptyList()))
        assertEquals(
            R.string.settings_wallet_presets_too_many,
            validateZapPresets((1L..MAX_WALLET_ZAP_PRESETS + 1L).toList()),
        )
        assertNull(validateZapPresets((1L..MAX_WALLET_ZAP_PRESETS.toLong()).toList()))
    }
}
