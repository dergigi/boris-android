package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NostrMentionsTest {
    private val nprofile =
        "nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p"
    private val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
    private val npub = Nip19.npubEncode(pubkey)

    @Test
    fun rewritesRawPrefixedNprofileToAtNameLink() {
        val label = "@" + Profile.displayName(pubkey, null)
        val out = NostrMentions.rewrite("Hello nostr:$nprofile there")
        assertEquals("Hello [$label](nostr:$nprofile) there", out)
    }

    @Test
    fun keepsCustomMarkdownLabel() {
        val src = "See [Gigi](nostr:$nprofile) for more."
        assertEquals(src, NostrMentions.rewrite(src))
    }

    @Test
    fun leavesBareIdentifiersAlone() {
        assertEquals("bare $nprofile here", NostrMentions.rewrite("bare $nprofile here"))
        assertEquals("bare $npub here", NostrMentions.rewrite("bare $npub here"))
    }

    @Test
    fun leavesSecretKeyAndNaddrAlone() {
        val nsec = "nostr:nsec1vl029mgpspedva04g90vltkh6fvh240zqtv9k0t9af8935ke9laqsnlfe5"
        assertEquals("key $nsec", NostrMentions.rewrite("key $nsec"))
        val naddr =
            "nostr:naddr1qqwhwmmjw35xcetnwvkk6mmwv4uj6arfd4jkcetnwvkkzun595pzq634npfz8rwfq2hdr8am76s9t7dt7gwpe2y3t5wyufl4phe09yxeqvzqqqr4gu7cgak5"
        assertEquals("article $naddr", NostrMentions.rewrite("article $naddr"))
    }

    @Test
    fun leavesCodeFencesAndInlineCodeAlone() {
        val fenced = """
            Before

            ```
            nostr:$nprofile
            ```

            After
        """.trimIndent()
        assertEquals(fenced, NostrMentions.rewrite(fenced))
        val inline = "Use `nostr:$nprofile` in code."
        assertEquals(inline, NostrMentions.rewrite(inline))
    }

    @Test
    fun truncatesUndecodablePrefixedNprofile() {
        val bad = "nostr:nprofile1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq"
        val out = NostrMentions.rewrite("x ${bad} y")
        assertFalse(out.contains("]("))
        assertTrue(out.contains(bad.take(20) + "…"))
    }

    @Test
    fun stripsAngleBracketsAroundPrefixedMention() {
        val label = "@" + Profile.displayName(pubkey, null)
        val out = NostrMentions.rewrite("Hello <nostr:$nprofile> there")
        assertEquals("Hello [$label](nostr:$nprofile) there", out)
    }
}
