package io.github.kutlu24.swipeskip.platform.youtube

import io.github.kutlu24.swipeskip.core.model.AdState
import io.github.kutlu24.swipeskip.core.model.Confidence
import io.github.kutlu24.swipeskip.core.model.Evidence
import io.github.kutlu24.swipeskip.core.model.PlatformId
import io.github.kutlu24.swipeskip.core.model.ScreenState
import io.github.kutlu24.swipeskip.core.model.ScrollDirection
import io.github.kutlu24.swipeskip.core.platformapi.AdvertisementMonitor
import io.github.kutlu24.swipeskip.core.platformapi.GestureSpec
import io.github.kutlu24.swipeskip.core.platformapi.NodeSnapshot
import io.github.kutlu24.swipeskip.core.platformapi.PlatformAdapter
import io.github.kutlu24.swipeskip.core.platformapi.ScreenBounds
import java.util.Locale

private fun NodeSnapshot.shortId(): String? = viewId?.substringAfter(":id/", missingDelimiterValue = viewId!!)

private fun Sequence<NodeSnapshot>.visible() = filter { it.visibleToUser }

/**
 * [extraPackages] exists only so debug builds can point the adapter at the fake Shorts test app.
 * Release builds never pass it.
 */
class YouTubeShortsAdapter(extraPackages: Set<String> = emptySet()) : PlatformAdapter {
    override val id = PlatformId.YOUTUBE_SHORTS
    override val packageNames = setOf(YouTubeMatchers.PACKAGE) + extraPackages
    override val adMonitor: AdvertisementMonitor = YouTubeAdMonitor(packageNames)
    override val skipControlCategory: String = YouTubeMatchers.Categories.SKIP_AD
    override val adContainerIds: Set<String> = YouTubeMatchers.Ids.skipContainers

    override fun classify(snapshot: NodeSnapshot): ScreenState {
        if (snapshot.packageName !in packageNames) return ScreenState.Unknown
        val ids = snapshot.walk().visible().mapNotNull { it.shortId() }.toSet()
        return when {
            ids.any { it in YouTubeMatchers.Ids.commentsPanelMarkers } -> ScreenState.CommentsOpen
            ids.any { it in YouTubeMatchers.Ids.feedMarkers } -> ScreenState.InTargetFeed
            ids.any { it in YouTubeMatchers.Ids.otherScreenMarkers } &&
                snapshot.nodeCount >= YouTubeMatchers.MIN_NODES_FOR_OTHER_SCREEN -> ScreenState.OtherScreen
            // No positive evidence either way: stay honest instead of guessing.
            else -> ScreenState.Unknown
        }
    }

    override fun nextItemGesture(bounds: ScreenBounds, direction: ScrollDirection): GestureSpec {
        val x = bounds.left + bounds.width * YouTubeMatchers.SWIPE_X_FRACTION
        val from = bounds.top + bounds.height * YouTubeMatchers.SWIPE_FROM_FRACTION
        val to = bounds.top + bounds.height * YouTubeMatchers.SWIPE_TO_FRACTION
        // UP = swipe finger upwards (next Short). DOWN is the reverse swipe.
        val (startY, endY) = if (direction == ScrollDirection.UP) from to to else to to from
        return GestureSpec(x, startY, x, endY, YouTubeMatchers.SWIPE_DURATION_MS)
    }

    /**
     * Whole-string match for every category. Only the AD_BADGE category also matches on the last line, because on a
     * real Shorts ad the description is "<advertiser and title>\nAnzeige". SKIP_AD never gets the relaxed match:
     * descriptions carry text that creators and advertisers control, and a skip match can lead to an automatic click.
     * Nothing of the description is kept.
     */
    override fun categorizeDescription(description: String): String? {
        val whole = description.trim().lowercase(Locale.ROOT)
        val lastLine = whole.substringAfterLast('\n').trim()
        return YouTubeMatchers.labels.entries.firstOrNull { (category, labels) ->
            whole in labels || (category == YouTubeMatchers.Categories.AD_BADGE && lastLine in labels)
        }?.key
    }
}

class YouTubeAdMonitor(private val packageNames: Set<String> = setOf(YouTubeMatchers.PACKAGE)) : AdvertisementMonitor {
    override fun evaluate(snapshot: NodeSnapshot): AdState {
        if (snapshot.packageName !in packageNames) return AdState.Unknown
        val nodes = snapshot.walk().visible().toList()
        val signals = mutableListOf<String>()

        val idHits = nodes.mapNotNull { it.shortId() }.filter { it in YouTubeMatchers.Ids.adMarkers }.distinct()
        idHits.forEach { signals += "id:$it" }
        val badge = nodes.any { it.descriptionCategory == YouTubeMatchers.Categories.AD_BADGE }
        if (badge) signals += "desc:${YouTubeMatchers.Categories.AD_BADGE}"
        val skip = nodes.any { it.descriptionCategory == YouTubeMatchers.Categories.SKIP_AD }
        if (skip) signals += "desc:${YouTubeMatchers.Categories.SKIP_AD}"

        return when {
            skip -> AdState.SkipLikelyAvailable(Evidence(signals, if (idHits.isNotEmpty()) Confidence.MEDIUM else Confidence.LOW))
            // Observed on real ads: the overlay id and the "<name>\nAnzeige" label appear together.
            idHits.isNotEmpty() && badge -> AdState.AdLikely(Evidence(signals, Confidence.HIGH))
            idHits.isNotEmpty() -> AdState.AdLikely(Evidence(signals, Confidence.MEDIUM))
            badge -> AdState.AdLikely(Evidence(signals, Confidence.LOW))
            // Only claim "no evidence" when we positively see the Shorts feed, otherwise we simply cannot tell.
            nodes.mapNotNull { it.shortId() }.any { it in YouTubeMatchers.Ids.feedMarkers } -> AdState.NoAdEvidence
            else -> AdState.Unknown
        }
    }
}
