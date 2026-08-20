package org.dergigi.boris.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dergigi.boris.nostr.RelayQuery

object SettingsSync {
    private val _settings = MutableStateFlow(UserSettings.defaults())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _ready = MutableStateFlow(true)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _hasRemote = MutableStateFlow(false)
    val hasRemote: StateFlow<Boolean> = _hasRemote.asStateFlow()

    @Volatile
    var dirty: Boolean = false
        private set

    fun reset() {
        dirty = false
        _hasRemote.value = false
        _ready.value = true
        _settings.value = UserSettings.defaults()
    }

    fun markLoading() {
        if (dirty) return
        _ready.value = false
    }

    fun apply(next: UserSettings) {
        dirty = true
        _settings.value = next
    }

    fun markSynced(next: UserSettings) {
        _settings.value = next
        dirty = false
    }

    suspend fun load(pubkeyHex: String) {
        if (dirty) {
            _ready.value = true
            return
        }
        val event = try {
            RelayQuery.fetchAppData(pubkeyHex)
        } catch (_: Exception) {
            null
        }
        if (dirty) {
            _ready.value = true
            return
        }
        if (event == null) {
            _hasRemote.value = false
            _settings.value = UserSettings.defaults()
        } else {
            _hasRemote.value = true
            val parsed = UserSettings.parse(event.content)
            _settings.value = if (parsed.firstTimeDismissed) {
                parsed
            } else {
                parsed.withBoolean("firstTimeDismissed", true)
            }
        }
        _ready.value = true
    }
}
