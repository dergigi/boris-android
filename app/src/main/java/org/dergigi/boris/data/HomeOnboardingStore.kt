package org.dergigi.boris.data

import android.content.Context

/** Local flag for the Home “First time?” onboarding section. */
object HomeOnboardingStore {
    private const val PREFS = "boris_home"
    private const val KEY_DISMISSED = "first_time_dismissed"

    fun isDismissed(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DISMISSED, false)

    fun dismiss(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DISMISSED, true)
            .apply()
    }
}
