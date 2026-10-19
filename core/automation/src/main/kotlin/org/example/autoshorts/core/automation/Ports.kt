package org.example.autoshorts.core.automation

import org.example.autoshorts.core.model.ScrollDirection
import org.example.autoshorts.core.platformapi.GestureResult

/** Performs one swipe to the next item. Implemented by the accessibility service. */
fun interface Scroller {
    suspend fun scroll(direction: ScrollDirection): GestureResult
}

/** Timing knobs. Defaults come from the product rules; tests shrink them. */
data class EngineConfig(
    /** Session ends when the target app stayed out of the foreground this long. */
    val foregroundGraceMillis: Long = 60_000,
    /** Ad evidence must be gone this long before the ad pause is lifted. */
    val adClearDebounceMillis: Long = 2_000,
    val maxConsecutiveGestureFailures: Int = 3,
    /** Watchdog fires after this many intervals without a successful gesture. */
    val watchdogIntervals: Int = 4,
    val watchdogMinMillis: Long = 30_000,
)

/** One-shot signals for the UI and the service (haptics, notifications). */
sealed interface EngineEvent {
    /** MANUAL_TAP only: the interval elapsed, the user may tap to scroll. */
    data object IntervalElapsed : EngineEvent
    data class Stopped(val reason: org.example.autoshorts.core.model.StopReason) : EngineEvent
}

data class EngineStats(
    val scrollCount: Int = 0,
    val consecutiveFailures: Int = 0,
    val lastGestureResult: GestureResult? = null,
    val lastStopReason: org.example.autoshorts.core.model.StopReason? = null,
    val lastTransitionAtMillis: Long? = null,
)
