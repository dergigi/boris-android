package org.dergigi.boris.nostr

import org.json.JSONObject
import java.security.SecureRandom

/**
 * NIP-17 private direct messages: an unsigned kind-14 rumor, sealed (kind 13)
 * by the sender and gift-wrapped (kind 1059) by a throwaway key.
 */
object Nip17 {
    const val KIND_DM = 14
    const val KIND_SEAL = 13
    const val KIND_GIFT_WRAP = 1059
    const val KIND_DM_RELAYS = 10050

    /** Seals and wraps randomize `created_at` up to this far into the past. */
    private const val TIMESTAMP_JITTER_SECONDS = 2L * 24 * 60 * 60

    fun giftWrap(
        message: String,
        recipientHex: String,
        sender: ClientKeypair,
        now: Long = System.currentTimeMillis() / 1000,
    ): Nip01Event {
        val rumor = rumorJson(sender.pubkeyHex, recipientHex, message, now)
        val seal = Nip01Event.sign(
            privkey = sender.privkey,
            pubkeyHex = sender.pubkeyHex,
            kind = KIND_SEAL,
            tags = emptyList(),
            content = Nip44.encrypt(rumor, sender.privkey, recipientHex),
            createdAt = jitter(now),
        )
        val wrapper = ClientKeypair.generate()
        try {
            return Nip01Event.sign(
                privkey = wrapper.privkey,
                pubkeyHex = wrapper.pubkeyHex,
                kind = KIND_GIFT_WRAP,
                tags = listOf(listOf("p", recipientHex)),
                content = Nip44.encrypt(seal.toJsonString(), wrapper.privkey, recipientHex),
                createdAt = jitter(now),
            )
        } finally {
            wrapper.privkey.fill(0)
        }
    }

    /** `relay` tags of the newest kind-10050 event, or empty when none. */
    fun parseDmRelays(events: List<Nip01Event>): List<String> {
        val newest = events
            .filter { it.kind == KIND_DM_RELAYS }
            .maxByOrNull { it.createdAt }
            ?: return emptyList()
        return newest.tags
            .filter { it.size >= 2 && it[0] == "relay" }
            .map { it[1].trim() }
            .filter { it.startsWith("wss://", ignoreCase = true) }
            .distinct()
    }

    /** Rumors carry an id but no signature. */
    private fun rumorJson(senderHex: String, recipientHex: String, message: String, createdAt: Long): String {
        val tags = listOf(listOf("p", recipientHex))
        val unsigned = Nip01Event.complete(senderHex, createdAt, KIND_DM, tags, message, sig = "")
        return JSONObject(unsigned.toJsonString()).apply { remove("sig") }.toString()
    }

    private fun jitter(now: Long): Long =
        now - (SecureRandom().nextLong().mod(TIMESTAMP_JITTER_SECONDS))
}
