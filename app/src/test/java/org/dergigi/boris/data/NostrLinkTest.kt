package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip19
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NostrLinkTest {
    private val naddr =
        "naddr1qqxnzd3cxqmrzv3exgmr2wfeqy08wumn8ghj7mn0wd68yttsw43zuam9d3kx7unyv4ezumn9wshszyrhwden5te0dehhxarj9ekk7mf0qy88wumn8ghj7mn0wvhxcmmv9uq3zamnwvaz7tmwdaehgu3wwa5kuef0qy2hwumn8ghj7un9d3shjtnwdaehgu3wvfnj7q3qdergggklka99wwrs92yz8wdjs952h2ux2ha2ed598ngwu9w7a6fsxpqqqp65wy2vhhv"

    @Test
    fun parsesSharedNostrNaddr() {
        val target = NostrLink.parse("nostr:$naddr") as NostrTarget.Article
        assertEquals("1680612926599", target.ref.pointer.identifier)
        assertEquals("nostr:$naddr", target.uri)
    }

    @Test
    fun parsesNostrUriWithSlashes() {
        val target = NostrLink.parse("nostr://$naddr") as NostrTarget.Article
        assertEquals("1680612926599", target.ref.pointer.identifier)
    }

    @Test
    fun parsesNoteAndNevent() {
        val id = "d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f"
        val note = Nip19.noteEncode(id)
        val fromNote = NostrLink.parse("nostr:$note") as NostrTarget.Note
        assertEquals(id, fromNote.eventId)
        assertEquals("nostr:$note", fromNote.uri)
        assertEquals("https://njump.to/$note", fromNote.publicUrl)

        val nevent = Nip19.neventEncode(
            org.dergigi.boris.nostr.NeventPointer(id, listOf("wss://nos.lol"), kind = 1),
        )
        val fromEvent = NostrLink.parse(nevent) as NostrTarget.Note
        assertEquals(id, fromEvent.eventId)
        assertEquals(listOf("wss://nos.lol"), fromEvent.relays)
        assertEquals("https://njump.to/$nevent", fromEvent.publicUrl)
    }

    @Test
    fun parsesEventPathOnBorisAndNjump() {
        val id = "d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f"
        val fromBoris = NostrLink.parse("https://readwithboris.com/e/$id") as NostrTarget.Note
        assertEquals(id, fromBoris.eventId)
        val fromNjump = NostrLink.parse("https://njump.to/e/$id") as NostrTarget.Note
        assertEquals(id, fromNjump.eventId)
    }

    @Test
    fun ignoresOtherNaddrKinds() {
        val pointer = org.dergigi.boris.nostr.NaddrPointer(
            identifier = "draft",
            pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            kind = 30024,
        )
        assertNull(NostrLink.parse(Nip19.naddrEncode(pointer)))
    }

    @Test
    fun ignoresBareHex() {
        assertNull(NostrLink.parse("d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f"))
        assertNull(NostrLink.parse("not a nostr link"))
    }
}
