package org.example.autoshorts.core.automation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.example.autoshorts.core.model.AdState
import org.example.autoshorts.core.model.AutomationMode
import org.example.autoshorts.core.model.ErrorReason
import org.example.autoshorts.core.model.PauseCause
import org.example.autoshorts.core.model.ScreenState
import org.example.autoshorts.core.model.SessionSettings
import org.example.autoshorts.core.model.SessionState
import org.example.autoshorts.core.model.StopReason
import org.example.autoshorts.core.platformapi.GestureResult

/**
 * Owns one session at a time. All calls must come from the dispatcher that [scope] runs on
 * (the main thread in the app, the test scheduler in tests); the engine does no locking of its own
 * except the mutex that prevents two gestures from being in flight.
 *
 * Interval waits use `delay`, never polling, and nothing holds a wake lock.
 */
class SessionEngine(
    private val scope: CoroutineScope,
    private val scroller: Scroller,
    private val nowMillis: () -> Long,
    private val config: EngineConfig = EngineConfig(),
) {
    private val _model = MutableStateFlow(SessionModel())
    val model: StateFlow<SessionModel> = _model.asStateFlow()

    private val _stats = MutableStateFlow(EngineStats())
    val stats: StateFlow<EngineStats> = _stats.asStateFlow()

    private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    private var settings: SessionSettings = SessionSettings()

    /** Mode of the session that was last started (or the default before any start). */
    val activeMode: AutomationMode get() = settings.mode

    // Inputs reported by the service.
    private var permissionGranted = false
    private var targetInForeground = false
    private var userPaused = false
    private var touchPaused = false
    private var adPresent = false
    private var commentsOpen = false
    private var notInFeed = false

    // Scheduling.
    private var loopJob: Job? = null
    private var watchdogJob: Job? = null
    private var durationJob: Job? = null
    private var foregroundJob: Job? = null
    private var touchResumeJob: Job? = null
    private var adClearJob: Job? = null

    private val scrollMutex = Mutex()
    private var inFlight = false
    private var lastGestureEndMillis = Long.MIN_VALUE
    private var pendingStopReason = StopReason.USER

    // ---- Commands -------------------------------------------------------------------------

    fun start(sessionSettings: SessionSettings) {
        val state = _model.value.state
        if (state.isActive || state is SessionState.Starting) return
        settings = sessionSettings.coerced()
        _stats.value = EngineStats(lastStopReason = _stats.value.lastStopReason)
        userPaused = false
        touchPaused = false
        apply(SessionEvent.StartRequested(permissionGranted, targetInForeground))
    }

    fun stop(reason: StopReason = StopReason.USER) {
        pendingStopReason = reason
        apply(SessionEvent.StopRequested)
    }

    fun pause() {
        userPaused = true
        recomputePauses()
    }

    /** Explicit resume also clears a touch pause that is still waiting for confirmation. */
    fun resume() {
        userPaused = false
        touchPaused = false
        touchResumeJob?.cancel()
        recomputePauses()
    }

    /** MANUAL_TAP: one scroll per call. Ignored in AUTO_TIMER and whenever the session is not running. */
    fun manualScroll() {
        if (settings.mode != AutomationMode.MANUAL_TAP) return
        userScroll()
    }

    /**
     * One scroll requested by the user (motion gesture). Works in both modes while running, and while an ad
     * pause holds, because swiping past an ad is what the user would do by hand. The interval restarts after it.
     */
    fun userScroll() {
        val state = _model.value.state
        if (state != SessionState.Running && state != SessionState.PausedByAd) return
        scope.launch {
            if (performScroll()) restartLoop()
        }
    }

    // ---- Inputs from the service ----------------------------------------------------------

    fun onPermissionChanged(granted: Boolean) {
        permissionGranted = granted
        apply(SessionEvent.PermissionChanged(granted))
    }

    /** The accessibility service went away: same outcome as losing the permission. */
    fun onServiceUnbound() = onPermissionChanged(false)

    fun onScreenOff() = stop(StopReason.SCREEN_OFF)

    fun onForegroundChanged(isTarget: Boolean) {
        targetInForeground = isTarget
        apply(SessionEvent.ForegroundChanged(isTarget))
        syncJobs()
    }

    fun onScreenState(state: ScreenState) {
        commentsOpen = state == ScreenState.CommentsOpen
        notInFeed = state == ScreenState.OtherScreen
        recomputePauses()
    }

    fun onAdState(state: AdState) {
        if (state.isAdPresent) {
            adClearJob?.cancel()
            adClearJob = null
            adPresent = true
            recomputePauses()
        } else if (adPresent && adClearJob == null) {
            adClearJob = scope.launch {
                delay(config.adClearDebounceMillis)
                adPresent = false
                adClearJob = null
                recomputePauses()
            }
        }
    }

    /** A real touch or scroll by the user. Our own gestures are filtered out. */
    fun onUserInteraction() {
        if (!settings.pauseOnTouch || !_model.value.state.isActive) return
        if (inFlight || nowMillis() < lastGestureEndMillis + OWN_GESTURE_GRACE_MILLIS) return
        touchPaused = true
        touchResumeJob?.cancel()
        if (settings.resumeDelaySeconds > 0) {
            touchResumeJob = scope.launch {
                delay(settings.resumeDelaySeconds * 1000L)
                touchPaused = false
                recomputePauses()
            }
        }
        recomputePauses()
    }

    // ---- Internals ------------------------------------------------------------------------

    private fun desiredPauses(): Set<PauseCause> = buildSet {
        if (userPaused) add(PauseCause.USER)
        if (adPresent) add(PauseCause.AD)
        if (touchPaused) add(PauseCause.TOUCH)
        if (commentsOpen) add(PauseCause.COMMENTS)
        if (notInFeed) add(PauseCause.NOT_IN_FEED)
    }

    private fun recomputePauses() {
        if (!_model.value.state.isActive) return
        val desired = desiredPauses()
        val current = _model.value.pauses
        (desired - current).forEach { apply(SessionEvent.PauseAdded(it)) }
        (current - desired).forEach { apply(SessionEvent.PauseRemoved(it)) }
    }

    private fun apply(event: SessionEvent) {
        val before = _model.value
        val after = SessionReducer.reduce(before, event)
        if (after == before) return
        _model.value = after
        _stats.value = _stats.value.copy(lastTransitionAtMillis = nowMillis())
        onTransition(before, after)
    }

    private fun onTransition(before: SessionModel, after: SessionModel) {
        val wasLive = before.state.isActive || before.state is SessionState.Starting
        when {
            after.state is SessionState.Stopping -> {
                cancelAllJobs()
                _stats.value = _stats.value.copy(lastStopReason = pendingStopReason)
                _events.tryEmit(EngineEvent.Stopped(pendingStopReason))
                pendingStopReason = StopReason.USER
                apply(SessionEvent.StopCompleted)
            }

            after.state is SessionState.PermissionRequired && wasLive -> {
                cancelAllJobs()
                _stats.value = _stats.value.copy(lastStopReason = StopReason.SERVICE_UNBOUND)
                _events.tryEmit(EngineEvent.Stopped(StopReason.SERVICE_UNBOUND))
            }

            else -> {
                if (after.state is SessionState.Running && before.state is SessionState.Starting) recomputePauses()
                syncJobs()
            }
        }
    }

    private fun syncJobs() {
        val state = _model.value.state
        val live = state.isActive || state is SessionState.Starting

        if (!live) {
            cancelAllJobs()
            return
        }

        if (state.isActive && durationJob == null) {
            durationJob = scope.launch {
                delay(settings.maxDurationMinutes * 60_000L)
                stop(StopReason.DURATION_REACHED)
            }
        }

        if (!targetInForeground && foregroundJob == null) {
            foregroundJob = scope.launch {
                delay(config.foregroundGraceMillis)
                stop(StopReason.APP_LEFT_FOREGROUND)
            }
        } else if (targetInForeground) {
            foregroundJob?.cancel()
            foregroundJob = null
        }

        if (state is SessionState.Running) {
            if (loopJob == null) {
                loopJob = launchLoop()
            }
            // The watchdog only judges a session that is really working in the target app.
            if (targetInForeground && watchdogJob == null && settings.mode == AutomationMode.AUTO_TIMER) {
                startWatchdog()
            } else if (!targetInForeground) {
                watchdogJob?.cancel()
                watchdogJob = null
            }
        } else {
            loopJob?.cancel()
            loopJob = null
            watchdogJob?.cancel()
            watchdogJob = null
        }
    }

    private fun cancelAllJobs() {
        listOf(loopJob, watchdogJob, durationJob, foregroundJob, touchResumeJob, adClearJob).forEach { it?.cancel() }
        loopJob = null
        watchdogJob = null
        durationJob = null
        foregroundJob = null
        touchResumeJob = null
        adClearJob = null
        userPaused = false
        touchPaused = false
        adPresent = false
    }

    /** WATCH_ADS has no timer at all: the session only holds its limits while the host taps Skip. */
    private fun launchLoop(): Job? = when (settings.mode) {
        AutomationMode.AUTO_TIMER -> scope.launch { autoLoop() }
        AutomationMode.MANUAL_TAP -> scope.launch { reminderLoop() }
        AutomationMode.WATCH_ADS -> null
    }

    private suspend fun autoLoop() {
        while (true) {
            delay(settings.intervalMillis)
            if (_model.value.state == SessionState.Running) performScroll()
        }
    }

    private suspend fun reminderLoop() {
        while (true) {
            delay(settings.intervalMillis)
            if (_model.value.state == SessionState.Running) _events.tryEmit(EngineEvent.IntervalElapsed)
        }
    }

    private fun restartLoop() {
        if (_model.value.state != SessionState.Running) return
        loopJob?.cancel()
        loopJob = launchLoop()
    }

    private fun startWatchdog() {
        val window = maxOf(settings.intervalMillis * config.watchdogIntervals, config.watchdogMinMillis)
        watchdogJob = scope.launch {
            delay(window)
            apply(SessionEvent.Failed(ErrorReason.WATCHDOG))
        }
    }

    /** Returns false when another gesture was already in flight and nothing was dispatched. */
    private suspend fun performScroll(): Boolean {
        if (!scrollMutex.tryLock()) return false
        inFlight = true
        val result = try {
            scroller.scroll(settings.direction)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GestureResult.FAILED
        } finally {
            inFlight = false
            lastGestureEndMillis = nowMillis()
            scrollMutex.unlock()
        }
        record(result)
        return true
    }

    private fun record(result: GestureResult) {
        if (!_model.value.state.isActive) return
        val s = _stats.value
        if (result == GestureResult.SKIPPED) {
            _stats.value = s.copy(lastGestureResult = result)
            return
        }
        if (result == GestureResult.COMPLETED) {
            val count = s.scrollCount + 1
            _stats.value = s.copy(scrollCount = count, consecutiveFailures = 0, lastGestureResult = result)
            if (watchdogJob != null) {
                watchdogJob?.cancel()
                startWatchdog()
            }
            val limit = settings.maxScrolls
            if (limit != null && count >= limit) stop(StopReason.SCROLL_LIMIT_REACHED)
        } else {
            val failures = s.consecutiveFailures + 1
            _stats.value = s.copy(consecutiveFailures = failures, lastGestureResult = result)
            if (failures >= config.maxConsecutiveGestureFailures) apply(SessionEvent.Failed(ErrorReason.GESTURE_FAILURES))
        }
    }

    private companion object {
        /** Accessibility events right after our own swipe are not the user touching the screen. */
        const val OWN_GESTURE_GRACE_MILLIS = 1_000L
    }
}
