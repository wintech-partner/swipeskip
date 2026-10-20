package io.github.wintech_partner.swipeskip.core.automation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdSkipGateTest {
    @Test fun `first attempt is allowed`() = assertTrue(AdSkipGate().tryAcquire(0))

    @Test fun `attempts closer than the cooldown are refused`() {
        val g = AdSkipGate(cooldownMillis = 2_000)
        assertTrue(g.tryAcquire(0))
        assertFalse(g.tryAcquire(1_999))
        assertTrue(g.tryAcquire(2_000))
    }

    @Test fun `at most three attempts per ad`() {
        val g = AdSkipGate(cooldownMillis = 1_000, maxAttemptsPerAd = 3)
        assertTrue(g.tryAcquire(0)); assertTrue(g.tryAcquire(1_000)); assertTrue(g.tryAcquire(2_000))
        assertFalse(g.tryAcquire(10_000))
    }

    @Test fun `a new ad starts fresh`() {
        val g = AdSkipGate(maxAttemptsPerAd = 1)
        assertTrue(g.tryAcquire(0))
        assertFalse(g.tryAcquire(10_000))
        g.onAdCleared()
        assertTrue(g.tryAcquire(10_001))
    }
}
