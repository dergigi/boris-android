package org.dergigi.boris.nostr

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventSignerTest {
    @Test
    fun toUnsignedJsonMatchesNip01Helper() {
        val pending = PendingUnsignedEvent(
            pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            createdAt = 1_700_000_000,
            kind = Nip01Event.KIND_HIGHLIGHT,
            tags = Nip84.tags("https://example.com/article", "context"),
            content = "a quote",
        )
        val expected = Nip01Event.unsignedJson(
            kind = pending.kind,
            content = pending.content,
            tags = pending.tags,
            pubkeyHex = pending.pubkey,
            createdAt = pending.createdAt,
        )
        assertJsonEquals(expected, pending.toUnsignedJson(includePubkey = true))
    }

    @Test
    fun toUnsignedJsonOmitsPubkeyWhenAsked() {
        val pending = PendingUnsignedEvent(
            pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            createdAt = 1_700_000_000,
            kind = Nip01Event.KIND_DELETION,
            tags = Nip09.tags("ab".repeat(32), Nip01Event.KIND_HIGHLIGHT),
            content = Nip09.REASON,
        )
        val json = JSONObject(pending.toUnsignedJson(includePubkey = false))
        assertFalse(json.has("pubkey"))
        assertEquals(Nip01Event.KIND_DELETION, json.getInt("kind"))
        assertEquals(Nip09.REASON, json.getString("content"))
        assertEquals(1_700_000_000, json.getLong("created_at"))
    }

    @Test
    fun toUnsignedJsonMatchesNip09AndNip78Helpers() {
        val createdAt = 1_700_000_123L
        val pubkey = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val deletion = PendingUnsignedEvent(
            pubkey = pubkey,
            createdAt = createdAt,
            kind = Nip01Event.KIND_DELETION,
            tags = Nip09.tags("cd".repeat(32), Nip01Event.KIND_HIGHLIGHT),
            content = Nip09.REASON,
        )
        assertJsonEquals(
            Nip09.unsignedJson("cd".repeat(32), Nip01Event.KIND_HIGHLIGHT, pubkey, createdAt),
            deletion.toUnsignedJson(),
        )
        val settings = PendingUnsignedEvent(
            pubkey = pubkey,
            createdAt = createdAt,
            kind = Nip01Event.KIND_APP_DATA,
            tags = Nip78.tags(),
            content = """{"fontSize":21}""",
        )
        assertJsonEquals(
            Nip78.unsignedJson("""{"fontSize":21}""", pubkey, createdAt),
            settings.toUnsignedJson(),
        )
    }

    private fun assertJsonEquals(expected: String, actual: String) {
        val left = JSONObject(expected)
        val right = JSONObject(actual)
        assertEquals(left.getInt("kind"), right.getInt("kind"))
        assertEquals(left.getString("content"), right.getString("content"))
        assertEquals(left.getLong("created_at"), right.getLong("created_at"))
        assertEquals(left.optString("pubkey", ""), right.optString("pubkey", ""))
        assertEquals(left.getJSONArray("tags").toString(), right.getJSONArray("tags").toString())
        assertTrue(left.length() == right.length())
    }
}
