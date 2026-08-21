package org.dergigi.boris.tts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** API 33+: ask for POST_NOTIFICATIONS once on first play; denial never blocks playback. */
fun requestTtsNotificationPermissionOnce(context: Context, request: () -> Unit) {
    if (Build.VERSION.SDK_INT < 33) return
    val granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    if (granted) return
    val prefs = context.getSharedPreferences("tts", Context.MODE_PRIVATE)
    if (prefs.getBoolean("notif_requested", false)) return
    prefs.edit().putBoolean("notif_requested", true).apply()
    request()
}
