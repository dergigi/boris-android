package org.dergigi.boris.nostr

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object OfflineOutbox {
    private val lock = Any()
    private val events = LinkedHashMap<String, Nip01Event>()
    private var file: File? = null

    fun init(storage: File) {
        synchronized(lock) {
            file = storage
            events.clear()
            if (!storage.exists()) return
            runCatching {
                val array = JSONArray(storage.readText())
                for (i in 0 until array.length()) {
                    val event = Nip01Event.parse(array.getJSONObject(i)) ?: continue
                    events[event.id.lowercase()] = event
                }
            }
        }
    }

    fun add(event: Nip01Event) {
        synchronized(lock) {
            events[event.id.lowercase()] = event
            persist()
        }
    }

    fun remove(id: String) {
        synchronized(lock) {
            if (events.remove(id.lowercase()) != null) persist()
        }
    }

    fun pending(): List<Nip01Event> = synchronized(lock) { events.values.toList() }

    internal fun reset() {
        synchronized(lock) {
            events.clear()
            file = null
        }
    }

    private fun persist() {
        val storage = file ?: return
        runCatching {
            storage.parentFile?.mkdirs()
            val array = JSONArray()
            events.values.forEach { array.put(JSONObject(it.toJsonString())) }
            storage.writeText(array.toString())
        }
    }
}
