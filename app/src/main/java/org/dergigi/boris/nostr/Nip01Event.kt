package org.dergigi.boris.nostr

import fr.acinq.secp256k1.Secp256k1
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

data class Nip01Event(
    val id: String,
    val pubkey: String,
    val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String,
) {
    fun toJsonString(): String {
        return JSONObject()
            .put("id", id)
            .put("pubkey", pubkey)
            .put("created_at", createdAt)
            .put("kind", kind)
            .put("tags", tagsToJson(tags))
            .put("content", content)
            .put("sig", sig)
            .toString()
    }

    fun verify(): Boolean {
        val serialized = serializeForId(pubkey, createdAt, kind, tags, content)
        val computed = sha256(serialized.toByteArray(Charsets.UTF_8)).toHex()
        if (computed != id) return false
        return try {
            Secp256k1.verifySchnorr(
                sig.hexToByteArray(),
                id.hexToByteArray(),
                pubkey.hexToByteArray(),
            )
        } catch (_: Exception) {
            false
        }
    }

    fun hasPTag(pubkeyHex: String): Boolean =
        tags.any { it.size >= 2 && it[0] == "p" && it[1].equals(pubkeyHex, ignoreCase = true) }

    companion object {
        const val KIND_METADATA = 0
        const val KIND_RPC = 24133
        const val KIND_AUTH = 22242
        const val KIND_HIGHLIGHT = 9802
        const val KIND_RELAY_LIST = 10002
        const val KIND_APP_DATA = 30078
        const val KIND_RELAY_DISCOVERY = 30166

        fun complete(
            pubkey: String,
            createdAt: Long,
            kind: Int,
            tags: List<List<String>>,
            content: String,
            sig: String,
        ): Nip01Event {
            val serialized = serializeForId(pubkey, createdAt, kind, tags, content)
            val id = sha256(serialized.toByteArray(Charsets.UTF_8)).toHex()
            return Nip01Event(id, pubkey, createdAt, kind, tags, content, sig)
        }

        fun sign(
            privkey: ByteArray,
            pubkeyHex: String,
            kind: Int,
            tags: List<List<String>>,
            content: String,
            createdAt: Long = System.currentTimeMillis() / 1000,
        ): Nip01Event {
            val serialized = serializeForId(pubkeyHex, createdAt, kind, tags, content)
            val id = sha256(serialized.toByteArray(Charsets.UTF_8)).toHex()
            val aux = ByteArray(32)
            SecureRandom().nextBytes(aux)
            val sig = Secp256k1.signSchnorr(id.hexToByteArray(), privkey, aux).toHex()
            return complete(pubkeyHex, createdAt, kind, tags, content, sig)
        }

        fun parse(json: JSONObject): Nip01Event? {
            return try {
                val tagsJson = json.getJSONArray("tags")
                val tags = buildList {
                    for (i in 0 until tagsJson.length()) {
                        val row = tagsJson.getJSONArray(i)
                        add(buildList {
                            for (j in 0 until row.length()) add(row.getString(j))
                        })
                    }
                }
                Nip01Event(
                    id = json.getString("id"),
                    pubkey = json.getString("pubkey"),
                    createdAt = json.getLong("created_at"),
                    kind = json.getInt("kind"),
                    tags = tags,
                    content = json.getString("content"),
                    sig = json.getString("sig"),
                )
            } catch (_: Exception) {
                null
            }
        }

        internal fun serializeForId(
            pubkey: String,
            createdAt: Long,
            kind: Int,
            tags: List<List<String>>,
            content: String,
        ): String {
            return buildString {
                append("[0,\"")
                append(jsonEscape(pubkey))
                append("\",")
                append(createdAt)
                append(',')
                append(kind)
                append(',')
                append(serializeTags(tags))
                append(",\"")
                append(jsonEscape(content))
                append("\"]")
            }
        }

        private fun serializeTags(tags: List<List<String>>): String = buildString {
            append('[')
            tags.forEachIndexed { i, tag ->
                if (i > 0) append(',')
                append('[')
                tag.forEachIndexed { j, value ->
                    if (j > 0) append(',')
                    append('"')
                    append(jsonEscape(value))
                    append('"')
                }
                append(']')
            }
            append(']')
        }

        private fun tagsToJson(tags: List<List<String>>): JSONArray {
            val out = JSONArray()
            for (tag in tags) {
                val row = JSONArray()
                for (value in tag) row.put(value)
                out.put(row)
            }
            return out
        }

        private fun jsonEscape(value: String): String = buildString(value.length) {
            for (c in value) {
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000c' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(c)
                }
            }
        }

        private fun sha256(data: ByteArray): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(data)
    }
}
