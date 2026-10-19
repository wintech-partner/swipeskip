package org.example.autoshorts.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.example.autoshorts.core.automation.AdSkipGate
import org.example.autoshorts.core.automation.EngineEvent
import org.example.autoshorts.core.automation.MotionDetector
import org.example.autoshorts.core.automation.Scroller
import org.example.autoshorts.core.automation.SessionEngine
import org.example.autoshorts.core.model.AdState
import org.example.autoshorts.core.model.ScreenState
import org.example.autoshorts.core.model.SessionState
import org.example.autoshorts.core.platformapi.GestureResult
import org.example.autoshorts.core.platformapi.PlatformAdapter
import org.example.autoshorts.data.settings.SettingsRepository
import org.example.autoshorts.diagnostics.TransitionLog

/** What the platform adapter last concluded. Structural labels only, never screen text. */
data class LiveObservation(
    val screenState: ScreenState = ScreenState.Unknown,
    val adState: AdState = AdState.Unknown,
    val targetInForeground: Boolean = false,
)

/**
 * App-wide bridge between the Android service, the pure [SessionEngine] and the UI.
 * One engine lives for the whole process; the service only supplies a gesture channel and events.
 */
class SessionHost(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settings: SettingsRepository,
    val adapter: PlatformAdapter,
    private val adSkipAllowed: Boolean,
) {
    private var service: AutoScrollService? = null

    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    private val _observation = MutableStateFlow(LiveObservation())
    val observation: StateFlow<LiveObservation> = _observation.asStateFlow()

    val transitions = TransitionLog()

    val engine = SessionEngine(
        scope = scope,
        scroller = Scroller { direction -> performScroll(direction) },
        nowMillis = System::currentTimeMillis,
    )

    private val notifier = SessionNotifier(context)
    private val skipGate = AdSkipGate()
    private var motionDetector: MotionDetector? = null
    private var motionRegistered = false
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val motionListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val d = motionDetector ?: return
            if (d.onSample(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2])) engine.userScroll()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    private var snapshotJob: Job? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            engine.onScreenOff()
        }
    }

    init {
        engine.model.onEach { model ->
            transitions.record(model.state)
            val s = settings.settings.first()
            notifier.update(model.state, engine.stats.value, s.session.mode, _observation.value.adState)
            // The accelerometer is only listened to while a session is live and the user turned this on.
            val live = model.state.isActive
            if (live && s.motionNext) startMotion(s.motionSensitivity) else stopMotion()
            if (!live) skipGate.onAdCleared()
        }.launchIn(scope)

        engine.events.onEach { event ->
            if (event is EngineEvent.IntervalElapsed && settings.settings.first().haptics) vibrate()
        }.launchIn(scope)

        _observation.onEach { obs ->
            val s = settings.settings.first()
            if (s.adNotifications) notifier.updateAd(engine.model.value.state, obs.adState, adSkipAllowed)
            if (!obs.adState.isAdPresent) skipGate.onAdCleared()
            if (obs.adState is AdState.SkipLikelyAvailable && s.autoSkipAds && adSkipAllowed &&
                engine.model.value.state.isActive && skipGate.tryAcquire(System.currentTimeMillis())
            ) {
                service?.clickSkipControl()
            }
        }.launchIn(scope)
    }

    // ---- Service lifecycle ----

    fun attach(service: AutoScrollService) {
        this.service = service
        _serviceConnected.value = true
        context.registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        engine.onPermissionChanged(true)
    }

    fun detach(service: AutoScrollService) {
        if (this.service !== service) return
        this.service = null
        _serviceConnected.value = false
        runCatching { context.unregisterReceiver(screenOffReceiver) }
        snapshotJob?.cancel()
        snapshotJob = null
        _observation.value = LiveObservation()
        stopMotion()
        engine.onServiceUnbound()
        notifier.cancelAll()
    }

    // ---- Commands from the UI and the notification ----

    fun start() {
        scope.launch {
            val s = settings.settings.first()
            if (!s.consentGiven) return@launch
            // Know right away whether the target app is already in front.
            val svc = service
            val inFront = svc?.activeWindowPackage() in adapter.packageNames
            engine.onForegroundChanged(inFront)
            engine.start(s.session)
        }
    }

    fun stop() = engine.stop()
    fun pause() = engine.pause()
    fun resume() = engine.resume()
    fun manualScroll() = engine.manualScroll()

    /** The user pressed "Skip ad": tap the platform's own Skip control if one is visible. Needs a live session. */
    fun skipAd() {
        if (!adSkipAllowed || !engine.model.value.state.isActive) return
        service?.clickSkipControl()
    }

    private fun startMotion(sensitivity: org.example.autoshorts.core.model.MotionSensitivity) {
        if (motionRegistered) return
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        motionDetector = MotionDetector.forSensitivity(sensitivity)
        motionRegistered = sensorManager.registerListener(motionListener, accel, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun stopMotion() {
        if (!motionRegistered) return
        sensorManager.unregisterListener(motionListener)
        motionRegistered = false
        motionDetector = null
    }

    // ---- Events from the service ----

    /** Called for every event the service receives. Returns immediately when no session is live. */
    fun onAccessibilityEvent(eventType: Int, packageName: String?) {
        val state = engine.model.value.state
        if (!(state.isActive || state is SessionState.Starting)) return
        if (packageName !in adapter.packageNames) return

        engine.onForegroundChanged(true)
        _observation.value = _observation.value.copy(targetInForeground = true)

        when (eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> engine.onUserInteraction()

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> scheduleSnapshot()
        }
    }

    /** Trailing-edge throttle: at most one tree walk per [SNAPSHOT_THROTTLE_MILLIS]. */
    private fun scheduleSnapshot() {
        if (snapshotJob != null) return
        snapshotJob = scope.launch {
            delay(SNAPSHOT_THROTTLE_MILLIS)
            snapshotJob = null
            val state = engine.model.value.state
            if (!(state.isActive || state is SessionState.Starting)) return@launch
            val snapshot = service?.snapshotActiveWindow() ?: return@launch
            val screen = adapter.classify(snapshot)
            val ad = adapter.adMonitor.evaluate(snapshot)
            _observation.value = _observation.value.copy(screenState = screen, adState = ad)
            engine.onScreenState(screen)
            engine.onAdState(ad)
        }
    }

    // ---- Gesture channel ----

    private suspend fun performScroll(direction: org.example.autoshorts.core.model.ScrollDirection): GestureResult {
        val svc = service ?: return GestureResult.SKIPPED
        // Never swipe over another app: the active window must be the target.
        if (svc.activeWindowPackage() !in adapter.packageNames) {
            engine.onForegroundChanged(false)
            _observation.value = _observation.value.copy(targetInForeground = false)
            return GestureResult.SKIPPED
        }
        val bounds = svc.activeWindowBounds() ?: return GestureResult.SKIPPED
        if (bounds.isEmpty) return GestureResult.FAILED
        return svc.swipe(bounds, direction)
    }

    private fun vibrate() {
        val effect = VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(effect)
        }
    }

    private companion object {
        const val SNAPSHOT_THROTTLE_MILLIS = 400L
    }
}
