package org.dergigi.boris.nostr

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Nip17Test {
    @Test
    fun giftWrapUnwrapsToRumorForRecipient() {
        val recipient = ClientKeypair.generate()
        val sender = ClientKeypair.generate()
        val now = 1_757_400_000L

        val wrap = Nip17.giftWrap("hello boris", recipient.pubkeyHex, sender, now)

        assertEquals(Nip17.KIND_GIFT_WRAP, wrap.kind)
        assertTrue(wrap.verify())
        assertNotEquals(sender.pubkeyHex, wrap.pubkey)
        assertTrue(wrap.hasPTag(recipient.pubkeyHex))
        assertTrue(wrap.createdAt <= now)
        assertTrue(wrap.createdAt > now - 3 * 24 * 60 * 60)

        val sealJson = Nip44.decrypt(wrap.content, recipient.privkey, wrap.pubkey)
        val seal = Nip01Event.parse(JSONObject(sealJson))!!
        assertEquals(Nip17.KIND_SEAL, seal.kind)
        assertEquals(sender.pubkeyHex, seal.pubkey)
        assertTrue(seal.verify())
        assertTrue(seal.tags.isEmpty())

        val rumor = JSONObject(Nip44.decrypt(seal.content, recipient.privkey, seal.pubkey))
        assertEquals(Nip17.KIND_DM, rumor.getInt("kind"))
        assertEquals("hello boris", rumor.getString("content"))
        assertEquals(sender.pubkeyHex, rumor.getString("pubkey"))
        assertEquals(now, rumor.getLong("created_at"))
        assertFalse(rumor.has("sig"))
        assertEquals(recipient.pubkeyHex, rumor.getJSONArray("tags").getJSONArray(0).getString(1))
    }

    @Test
    fun parseDmRelaysTakesNewestListAndDropsJunk() {
        val old = event(Nip17.KIND_DM_RELAYS, 1, listOf(listOf("relay", "wss://old.example")))
        val new = event(
            Nip17.KIND_DM_RELAYS,
            2,
            listOf(
                listOf("relay", "wss://inbox.example"),
                listOf("relay", "ws://plain.example"),
                listOf("r", "wss://not-a-relay-tag.example"),
                listOf("relay", "wss://inbox.example"),
            ),
        )
        assertEquals(listOf("wss://inbox.example"), Nip17.parseDmRelays(listOf(old, new)))
        assertEquals(emptyList<String>(), Nip17.parseDmRelays(emptyList()))
    }

    private fun event(kind: Int, createdAt: Long, tags: List<List<String>>): Nip01Event =
        Nip01Event("id$createdAt", "pub", createdAt, kind, tags, "", "sig")
}
