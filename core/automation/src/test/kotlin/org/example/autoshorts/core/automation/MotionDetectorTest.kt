package org.example.autoshorts.core.automation

import org.example.autoshorts.core.model.MotionSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionDetectorTest {
    private fun MotionDetector.rest(from: Long, samples: Int) {
        repeat(samples) { onSample(from + it * 20L, 0f, 0f, 9.81f) }
    }

    @Test fun `a phone lying still never triggers`() {
        val d = MotionDetector(11f)
        repeat(500) { assertFalse(d.onSample(it * 20L, 0.1f, -0.1f, 9.8f)) }
    }

    @Test fun `gravity alone is not a flick whatever the orientation`() {
        val d = MotionDetector(11f)
        repeat(200) { assertFalse(d.onSample(it * 20L, 9.81f, 0f, 0f)) }
    }

    @Test fun `a sideways flick triggers`() {
        val d = MotionDetector(11f)
        d.rest(0, 50)
        assertTrue(d.onSample(1_000, 20f, 0f, 9.81f))
    }

    @Test fun `a forward flick triggers`() {
        val d = MotionDetector(11f)
        d.rest(0, 50)
        assertTrue(d.onSample(1_000, 0f, 0f, 25f))
    }

    @Test fun `one flick triggers once then cools down`() {
        val d = MotionDetector(11f, cooldownMillis = 1_200)
        d.rest(0, 50)
        assertTrue(d.onSample(1_000, 20f, 0f, 9.81f))
        assertFalse(d.onSample(1_020, -20f, 0f, 9.81f)) // the rebound
        assertFalse(d.onSample(2_199, 20f, 0f, 9.81f))
    }

    @Test fun `a second flick after the cooldown triggers again`() {
        val d = MotionDetector(11f, cooldownMillis = 1_200)
        d.rest(0, 50)
        assertTrue(d.onSample(1_000, 20f, 0f, 9.81f))
        d.rest(1_100, 50)
        assertTrue(d.onSample(3_000, 20f, 0f, 9.81f))
    }

    @Test fun `a gentle tilt below the threshold does not trigger`() {
        val d = MotionDetector(11f)
        d.rest(0, 50)
        assertFalse(d.onSample(1_000, 4f, 0f, 9.81f))
    }

    @Test fun `higher sensitivity triggers on weaker motion`() {
        fun triggers(s: MotionSensitivity): Boolean {
            val d = MotionDetector.forSensitivity(s)
            d.rest(0, 50)
            return d.onSample(1_000, 11f, 0f, 9.81f) // filtered linear part is about 0.8 x 11
        }
        assertTrue(triggers(MotionSensitivity.HIGH))
        assertFalse(triggers(MotionSensitivity.MEDIUM))
        assertFalse(triggers(MotionSensitivity.LOW))
    }
}
