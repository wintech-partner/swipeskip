package io.github.kutlu24.swipeskip.core.platformapi

/** A single straight swipe in screen pixels. No randomization is ever applied. */
data class GestureSpec(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val durationMs: Long,
)

enum class GestureResult {
    COMPLETED,
    CANCELLED,
    FAILED,

    /** Nothing was dispatched because the target app was not the active window. Neither success nor failure. */
    SKIPPED,
}
