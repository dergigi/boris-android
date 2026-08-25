package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip01Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveContentTest {
    @Test
    fun contentWarningTagIsConfirmed() {
        val warning = SensitiveContent.fromTags(listOf(listOf("content-warning", "nudity")))
        assertNotNull(warning)
        assertTrue(warning!!.confirmed)
        assertEquals("nudity", warning.reason)
    }

    @Test
    fun bareContentWarningTagHasNoReason() {
        val warning = SensitiveContent.fromTags(listOf(listOf("content-warning")))
        assertNotNull(warning)
        assertTrue(warning!!.confirmed)
        assertNull(warning.reason)
    }

    @Test
    fun nip32LabelIsConfirmed() {
        val warning = SensitiveContent.fromTags(
            listOf(
                listOf("L", "content-warning"),
                listOf("l", "nsfw", "content-warning"),
            ),
        )
        assertNotNull(warning)
        assertTrue(warning!!.confirmed)
        assertEquals("nsfw", warning.reason)
    }

    @Test
    fun nsfwHashtagIsConfirmed() {
        val warning = SensitiveContent.fromTags(listOf(listOf("t", "NSFW")))
        assertNotNull(warning)
        assertTrue(warning!!.confirmed)
        assertEquals("nsfw", warning.reason)
    }

    @Test
    fun titleKeywordIsUnconfirmed() {
        val warning = SensitiveContent.fromText("An NSFW field report", null)
        assertNotNull(warning)
        assertFalse(warning!!.confirmed)
        assertEquals("nsfw", warning.reason)
    }

    @Test
    fun summaryKeywordIsUsedWhenTitleIsClean() {
        val warning = SensitiveContent.fromText("Travel notes", "Includes explicit photos")
        assertNotNull(warning)
        assertFalse(warning!!.confirmed)
        assertEquals("explicit", warning.reason)
    }

    @Test
    fun cleanTextIsNotFlagged() {
        assertNull(SensitiveContent.fromText("On Bitcoin and time", "A calm essay."))
        assertNull(SensitiveContent.fromTags(listOf(listOf("t", "bitcoin"))))
    }

    @Test
    fun keywordDoesNotMatchInsideOtherWords() {
        assertNull(SensitiveContent.fromText("The pornographers of metaphor", null))
        assertNotNull(SensitiveContent.fromText("This is porn", null))
    }

    @Test
    fun tagsWinOverTitleKeywords() {
        val content = ReadableContent(
            url = "https://example.com/post",
            title = "An NSFW essay",
            tags = listOf(listOf("content-warning", "violence")),
        )
        val warning = SensitiveContent.classify(content)
        assertNotNull(warning)
        assertTrue(warning!!.confirmed)
        assertEquals("violence", warning.reason)
    }

    @Test
    fun fromEventReadsContentWarning() {
        val event = Nip01Event(
            id = "11".repeat(32),
            pubkey = "aa".repeat(32),
            createdAt = 1,
            kind = Nip01Event.KIND_LONG_FORM,
            tags = listOf(listOf("title", "Hello"), listOf("content-warning", "nsfw")),
            content = "body",
            sig = "22".repeat(64),
        )
        val warning = SensitiveContent.fromEvent(event)
        assertNotNull(warning)
        assertTrue(warning!!.confirmed)
        assertEquals("nsfw", warning.reason)
    }
}
