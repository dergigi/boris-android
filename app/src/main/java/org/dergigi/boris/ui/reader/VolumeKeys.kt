package org.dergigi.boris.ui.reader

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

object VolumeKeys {
    @Volatile
    private var listener: ((Boolean) -> Boolean)? = null

    fun handle(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }
        val current = listener ?: return false
        if (event.action == KeyEvent.ACTION_UP) return true
        if (event.action != KeyEvent.ACTION_DOWN) return false
        return current(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
    }

    fun nextOffset(current: Int, max: Int, page: Int, up: Boolean): Int {
        val delta = if (up) -page else page
        return (current + delta).coerceIn(0, max.coerceAtLeast(0))
    }

    @Composable
    fun Handle(enabled: Boolean = true, onVolume: (up: Boolean) -> Boolean) {
        val latest = rememberUpdatedState(onVolume)
        DisposableEffect(enabled) {
            if (!enabled) {
                listener = null
                return@DisposableEffect onDispose { }
            }
            listener = { up -> latest.value(up) }
            onDispose { listener = null }
        }
    }
}
