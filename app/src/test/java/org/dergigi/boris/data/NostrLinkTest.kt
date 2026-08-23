package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip01Event
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
    fun parsesHighlightAndCommentNevents() {
        val id = "78a214159e5b82028302c9d4212548a14592ca4ec290f1f6a796bdb9bb5dafa5"
        val comment = Nip19.neventEncode(
            org.dergigi.boris.nostr.NeventPointer(
                eventId = id,
                relays = listOf("wss://relay.dergigi.com/"),
                author = "6e468422dfb74a5738702a8823b9b28168abab8655faacb6853cd0ee15deee93",
                kind = Nip01Event.KIND_COMMENT,
            ),
        )
        val fromComment = NostrLink.parse("nostr:$comment") as NostrTarget.Note
        assertEquals(id, fromComment.eventId)
        assertEquals(Nip01Event.KIND_COMMENT, fromComment.kind)
        assertEquals(listOf("wss://relay.dergigi.com/"), fromComment.relays)

        val highlight = Nip19.neventEncode(
            org.dergigi.boris.nostr.NeventPointer(id, kind = Nip01Event.KIND_HIGHLIGHT),
        )
        val fromHighlight = NostrLink.parse("nostr:$highlight") as NostrTarget.Note
        assertEquals(Nip01Event.KIND_HIGHLIGHT, fromHighlight.kind)
        assertEquals("nostr:$highlight", UrlExtractor.extract("nostr:$highlight"))

        val issue99 =
            "nostr:nevent1qqs83gs5zk09hqszsvpvn4ppy4y2z3vjef8v9y8376ned0dehdw6lfgprpmhxue69uhhyetvv9ujuer9wfnkjemf9e3k7mf0qgsxu35yyt0mwjjh8pcz4zprhxegz69t4wr9t74vk6zne58wzh0waycrqsqqqpzhs9hrq9"
        val fromIssue = NostrLink.parse(issue99) as NostrTarget.Note
        assertEquals(
            "78a214159e5b82028302c9d4212548a14592ca4ec290f1f6a796bdb9bb5dafa5",
            fromIssue.eventId,
        )
        assertEquals(Nip01Event.KIND_COMMENT, fromIssue.kind)
        assertEquals(true, fromIssue.relays.any { it.contains("relay.dergigi.com") })
        assertEquals(fromIssue.uri, UrlExtractor.extract(issue99))
    }

    @Test
    fun ignoresUnsupportedNeventKinds() {
        val id = "78a214159e5b82028302c9d4212548a14592ca4ec290f1f6a796bdb9bb5dafa5"
        val reaction = Nip19.neventEncode(
            org.dergigi.boris.nostr.NeventPointer(id, kind = Nip01Event.KIND_REACTION),
        )
        assertNull(NostrLink.parse("nostr:$reaction"))
        assertNull(UrlExtractor.extract("nostr:$reaction"))
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
    fun njumpCopyUrlIsOnlyForReadableNostrContent() {
        val note = Nip19.noteEncode(
            "d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f",
        )
        assertEquals("https://njump.to/$naddr", NostrLink.njumpCopyUrl("nostr:$naddr"))
        assertEquals("https://njump.to/$note", NostrLink.njumpCopyUrl("https://njump.to/$note"))
        assertNull(NostrLink.njumpCopyUrl("https://www.citadel21.com/the-paranoid-wallet"))
        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        assertNull(NostrLink.njumpCopyUrl("https://njump.to/$npub"))
    }

    @Test
    fun copyTextUsesNostrUriOrPlainUrl() {
        val id = "d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f"
        val note = Nip19.noteEncode(id)
        assertEquals("nostr:$note", NostrLink.copyText("nostr:$note"))
        assertEquals("nostr:$naddr", NostrLink.copyText("https://njump.to/$naddr"))
        assertEquals(
            "https://www.citadel21.com/the-paranoid-wallet",
            NostrLink.copyText("https://www.citadel21.com/the-paranoid-wallet"),
        )
    }

    @Test
    fun ignoresBareHex() {
        assertNull(NostrLink.parse("d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f"))
        assertNull(NostrLink.parse("not a nostr link"))
    }

    @Test
    fun parsesPrefixedNprofileAndNpubAsProfile() {
        val nprofile =
            "nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p"
        val fromNostr = NostrLink.parse("nostr:$nprofile") as NostrTarget.Profile
        assertEquals("3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d", fromNostr.pubkeyHex)
        assertEquals(listOf("wss://r.x.com", "wss://djbas.sadkb.com"), fromNostr.relays)
        assertEquals("nostr:$nprofile", fromNostr.uri)

        val fromSlashes = NostrLink.parse("nostr://$nprofile") as NostrTarget.Profile
        assertEquals(fromNostr.pubkeyHex, fromSlashes.pubkeyHex)

        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        val fromNpub = NostrLink.parse("nostr:$npub") as NostrTarget.Profile
        assertEquals("3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d", fromNpub.pubkeyHex)
        assertEquals(emptyList<String>(), fromNpub.relays)
    }

    @Test
    fun rejectsBareProfileIdentifiers() {
        val nprofile =
            "nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p"
        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        assertNull(NostrLink.parse(nprofile))
        assertNull(NostrLink.parse(npub))
    }

    @Test
    fun parsesProfileGatewayPaths() {
        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        val fromNjump = NostrLink.parse("https://njump.to/$npub") as NostrTarget.Profile
        assertEquals("3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d", fromNjump.pubkeyHex)
        val nprofile =
            "nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p"
        val fromBoris = NostrLink.parse("https://readwithboris.com/$nprofile") as NostrTarget.Profile
        assertEquals(fromNjump.pubkeyHex, fromBoris.pubkeyHex)
    }

    @Test
    fun rejectsSecretKeyUri() {
        assertNull(
            NostrLink.parse("nostr:nsec1vl029mgpspedva04g90vltkh6fvh240zqtv9k0t9af8935ke9laqsnlfe5"),
        )
    }

    @Test
    fun parsesIssue5Nprofile() {
        val nprofile =
            "nprofile1qyv8wue69uhk6mmwv9jzu6nzx56jucm0d5arsvpcxqq3qamn8ghj7atdvfex2mp6xsurgwqqyzdkm9dhdgq3jxjvw7qc26q76lememf0l75wg9gka3uzgzepx2zl2ewxw6s"
        val target = NostrLink.parse("nostr:$nprofile") as NostrTarget.Profile
        assertEquals(64, target.pubkeyHex.length)
        assertEquals("nostr:$nprofile", target.uri)
    }
}
