package org.example.autoshorts.platform.youtube

import org.example.autoshorts.core.model.AdState
import org.example.autoshorts.core.model.Confidence
import org.example.autoshorts.core.model.Evidence
import org.example.autoshorts.core.model.PlatformId
import org.example.autoshorts.core.model.ScreenState
import org.example.autoshorts.core.model.ScrollDirection
import org.example.autoshorts.core.platformapi.AdvertisementMonitor
import org.example.autoshorts.core.platformapi.GestureSpec
import org.example.autoshorts.core.platformapi.NodeSnapshot
import org.example.autoshorts.core.platformapi.PlatformAdapter
import org.example.autoshorts.core.platformapi.ScreenBounds
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

    override fun categorizeDescription(description: String): String? {
        val normalized = description.trim().lowercase(Locale.ROOT)
        return YouTubeMatchers.labels.entries.firstOrNull { normalized in it.value }?.key
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
            idHits.isNotEmpty() && badge -> AdState.AdLikely(Evidence(signals, Confidence.MEDIUM))
            idHits.isNotEmpty() || badge -> AdState.AdLikely(Evidence(signals, Confidence.LOW))
            // Only claim "no evidence" when we positively see the Shorts feed, otherwise we simply cannot tell.
            nodes.mapNotNull { it.shortId() }.any { it in YouTubeMatchers.Ids.feedMarkers } -> AdState.NoAdEvidence
            else -> AdState.Unknown
        }
    }
}
