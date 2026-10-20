package io.github.kutlu24.swipeskip.core.automation

import io.github.kutlu24.swipeskip.core.model.MotionSensitivity
import kotlin.math.sqrt

/**
 * Detects a deliberate flick of the phone (any direction) from raw accelerometer samples.
 * Gravity is removed with a low-pass filter; a sample whose linear acceleration reaches the threshold
 * triggers once, then the detector stays quiet for [cooldownMillis]. Pure and unit-tested; the
 * thresholds are first guesses that need tuning on a device (docs/OPEN_ITEMS.md).
 */
class MotionDetector(
    private val thresholdMs2: Float,
    private val cooldownMillis: Long = 1_200,
) {
    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private var primed = false
    private var lastTriggerAt = Long.MIN_VALUE

    /** Feed one sample (m/s^2). Returns true when this sample is a flick that should trigger a scroll. */
    fun onSample(timeMillis: Long, x: Float, y: Float, z: Float): Boolean {
        if (!primed) {
            gx = x; gy = y; gz = z
            primed = true
            return false
        }
        gx = ALPHA * gx + (1 - ALPHA) * x
        gy = ALPHA * gy + (1 - ALPHA) * y
        gz = ALPHA * gz + (1 - ALPHA) * z
        val lx = x - gx
        val ly = y - gy
        val lz = z - gz
        val magnitude = sqrt(lx * lx + ly * ly + lz * lz)
        if (magnitude < thresholdMs2) return false
        if (lastTriggerAt != Long.MIN_VALUE && timeMillis - lastTriggerAt < cooldownMillis) return false
        lastTriggerAt = timeMillis
        return true
    }

    companion object {
        private const val ALPHA = 0.8f

        fun forSensitivity(s: MotionSensitivity) = MotionDetector(
            when (s) {
                MotionSensitivity.LOW -> 14f
                MotionSensitivity.MEDIUM -> 11f
                MotionSensitivity.HIGH -> 8f
            },
        )
    }
}
