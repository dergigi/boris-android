package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun parseUnescapesJsonStringValues() {
        val profile = Profile.parse(
            """{"name":"Uno","about":"REJECT THE ORDINARY.\n\nBeRetarded.com\t\u2713 \"quoted\""}""",
        )
        assertEquals(
            "REJECT THE ORDINARY.\n\nBeRetarded.com\t✓ \"quoted\"",
            profile.about,
        )
    }

    @Test
    fun displayNameFallsBackToShortNpub() {
        val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        assertEquals("Gigi", Profile.displayName(pubkey, Profile(name = "Gigi", picture = null)))
        val short = Profile.displayName(pubkey, null)
        assertEquals(Profile.shortNpub(pubkey), short)
        assertTrue(short.startsWith("npub1"))
        assertTrue(short.endsWith("…"))
    }

    @Test
    fun parseFallsBackToNameAndDropsBadPicture() {
        val profile = Profile.parse("""{"name":"short","picture":"nope"}""")
        assertEquals("short", profile.name)
        assertNull(profile.picture)
    }
}
