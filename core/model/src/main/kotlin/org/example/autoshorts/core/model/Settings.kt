package org.example.autoshorts.core.model

object SettingsLimits {
    const val MIN_INTERVAL_SECONDS = 3
    const val MAX_INTERVAL_SECONDS = 120
    const val DEFAULT_INTERVAL_SECONDS = 10
    val INTERVAL_PRESETS_SECONDS = listOf(3, 5, 8, 10, 15, 30)

    /** There is deliberately no "unlimited" option. */
    val DURATION_OPTIONS_MINUTES = listOf(15, 30, 60, 120, 240)
    const val DEFAULT_DURATION_MINUTES = 30

    const val MIN_MAX_SCROLLS = 1
    const val MAX_MAX_SCROLLS = 10_000

    const val MAX_RESUME_DELAY_SECONDS = 60
    const val DEFAULT_RESUME_DELAY_SECONDS = 5
}

/**
 * Rules the user picks for one session.
 *
 * [resumeDelaySeconds] applies after a touch pause: 0 means "wait for the user to resume".
 */
data class SessionSettings(
    val intervalSeconds: Int = SettingsLimits.DEFAULT_INTERVAL_SECONDS,
    val maxDurationMinutes: Int = SettingsLimits.DEFAULT_DURATION_MINUTES,
    val maxScrolls: Int? = null,
    val direction: ScrollDirection = ScrollDirection.UP,
    val mode: AutomationMode = AutomationMode.AUTO_TIMER,
    val pauseOnTouch: Boolean = true,
    val resumeDelaySeconds: Int = SettingsLimits.DEFAULT_RESUME_DELAY_SECONDS,
) {
    /** Returns a copy with every value forced into its allowed range. */
    fun coerced(): SessionSettings = copy(
        intervalSeconds = intervalSeconds.coerceIn(
            SettingsLimits.MIN_INTERVAL_SECONDS,
            SettingsLimits.MAX_INTERVAL_SECONDS,
        ),
        maxDurationMinutes = if (maxDurationMinutes in SettingsLimits.DURATION_OPTIONS_MINUTES) {
            maxDurationMinutes
        } else {
            SettingsLimits.DEFAULT_DURATION_MINUTES
        },
        maxScrolls = maxScrolls?.coerceIn(SettingsLimits.MIN_MAX_SCROLLS, SettingsLimits.MAX_MAX_SCROLLS),
        resumeDelaySeconds = resumeDelaySeconds.coerceIn(0, SettingsLimits.MAX_RESUME_DELAY_SECONDS),
    )

    val intervalMillis: Long get() = intervalSeconds * 1000L
}

data class AppSettings(
    val session: SessionSettings = SessionSettings(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val adNotifications: Boolean = true,
    val haptics: Boolean = true,
    /** Shake/tilt the phone to go to the next Short. Off by default. */
    val motionNext: Boolean = false,
    val motionSensitivity: MotionSensitivity = MotionSensitivity.MEDIUM,
    /**
     * Opt-in: tap YouTube's own Skip button when it is likely on screen. Never taps the ad itself.
     * Forced off in flavors whose policy does not allow it. See docs/DECISIONS.md (ADR 9).
     */
    val autoSkipAds: Boolean = false,
    /** True once the user pressed the consent button on the disclosure screen. */
    val consentGiven: Boolean = false,
)

/** Which automation modes a build flavor may offer. A single value per flavor, not scattered checks. */
data class AutomationPolicy(
    val allowedModes: Set<AutomationMode>,
    val defaultMode: AutomationMode,
    /** Whether the Skip-ad control (manual button and opt-in automatic tap) exists in this flavor. */
    val adSkipAllowed: Boolean = false,
) {
    init {
        require(defaultMode in allowedModes) { "default mode must be allowed" }
        require(AutomationMode.WATCH_ADS !in allowedModes || adSkipAllowed) { "WATCH_ADS needs ad skipping" }
    }

    fun apply(settings: SessionSettings): SessionSettings =
        if (settings.mode in allowedModes) settings else settings.copy(mode = defaultMode)
}
