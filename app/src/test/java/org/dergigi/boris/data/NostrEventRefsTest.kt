package org.dergigi.boris.data

import org.dergigi.boris.data.takeIfActive
import org.dergigi.boris.nostr.HintedRelays
import org.dergigi.boris.nostr.NeventPointer
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.Profile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class NostrEventRefsTest {
    private val eventId = "d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f"
    private val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
    private val note = Nip19.noteEncode(eventId)
    private val nevent = Nip19.neventEncode(
        NeventPointer(eventId, listOf("wss://nos.lol"), author = pubkey, kind = 1),
    )
    private lateinit var hintedFile: File

    @Before
    fun setUp() {
        hintedFile = File.createTempFile("hinted_relays", ".json")
        HintedRelays.clear()
        HintedRelays.init(hintedFile)
    }

    @After
    fun tearDown() {
        HintedRelays.clear()
        hintedFile.delete()
    }

    @Test
    fun collectFindsPrefixedNoteAndNevent() {
        val refs = NostrEventRefs.collect("See nostr:$note and nostr:$nevent please")
        assertEquals(1, refs.size)
        assertEquals(eventId, refs[0].eventId)
    }

    @Test
    fun collectLeavesBareIdentifiersAndCodeAlone() {
        assertTrue(NostrEventRefs.collect("bare $nevent here").isEmpty())
        val fenced = """
            Before

            ```
            nostr:$note
            ```

            After
        """.trimIndent()
        assertTrue(NostrEventRefs.collect(fenced).isEmpty())
        assertTrue(NostrEventRefs.collect("code `nostr:$note` here").isEmpty())
    }

    @Test
    fun collectSkipsExistingMarkdownLinks() {
        assertTrue(NostrEventRefs.collect("See [earlier](nostr:$note) here").isEmpty())
    }

    @Test
    fun collectRemembersNeventRelayHints() {
        NostrEventRefs.collect("Hello nostr:$nevent")
        assertEquals(listOf("wss://nos.lol"), HintedRelays.forPubkey(pubkey))
    }

    @Test
    fun collectMergesRelaysWhenNoteComesBeforeNevent() {
        val refs = NostrEventRefs.collect("nostr:$note then nostr:$nevent")
        assertEquals(1, refs.size)
        assertEquals(listOf("wss://nos.lol"), refs[0].relays)
        assertEquals(pubkey, refs[0].author)
    }

    @Test
    fun parseStandaloneAcceptsRawAndAngleButNotMarkdown() {
        assertEquals(eventId, NostrEventRefs.parseStandalone("nostr:$nevent")?.eventId)
        assertEquals(eventId, NostrEventRefs.parseStandalone("<nostr:$note>")?.eventId)
        assertNull(NostrEventRefs.parseStandalone("[Watch](nostr:$note)"))
        assertNull(NostrEventRefs.parseStandalone("See nostr:$note here"))
        assertNull(NostrEventRefs.parseStandalone(nevent))
    }

    @Test
    fun rewriteTurnsInlineNoteIntoAuthorLink() {
        val resolved = mapOf(eventId to resolvedNote("Hello world"))
        val out = NostrEventRefs.rewrite(
            "See nostr:$note today",
            resolved,
        ) { "Note by $it" }
        assertEquals("See [Note by Alice](nostr:$note) today", out)
    }

    @Test
    fun rewriteTurnsInlineArticleIntoTitleLink() {
        val resolved = mapOf(eventId to resolvedArticle("Taste Is All That's Left"))
        val out = NostrEventRefs.rewrite(
            "Read nostr:$note next",
            resolved,
        ) { "Note by $it" }
        assertEquals("Read [Taste Is All That's Left](nostr:$note) next", out)
    }

    @Test
    fun rewriteLeavesUnknownKindAndMissingEvents() {
        val highlight = ResolvedEventRef(
            event = event(kind = Nip01Event.KIND_HIGHLIGHT, content = "quote"),
        )
        assertEquals(
            "See nostr:$note",
            NostrEventRefs.rewrite("See nostr:$note", mapOf(eventId to highlight)) { "Note by $it" },
        )
        assertEquals(
            "See nostr:$note",
            NostrEventRefs.rewrite("See nostr:$note", emptyMap()) { "Note by $it" },
        )
    }

    @Test
    fun rewriteLeavesStandaloneParagraphRaw() {
        val src = "Before\n\nnostr:$note\n\nAfter"
        val resolved = mapOf(eventId to resolvedNote("Hello"))
        assertEquals(src, NostrEventRefs.rewrite(src, resolved) { "Note by $it" })
    }

    @Test
    fun takeIfActiveDropsStaleResults() {
        val resolved = mapOf(eventId to resolvedNote("Hello"))
        assertEquals(resolved, resolved.takeIfActive(true))
        assertNull(resolved.takeIfActive(false))
    }

    @Test
    fun rewriteLeavesCodeFencesAlone() {
        val src = """
            Before

            ```
            nostr:$note
            ```

            After
        """.trimIndent()
        val resolved = mapOf(eventId to resolvedNote("Hello"))
        assertEquals(src, NostrEventRefs.rewrite(src, resolved) { "Note by $it" })
    }

    @Test
    fun inlineLabelUsesTitleOrNoteByAuthor() {
        assertEquals(
            "Taste",
            NostrEventRefs.inlineLabel(resolvedArticle("Taste")) { "Note by $it" },
        )
        assertEquals(
            "Note by Alice",
            NostrEventRefs.inlineLabel(resolvedNote("Hello")) { "Note by $it" },
        )
        assertNull(
            NostrEventRefs.inlineLabel(
                ResolvedEventRef(event(kind = Nip01Event.KIND_HIGHLIGHT, content = "q")),
            ) { "Note by $it" },
        )
        assertNull(NostrEventRefs.inlineLabel(resolvedArticle(title = null)) { "Note by $it" })
    }

    private fun resolvedNote(content: String) = ResolvedEventRef(
        event = event(kind = Nip01Event.KIND_TEXT_NOTE, content = content),
        profile = Profile(name = "Alice", picture = null),
    )

    private fun resolvedArticle(title: String?) = ResolvedEventRef(
        event = event(
            kind = Nip01Event.KIND_LONG_FORM,
            content = "body",
            tags = if (title == null) emptyList() else listOf(listOf("title", title)),
        ),
        profile = Profile(name = "Alice", picture = null),
    )

    private fun event(
        kind: Int,
        content: String,
        tags: List<List<String>> = emptyList(),
    ) = Nip01Event(
        id = eventId,
        pubkey = pubkey,
        createdAt = 1,
        kind = kind,
        tags = tags,
        content = content,
        sig = "dd".repeat(64),
    )
}
