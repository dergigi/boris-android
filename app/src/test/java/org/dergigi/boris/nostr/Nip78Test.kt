package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Nip78Test {
    @Test
    fun settingsUseWebappIdentifier() {
        assertEquals(30078, Nip78.KIND)
        assertEquals("com.dergigi.boris.user-settings", Nip78.SETTINGS_D)
        assertEquals(listOf(listOf("d", "com.dergigi.boris.user-settings")), Nip78.tags())
    }

    @Test
    fun hasSettingsDReadsDTag() {
        val event = Nip01Event(
            id = "1".padStart(64, '0'),
            pubkey = "aa".repeat(32),
            createdAt = 1,
            kind = Nip01Event.KIND_APP_DATA,
            tags = listOf(listOf("d", Nip78.SETTINGS_D)),
            content = "{}",
            sig = "bb".repeat(32),
        )
        assertTrue(Nip78.hasSettingsD(event))
        assertFalse(Nip78.hasSettingsD(event.copy(tags = listOf(listOf("d", "other")))))
    }
}
