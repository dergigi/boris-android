package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileTest {
    @Test
    fun parsePrefersDisplayName() {
        val profile = Profile.parse(
            """{"name":"short","display_name":"Long Name","picture":"https://cdn.example/a.png"}""",
        )
        assertEquals("Long Name", profile.name)
        assertEquals("https://cdn.example/a.png", profile.picture)
        assertNull(profile.about)
    }

    @Test
    fun parseReadsAbout() {
        val profile = Profile.parse(
            """{"name":"Gigi","about":"Not doing DMs. Aspiring Saunameister."}""",
        )
        assertEquals("Gigi", profile.name)
        assertEquals("Not doing DMs. Aspiring Saunameister.", profile.about)
    }

    @Test
    fun parseFallsBackToNameAndDropsBadPicture() {
        val profile = Profile.parse("""{"name":"short","picture":"nope"}""")
        assertEquals("short", profile.name)
        assertNull(profile.picture)
    }
}
