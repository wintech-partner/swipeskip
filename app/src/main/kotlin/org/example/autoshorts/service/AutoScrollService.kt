package org.example.autoshorts.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.suspendCancellableCoroutine
import org.example.autoshorts.AutoshortsApp
import org.example.autoshorts.core.model.ScrollDirection
import org.example.autoshorts.core.platformapi.GestureResult
import org.example.autoshorts.core.platformapi.NodeSnapshot
import org.example.autoshorts.core.platformapi.ScreenBounds
import kotlin.coroutines.resume

/**
 * Thin Android shell. Everything decision-making lives in [SessionHost] and the pure core modules.
 * While no session is live this service does nothing: no snapshots, no gestures.
 */
class AutoScrollService : AccessibilityService() {
    private val host: SessionHost
        get() = (application as AutoshortsApp).container.host

    override fun onServiceConnected() {
        host.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        host.onAccessibilityEvent(event.eventType, event.packageName?.toString())
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        host.detach(this)
        return super.onUnbind(intent)
    }

    /** Snapshot of the active window, or null when there is none (or it is not the target app). */
    internal fun snapshotActiveWindow(): NodeSnapshot? {
        val root = rootInActiveWindow ?: return null
        return SnapshotMapper.map(root, host.adapter)
    }

    /** Package of the active window, read without walking the tree. */
    internal fun activeWindowPackage(): String? = rootInActiveWindow?.packageName?.toString()

    internal fun activeWindowBounds(): ScreenBounds? {
        val root = rootInActiveWindow ?: return null
        val r = Rect().also { root.getBoundsInScreen(it) }
        return ScreenBounds(r.left, r.top, r.right, r.bottom)
    }

    /** Dispatches one swipe and suspends until the system reports the result. */
    internal suspend fun swipe(bounds: ScreenBounds, direction: ScrollDirection): GestureResult {
        val spec = host.adapter.nextItemGesture(bounds, direction)
        val path = Path().apply {
            moveTo(spec.startX, spec.startY)
            lineTo(spec.endX, spec.endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, spec.durationMs))
            .build()
        return suspendCancellableCoroutine { cont ->
            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(GestureResult.COMPLETED)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(GestureResult.CANCELLED)
                }
            }
            if (!dispatchGesture(gesture, callback, null) && cont.isActive) cont.resume(GestureResult.FAILED)
        }
    }
}
