package io.github.wintech_partner.swipeskip.core.automation

/**
 * Rate limit for tapping the platform's own Skip control. Pure, no clock of its own.
 * At most [maxAttemptsPerAd] taps per ad, at least [cooldownMillis] apart; [onAdCleared] starts a fresh ad.
 */
class AdSkipGate(
    private val cooldownMillis: Long = 2_000,
    private val maxAttemptsPerAd: Int = 3,
) {
    private var attempts = 0
    private var lastAttemptAt = Long.MIN_VALUE

    /** True if a tap may happen now; records the attempt when it does. */
    fun tryAcquire(nowMillis: Long): Boolean {
        if (attempts >= maxAttemptsPerAd) return false
        if (lastAttemptAt != Long.MIN_VALUE && nowMillis - lastAttemptAt < cooldownMillis) return false
        attempts++
        lastAttemptAt = nowMillis
        return true
    }

    fun onAdCleared() {
        attempts = 0
        lastAttemptAt = Long.MIN_VALUE
    }
}
