package org.dergigi.boris.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dergigi.boris.nostr.RelayQuery

object SettingsSync {
    private val _settings = MutableStateFlow(UserSettings.defaults())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    @Volatile
    var dirty: Boolean = false
        private set

    fun reset() {
        dirty = false
        _settings.value = UserSettings.defaults()
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
        if (dirty) return
        val event = try {
            RelayQuery.fetchAppData(pubkeyHex)
        } catch (_: Exception) {
            null
        }
        if (dirty) return
        if (event == null) {
            _settings.value = UserSettings.defaults()
            return
        }
        _settings.value = UserSettings.parse(event.content)
    }
}
