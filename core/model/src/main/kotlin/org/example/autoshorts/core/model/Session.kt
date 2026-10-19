package org.example.autoshorts.core.model

/** Why a session is paused. Several can hold at once. */
enum class PauseCause { USER, AD, TOUCH, COMMENTS, NOT_IN_FEED }

enum class ErrorReason {
    /** Three gestures in a row were rejected or cancelled by the system. */
    GESTURE_FAILURES,

    /** Running, but no successful gesture for several intervals. */
    WATCHDOG,

    /** Unexpected failure inside the engine. */
    INTERNAL,
}

enum class StopReason {
    USER,
    DURATION_REACHED,
    SCROLL_LIMIT_REACHED,
    SCREEN_OFF,
    APP_LEFT_FOREGROUND,
    SERVICE_UNBOUND,
}

sealed interface SessionState {
    data object Idle : SessionState
    data object Starting : SessionState
    data object Running : SessionState
    data object PausedByUser : SessionState
    data object PausedByAd : SessionState
    data class PausedByInteraction(val cause: PauseCause) : SessionState
    data object PermissionRequired : SessionState
    data object Stopping : SessionState
    data class Error(val reason: ErrorReason) : SessionState

    /** Running or any pause: a session exists and its limits are ticking. */
    val isActive: Boolean
        get() = this is Running || this is PausedByUser || this is PausedByAd || this is PausedByInteraction
}
