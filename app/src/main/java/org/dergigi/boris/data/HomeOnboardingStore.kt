package org.dergigi.boris.data

import android.content.Context

/** Local flags for dismissible Home prompt sections. */
object HomeOnboardingStore {
    private const val PREFS = "boris_home"
    private const val KEY_FIRST_TIME_DISMISSED = "first_time_dismissed"
    private const val KEY_LOGIN_DISMISSED = "login_prompt_dismissed"

    fun isFirstTimeDismissed(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FIRST_TIME_DISMISSED, false)

    fun dismissFirstTime(context: Context) {
        prefs(context).edit().putBoolean(KEY_FIRST_TIME_DISMISSED, true).apply()
    }

    fun isLoginDismissed(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOGIN_DISMISSED, false)

    fun dismissLogin(context: Context) {
        prefs(context).edit().putBoolean(KEY_LOGIN_DISMISSED, true).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
