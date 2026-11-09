package io.github.wintech_partner.swipeskip.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/**
 * Remembers whether the accessibility service was switched on, so Home can tell "never set up" apart from
 * "Android switched it off behind our back". A graceful unbind (the user toggling it) clears the flag; a force-stop
 * kills the process without any callback, so the flag stays set while the setting is empty. Stores one boolean, nothing else.
 */
object ServiceWatch {
    private const val FILE = "service_watch"
    private const val KEY_EXPECTED = "expected"

    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun setExpected(c: Context, expected: Boolean) {
        prefs(c).edit().putBoolean(KEY_EXPECTED, expected).apply()
    }

    fun isEnabledInSettings(c: Context): Boolean {
        val me = ComponentName(c, AutoScrollService::class.java)
        val raw = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return raw.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    /** True when the service was on, nobody turned it off through the app's own lifecycle, and the setting no longer lists it. */
    fun wasDropped(c: Context): Boolean = prefs(c).getBoolean(KEY_EXPECTED, false) && !isEnabledInSettings(c)
}
