package io.github.wintech_partner.swipeskip.ui

import androidx.annotation.StringRes
import io.github.wintech_partner.swipeskip.R
import io.github.wintech_partner.swipeskip.core.model.AdState
import io.github.wintech_partner.swipeskip.core.model.AutomationMode
import io.github.wintech_partner.swipeskip.core.model.ErrorReason
import io.github.wintech_partner.swipeskip.core.model.PauseCause
import io.github.wintech_partner.swipeskip.core.model.SessionState
import io.github.wintech_partner.swipeskip.core.model.StopReason

@StringRes
fun SessionState.labelRes(): Int = when (this) {
    SessionState.Idle -> R.string.status_idle
    SessionState.Starting -> R.string.status_starting
    SessionState.Running -> R.string.status_running
    SessionState.PausedByUser -> R.string.status_paused_user
    SessionState.PausedByAd -> R.string.status_paused_ad
    is SessionState.PausedByInteraction -> when (cause) {
        PauseCause.COMMENTS -> R.string.status_paused_comments
        PauseCause.NOT_IN_FEED -> R.string.status_paused_not_in_feed
        else -> R.string.status_paused_touch
    }
    SessionState.PermissionRequired -> R.string.status_permission_required
    SessionState.Stopping -> R.string.status_stopping
    is SessionState.Error -> R.string.status_error
}

@StringRes
fun ErrorReason.labelRes(): Int = when (this) {
    ErrorReason.GESTURE_FAILURES -> R.string.error_gesture_failures
    ErrorReason.WATCHDOG -> R.string.error_watchdog
    ErrorReason.INTERNAL -> R.string.error_internal
}

@StringRes
fun StopReason.labelRes(): Int = when (this) {
    StopReason.USER -> R.string.stop_reason_user
    StopReason.DURATION_REACHED -> R.string.stop_reason_duration
    StopReason.SCROLL_LIMIT_REACHED -> R.string.stop_reason_scroll_limit
    StopReason.SCREEN_OFF -> R.string.stop_reason_screen_off
    StopReason.APP_LEFT_FOREGROUND -> R.string.stop_reason_app_left
    StopReason.SERVICE_UNBOUND -> R.string.stop_reason_service_unbound
}

/** Wording rule: "likely", never "detected". */
@StringRes
fun AdState.labelRes(): Int = when (this) {
    AdState.Unknown -> R.string.ad_unknown
    AdState.NoAdEvidence -> R.string.ad_none
    is AdState.AdLikely -> R.string.ad_likely
    is AdState.SkipLikelyAvailable -> R.string.ad_skip_likely
}

@StringRes
fun AutomationMode.labelRes(): Int = when (this) {
    AutomationMode.AUTO_TIMER -> R.string.home_mode_auto
    AutomationMode.MANUAL_TAP -> R.string.home_mode_manual
    AutomationMode.WATCH_ADS -> R.string.home_mode_watch
}
