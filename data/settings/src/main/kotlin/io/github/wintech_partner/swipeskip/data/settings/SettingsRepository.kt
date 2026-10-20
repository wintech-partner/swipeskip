package io.github.wintech_partner.swipeskip.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import io.github.wintech_partner.swipeskip.core.model.AppSettings
import io.github.wintech_partner.swipeskip.core.model.AutomationMode
import io.github.wintech_partner.swipeskip.core.model.AutomationPolicy
import io.github.wintech_partner.swipeskip.core.model.MotionSensitivity
import io.github.wintech_partner.swipeskip.core.model.ScrollDirection
import io.github.wintech_partner.swipeskip.core.model.SessionSettings
import io.github.wintech_partner.swipeskip.core.model.ThemeMode

/**
 * Settings live only in a local DataStore. [policy] is the flavor's allowed-modes rule; it is applied on
 * every read so a stale stored mode can never escape the flavor's limits.
 */
class SettingsRepository(
    private val store: DataStore<Preferences>,
    private val policy: AutomationPolicy,
) {
    private object Keys {
        val interval = intPreferencesKey("interval_seconds")
        val duration = intPreferencesKey("max_duration_minutes")
        val maxScrolls = intPreferencesKey("max_scrolls")
        val direction = stringPreferencesKey("direction")
        val mode = stringPreferencesKey("mode")
        val pauseOnTouch = booleanPreferencesKey("pause_on_touch")
        val resumeDelay = intPreferencesKey("resume_delay_seconds")
        val theme = stringPreferencesKey("theme")
        val adNotifications = booleanPreferencesKey("ad_notifications")
        val haptics = booleanPreferencesKey("haptics")
        val consent = booleanPreferencesKey("consent_given")
        val motionNext = booleanPreferencesKey("motion_next")
        val motionSensitivity = stringPreferencesKey("motion_sensitivity")
        val autoSkipAds = booleanPreferencesKey("auto_skip_ads")
    }

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toSettings())
            val session = policy.apply(next.session.coerced())
            prefs[Keys.interval] = session.intervalSeconds
            prefs[Keys.duration] = session.maxDurationMinutes
            if (session.maxScrolls != null) prefs[Keys.maxScrolls] = session.maxScrolls!! else prefs.remove(Keys.maxScrolls)
            prefs[Keys.direction] = session.direction.name
            prefs[Keys.mode] = session.mode.name
            prefs[Keys.pauseOnTouch] = session.pauseOnTouch
            prefs[Keys.resumeDelay] = session.resumeDelaySeconds
            prefs[Keys.theme] = next.theme.name
            prefs[Keys.adNotifications] = next.adNotifications
            prefs[Keys.haptics] = next.haptics
            prefs[Keys.consent] = next.consentGiven
            prefs[Keys.motionNext] = next.motionNext
            prefs[Keys.motionSensitivity] = next.motionSensitivity.name
            prefs[Keys.autoSkipAds] = next.autoSkipAds && policy.adSkipAllowed
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = SessionSettings(mode = policy.defaultMode)
        val session = SessionSettings(
            intervalSeconds = this[Keys.interval] ?: defaults.intervalSeconds,
            maxDurationMinutes = this[Keys.duration] ?: defaults.maxDurationMinutes,
            maxScrolls = this[Keys.maxScrolls],
            direction = enumOr(this[Keys.direction], defaults.direction),
            mode = enumOr(this[Keys.mode], defaults.mode),
            pauseOnTouch = this[Keys.pauseOnTouch] ?: defaults.pauseOnTouch,
            resumeDelaySeconds = this[Keys.resumeDelay] ?: defaults.resumeDelaySeconds,
        )
        return AppSettings(
            session = policy.apply(session.coerced()),
            theme = enumOr(this[Keys.theme], ThemeMode.SYSTEM),
            adNotifications = this[Keys.adNotifications] ?: true,
            haptics = this[Keys.haptics] ?: true,
            consentGiven = this[Keys.consent] ?: false,
            motionNext = this[Keys.motionNext] ?: false,
            motionSensitivity = enumOr(this[Keys.motionSensitivity], MotionSensitivity.MEDIUM),
            autoSkipAds = (this[Keys.autoSkipAds] ?: false) && policy.adSkipAllowed,
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback

    companion object {
        val MANUAL_ONLY = AutomationPolicy(setOf(AutomationMode.MANUAL_TAP), AutomationMode.MANUAL_TAP)
        val BOTH_AUTO_DEFAULT = AutomationPolicy(AutomationMode.entries.toSet(), AutomationMode.AUTO_TIMER, adSkipAllowed = true)
        val BOTH_MANUAL_DEFAULT = AutomationPolicy(setOf(AutomationMode.AUTO_TIMER, AutomationMode.MANUAL_TAP), AutomationMode.MANUAL_TAP)
    }
}
