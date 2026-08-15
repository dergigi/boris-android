package org.dergigi.boris.nostr

import org.json.JSONArray
import org.json.JSONObject

object Nip78 {
    const val KIND = Nip01Event.KIND_APP_DATA
    const val SETTINGS_D = "com.dergigi.boris.user-settings"

    fun tags(): List<List<String>> = listOf(listOf("d", SETTINGS_D))

    fun hasSettingsD(event: Nip01Event): Boolean =
        event.tags.any { tag ->
            tag.size >= 2 && tag[0] == "d" && tag[1] == SETTINGS_D
        }

    fun unsignedJson(
        content: String,
        pubkeyHex: String? = null,
        createdAt: Long = System.currentTimeMillis() / 1000,
    ): String {
        val obj = JSONObject()
            .put("kind", KIND)
            .put("content", content)
            .put("tags", tagsToJson(tags()))
            .put("created_at", createdAt)
        if (!pubkeyHex.isNullOrBlank()) {
            obj.put("pubkey", pubkeyHex)
        }
        return obj.toString()
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
}
