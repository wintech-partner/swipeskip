package org.example.autoshorts.core.platformapi

import org.example.autoshorts.core.model.AdState
import org.example.autoshorts.core.model.PlatformId
import org.example.autoshorts.core.model.ScreenState
import org.example.autoshorts.core.model.ScrollDirection

/** Looks at a snapshot and reports advertisement evidence. Never acts on it. */
interface AdvertisementMonitor {
    fun evaluate(snapshot: NodeSnapshot): AdState
}

/**
 * Everything platform specific. Adapters work on [NodeSnapshot] only, so they can be tested
 * with plain JVM tests and recorded fixtures.
 */
interface PlatformAdapter {
    val id: PlatformId
    val packageNames: Set<String>

    fun classify(snapshot: NodeSnapshot): ScreenState

    fun nextItemGesture(bounds: ScreenBounds, direction: ScrollDirection): GestureSpec

    val adMonitor: AdvertisementMonitor

    /**
     * Maps a content description to a category name such as "SKIP_AD", or null. Called by the
     * snapshot mapper; the raw text is discarded right after this call.
     */
    fun categorizeDescription(description: String): String?
}
