package org.dergigi.boris.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class DisplayType(val id: String) {
    Color("color"),
    Eink("eink"),
    ;

    val eink: Boolean get() = this == Eink

    companion object {
        fun fromId(value: String?): DisplayType =
            if (value?.trim() == Eink.id) Eink else Color
    }
}

/** Device-local screen mode. Not part of synced settings. */
object DisplayTypeStore {
    private val lock = Any()
    private var file: File? = null
    private val _type = MutableStateFlow(DisplayType.Color)
    val type: StateFlow<DisplayType> = _type.asStateFlow()

    fun init(target: File) {
        synchronized(lock) {
            file = target
            _type.value = if (target.exists()) {
                DisplayType.fromId(target.readText())
            } else {
                DisplayType.Color
            }
        }
    }

    fun set(next: DisplayType) {
        synchronized(lock) {
            _type.value = next
            file?.writeText(next.id)
        }
    }

    fun reset() = set(DisplayType.Color)
}
