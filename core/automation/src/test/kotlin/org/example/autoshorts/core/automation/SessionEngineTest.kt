package org.example.autoshorts.core.automation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.example.autoshorts.core.model.AdState
import org.example.autoshorts.core.model.AutomationMode
import org.example.autoshorts.core.model.Confidence
import org.example.autoshorts.core.model.ErrorReason
import org.example.autoshorts.core.model.Evidence
import org.example.autoshorts.core.model.PauseCause
import org.example.autoshorts.core.model.ScreenState
import org.example.autoshorts.core.model.ScrollDirection
import org.example.autoshorts.core.model.SessionSettings
import org.example.autoshorts.core.model.SessionState
import org.example.autoshorts.core.model.StopReason
import org.example.autoshorts.core.platformapi.GestureResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionEngineTest {

    private class FakeScroller(var result: GestureResult = GestureResult.COMPLETED) : Scroller {
        val directions = mutableListOf<ScrollDirection>()
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun scroll(direction: ScrollDirection): GestureResult {
            directions += direction
            gate?.await()
            return result
        }
    }

    private val ad = AdState.AdLikely(Evidence(listOf("test"), Confidence.MEDIUM))
    private val settings = SessionSettings(intervalSeconds = 5, maxDurationMinutes = 15, resumeDelaySeconds = 5)

    private fun TestScope.engine(scroller: Scroller, config: EngineConfig = EngineConfig()) =
        SessionEngine(backgroundScope, scroller, { testScheduler.currentTime }, config).also {
            it.onPermissionChanged(true)
            it.onForegroundChanged(true)
        }

    private fun TestScope.advance(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test fun `scrolls once per interval`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings)
        advance(5_000); assertEquals(1, s.directions.size)
        advance(5_000); assertEquals(2, s.directions.size)
        assertEquals(2, e.stats.value.scrollCount)
    }

    @Test fun `uses the configured direction`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings.copy(direction = ScrollDirection.DOWN))
        advance(5_000)
        assertEquals(listOf(ScrollDirection.DOWN), s.directions)
    }

    @Test fun `stop cancels pending work immediately`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings)
        advance(4_000)
        e.stop()
        advance(60_000)
        assertEquals(0, s.directions.size)
        assertEquals(SessionState.Idle, e.model.value.state)
    }

    @Test fun `start without permission does nothing`() = runTest {
        val s = FakeScroller()
        val e = SessionEngine(backgroundScope, s, { testScheduler.currentTime })
        e.start(settings)
        advance(60_000)
        assertEquals(SessionState.PermissionRequired, e.model.value.state)
        assertTrue(s.directions.isEmpty())
    }

    @Test fun `permission revoked mid session cancels everything`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings)
        advance(5_000)
        e.onServiceUnbound()
        advance(60_000)
        assertEquals(SessionState.PermissionRequired, e.model.value.state)
        assertEquals(1, s.directions.size)
        assertEquals(StopReason.SERVICE_UNBOUND, e.stats.value.lastStopReason)
    }

    @Test fun `session ends when the duration limit is reached`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings.copy(maxDurationMinutes = 15))
        advance(15 * 60_000L)
        assertEquals(SessionState.Idle, e.model.value.state)
        assertEquals(StopReason.DURATION_REACHED, e.stats.value.lastStopReason)
    }

    @Test fun `session ends at the scroll limit`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings.copy(maxScrolls = 3))
        advance(60_000)
        assertEquals(3, s.directions.size)
        assertEquals(StopReason.SCROLL_LIMIT_REACHED, e.stats.value.lastStopReason)
        assertEquals(SessionState.Idle, e.model.value.state)
    }

    @Test fun `screen off stops the session`() = runTest {
        val e = engine(FakeScroller())
        e.start(settings)
        e.onScreenOff()
        assertEquals(StopReason.SCREEN_OFF, e.stats.value.lastStopReason)
        assertEquals(SessionState.Idle, e.model.value.state)
    }

    @Test fun `leaving the target app for 60 seconds stops the session`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings)
        e.onForegroundChanged(false)
        advance(59_000)
        assertTrue(e.model.value.state.isActive)
        advance(1_000)
        assertEquals(StopReason.APP_LEFT_FOREGROUND, e.stats.value.lastStopReason)
        assertEquals(SessionState.Idle, e.model.value.state)
    }

    @Test fun `returning to the target app cancels the foreground timeout`() = runTest {
        val e = engine(FakeScroller())
        e.start(settings)
        e.onForegroundChanged(false)
        advance(30_000)
        e.onForegroundChanged(true)
        advance(120_000)
        assertTrue(e.model.value.state.isActive)
    }

    @Test fun `starting outside the target app waits in Starting`() = runTest {
        val s = FakeScroller()
        val e = SessionEngine(backgroundScope, s, { testScheduler.currentTime })
        e.onPermissionChanged(true)
        e.start(settings)
        assertEquals(SessionState.Starting, e.model.value.state)
        e.onForegroundChanged(true)
        assertEquals(SessionState.Running, e.model.value.state)
        advance(5_000)
        assertEquals(1, s.directions.size)
    }

    @Test fun `ad pauses then resumes only after the debounce`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings)
        e.onAdState(ad)
        assertEquals(SessionState.PausedByAd, e.model.value.state)
        advance(20_000)
        assertEquals(0, s.directions.size)

        e.onAdState(AdState.NoAdEvidence)
        advance(1_900)
        assertEquals(SessionState.PausedByAd, e.model.value.state)
        advance(200)
        assertEquals(SessionState.Running, e.model.value.state)
        advance(5_000)
        assertEquals(1, s.directions.size)
    }

    @Test fun `ad evidence returning inside the debounce keeps the pause`() = runTest {
        val e = engine(FakeScroller())
        e.start(settings)
        e.onAdState(ad)
        e.onAdState(AdState.NoAdEvidence)
        advance(1_000)
        e.onAdState(ad)
        advance(10_000)
        assertEquals(SessionState.PausedByAd, e.model.value.state)
    }

    @Test fun `unknown ad detection never pauses`() = runTest {
        val e = engine(FakeScroller())
        e.start(settings)
        e.onAdState(AdState.Unknown)
        assertEquals(SessionState.Running, e.model.value.state)
    }

    @Test fun `touch pauses and resumes after the configured delay`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings)
        advance(1)
        e.onUserInteraction()
        assertEquals(SessionState.PausedByInteraction(PauseCause.TOUCH), e.model.value.state)
        advance(4_999)
        assertEquals(SessionState.PausedByInteraction(PauseCause.TOUCH), e.model.value.state)
        advance(1)
        assertEquals(SessionState.Running, e.model.value.state)
    }

    @Test fun `touch with zero delay waits for explicit resume`() = runTest {
        val e = engine(FakeScroller())
        e.start(settings.copy(resumeDelaySeconds = 0))
        e.onUserInteraction()
        advance(120_000)
        assertEquals(SessionState.PausedByInteraction(PauseCause.TOUCH), e.model.value.state)
        e.resume()
        assertEquals(SessionState.Running, e.model.value.state)
    }

    @Test fun `touch pause can be disabled`() = runTest {
        val e = engine(FakeScroller())
        e.start(settings.copy(pauseOnTouch = false))
        e.onUserInteraction()
        assertEquals(SessionState.Running, e.model.value.state)
    }

    @Test fun `events during our own gesture are not user touches`() = runTest {
        val s = FakeScroller().apply { gate = CompletableDeferred() }
        val e = engine(s)
        e.start(settings)
        advance(5_000)
        e.onUserInteraction() // gesture still in flight
        assertEquals(SessionState.Running, e.model.value.state)
        s.gate!!.complete(Unit)
        runCurrent()
        e.onUserInteraction() // inside the post-gesture grace window
        assertEquals(SessionState.Running, e.model.value.state)
    }

    @Test fun `comments open pauses until closed`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings)
        e.onScreenState(ScreenState.CommentsOpen)
        assertEquals(SessionState.PausedByInteraction(PauseCause.COMMENTS), e.model.value.state)
        advance(30_000)
        assertEquals(0, s.directions.size)
        e.onScreenState(ScreenState.InTargetFeed)
        assertEquals(SessionState.Running, e.model.value.state)
    }

    @Test fun `user pause and resume`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings)
        e.pause()
        assertEquals(SessionState.PausedByUser, e.model.value.state)
        advance(30_000)
        assertEquals(0, s.directions.size)
        e.resume()
        advance(5_000)
        assertEquals(1, s.directions.size)
    }

    @Test fun `resuming restarts the interval instead of firing immediately`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings)
        advance(4_000)
        e.pause()
        e.resume()
        advance(4_000)
        assertEquals(0, s.directions.size)
        advance(1_000)
        assertEquals(1, s.directions.size)
    }

    @Test fun `ad seen before running is applied when the session starts running`() = runTest {
        val e = SessionEngine(backgroundScope, FakeScroller(), { testScheduler.currentTime })
        e.onPermissionChanged(true)
        e.start(settings)
        e.onAdState(ad) // Starting: not active yet, flag only
        e.onForegroundChanged(true)
        assertEquals(SessionState.PausedByAd, e.model.value.state)
    }

    @Test fun `three consecutive gesture failures move to Error`() = runTest {
        val s = FakeScroller(GestureResult.FAILED)
        val e = engine(s)
        e.start(settings)
        advance(15_000)
        assertEquals(SessionState.Error(ErrorReason.GESTURE_FAILURES), e.model.value.state)
        advance(60_000)
        assertEquals(3, s.directions.size)
    }

    @Test fun `a success resets the failure streak`() = runTest {
        val s = FakeScroller(GestureResult.FAILED)
        val e = engine(s)
        e.start(settings)
        advance(10_000)
        s.result = GestureResult.COMPLETED
        advance(5_000)
        s.result = GestureResult.CANCELLED
        advance(10_000)
        assertTrue(e.model.value.state.isActive)
        assertEquals(2, e.stats.value.consecutiveFailures)
    }

    @Test fun `scroller throwing counts as a failure`() = runTest {
        val e = engine(Scroller { error("boom") })
        e.start(settings)
        advance(5_000)
        assertEquals(1, e.stats.value.consecutiveFailures)
        assertTrue(e.model.value.state.isActive)
    }

    @Test fun `never two gestures in flight`() = runTest {
        val s = FakeScroller().apply { gate = CompletableDeferred() }
        val e = engine(s)
        e.start(settings.copy(intervalSeconds = 3))
        advance(30_000)
        assertEquals(1, s.directions.size)
    }

    @Test fun `watchdog moves a running session with no successful gesture to Error`() = runTest {
        // A gesture that never returns: no success, no failure, interval keeps elapsing.
        val s = FakeScroller().apply { gate = CompletableDeferred() }
        val e = engine(s)
        e.start(settings)
        advance(29_999)
        assertTrue(e.model.value.state.isActive)
        advance(1)
        assertEquals(SessionState.Error(ErrorReason.WATCHDOG), e.model.value.state)
    }

    @Test fun `watchdog is fed by successful gestures`() = runTest {
        val e = engine(FakeScroller())
        e.start(settings)
        advance(10 * 60_000L)
        assertEquals(SessionState.Running, e.model.value.state)
    }

    @Test fun `manual tap mode never scrolls by itself`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings.copy(mode = AutomationMode.MANUAL_TAP))
        advance(120_000)
        assertTrue(s.directions.isEmpty())
        assertEquals(SessionState.Running, e.model.value.state)
    }

    @Test fun `manual tap mode reminds each interval and scrolls once per tap`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        val seen = mutableListOf<EngineEvent>()
        backgroundScope.launchCollect(e, seen)
        e.start(settings.copy(mode = AutomationMode.MANUAL_TAP))
        advance(5_000)
        assertEquals(listOf(EngineEvent.IntervalElapsed), seen)
        e.manualScroll()
        runCurrent()
        assertEquals(1, s.directions.size)
        assertEquals(1, e.stats.value.scrollCount)
    }

    @Test fun `manual scroll is ignored in auto mode and when paused`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings)
        e.manualScroll()
        runCurrent()
        assertTrue(s.directions.isEmpty())

        val m = engine(s)
        m.start(settings.copy(mode = AutomationMode.MANUAL_TAP))
        m.pause()
        m.manualScroll()
        runCurrent()
        assertTrue(s.directions.isEmpty())
    }

    @Test fun `settings are coerced into range`() = runTest {
        val s = FakeScroller()
        val e = engine(s)
        e.start(settings.copy(intervalSeconds = 1))
        advance(3_000)
        assertEquals(1, s.directions.size)
    }

    @Test fun `stats reset on a new session but remember the last stop reason`() = runTest {
        val e = engine(FakeScroller())
        e.start(settings.copy(maxScrolls = 1))
        advance(5_000)
        assertEquals(StopReason.SCROLL_LIMIT_REACHED, e.stats.value.lastStopReason)
        e.start(settings)
        assertEquals(0, e.stats.value.scrollCount)
        assertEquals(StopReason.SCROLL_LIMIT_REACHED, e.stats.value.lastStopReason)
        assertNull(e.stats.value.lastGestureResult)
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchCollect(e: SessionEngine, into: MutableList<EngineEvent>) {
    launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { e.events.collect { into += it } }
}
