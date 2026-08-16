package org.dergigi.boris.data

import org.json.JSONObject
import java.io.File

/** Device-local reading positions, keyed by article URL. Values are scroll fractions (0..1). */
object ReadingPositionStore {
    private const val MAX_ENTRIES = 500
    private val lock = Any()
    private var file: File? = null
    private val positions = LinkedHashMap<String, Float>()

    fun init(target: File) {
        synchronized(lock) {
            file = target
            positions.clear()
            if (target.exists()) {
                runCatching {
                    val obj = JSONObject(target.readText())
                    for (key in obj.keys()) {
                        positions[key] = obj.getDouble(key).toFloat()
                    }
                }
            }
        }
    }

    fun fraction(url: String): Float = synchronized(lock) { positions[url] ?: 0f }

    fun save(url: String, fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        synchronized(lock) {
            positions.remove(url)
            positions[url] = clamped
            while (positions.size > MAX_ENTRIES) {
                positions.remove(positions.keys.first())
            }
            val target = file ?: return
            runCatching {
                val obj = JSONObject()
                positions.forEach { (key, value) -> obj.put(key, value.toDouble()) }
                target.writeText(obj.toString())
            }
        }
    }
}
