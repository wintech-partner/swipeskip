package io.github.kutlu24.swipeskip.core.platformapi

import io.github.kutlu24.swipeskip.core.model.AdState
import io.github.kutlu24.swipeskip.core.model.PlatformId
import io.github.kutlu24.swipeskip.core.model.ScreenState
import io.github.kutlu24.swipeskip.core.model.ScrollDirection

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

    /** Category name of the platform's own skip-ad control, or null if the platform has none. */
    val skipControlCategory: String?

    /**
     * Short view-id names of containers that hold an advertisement. A skip control is only ever clicked when it sits
     * inside one of these subtrees, so a look-alike label elsewhere on screen can never trigger a click.
     */
    val adContainerIds: Set<String>
}
