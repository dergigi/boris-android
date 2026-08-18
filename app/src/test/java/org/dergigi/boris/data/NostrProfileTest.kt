package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NostrProfileTest {
    private val nprofile =
        "nprofile1qyv8wue69uhk6mmwv9jzu6nzx56jucm0d5arsvpcxqq3qamn8ghj7atdvfex2mp6xsurgwqqyzdkm9dhdgq3jxjvw7qc26q76lememf0l75wg9gka3uzgzepx2zl2ewxw6s"

    @Test
    fun parsesNprofileUri() {
        val profile = NostrProfile.parse("nostr:$nprofile")!!
        assertEquals("9b6d95b76a01191a4c778185681ed7f3bced2fffa8e41516ec78240b213285f5", profile.pubkey)
        assertEquals(listOf("ws://monad.jb55.com:8080", "ws://umbrel:4848"), profile.relays)
        assertEquals("nostr:$nprofile", profile.uri)
        assertEquals("https://njump.to/$nprofile", profile.publicUrl)
    }

    @Test
    fun parsesNpubUri() {
        val profile = NostrProfile.parse(
            "nostr:npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6",
        )!!
        assertEquals("3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d", profile.pubkey)
    }

    @Test
    fun linkifyTurnsBareProfilesIntoMarkdownLinks() {
        val markdown = "Thanks nostr:$nprofile for the note."
        val out = NostrProfile.linkify(markdown)
        assertTrue(out.contains("](nostr:$nprofile)"))
        assertTrue(out.contains("@npub1"))
        assertTrue(out.endsWith(" for the note."))
    }

    @Test
    fun linkifyLeavesExistingMarkdownTargetsAlone() {
        val markdown = "[profile](nostr:$nprofile)"
        assertEquals(markdown, NostrProfile.linkify(markdown))
    }

    @Test
    fun ignoresOtherText() {
        assertNull(NostrProfile.parse("nostr:note1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq"))
        assertEquals("plain text", NostrProfile.linkify("plain text"))
    }
}
