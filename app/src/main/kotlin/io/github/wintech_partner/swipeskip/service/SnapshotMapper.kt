package io.github.wintech_partner.swipeskip.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import io.github.wintech_partner.swipeskip.core.platformapi.NodeSnapshot
import io.github.wintech_partner.swipeskip.core.platformapi.PlatformAdapter
import io.github.wintech_partner.swipeskip.core.platformapi.ScreenBounds

/**
 * The only place that reads [AccessibilityNodeInfo]. It copies structure into an immutable [NodeSnapshot]
 * and drops everything else. Content descriptions are turned into a category by the adapter and the raw
 * string is never stored, logged or passed on.
 */
object SnapshotMapper {
    private const val MAX_DEPTH = 24
    private const val MAX_NODES = 500

    fun map(root: AccessibilityNodeInfo, adapter: PlatformAdapter): NodeSnapshot {
        val budget = intArrayOf(MAX_NODES)
        return mapNode(root, adapter, depth = 0, budget = budget)
    }

    private fun mapNode(node: AccessibilityNodeInfo, adapter: PlatformAdapter, depth: Int, budget: IntArray): NodeSnapshot {
        budget[0]--
        val rect = Rect().also { node.getBoundsInScreen(it) }
        val category = node.contentDescription?.toString()?.let(adapter::categorizeDescription)

        val children = buildList {
            if (depth < MAX_DEPTH) {
                for (i in 0 until node.childCount) {
                    if (budget[0] <= 0) break
                    val child = node.getChild(i) ?: continue
                    add(mapNode(child, adapter, depth + 1, budget))
                    @Suppress("DEPRECATION")
                    child.recycle() // no-op on newer Android, required before API 33
                }
            }
        }
        return NodeSnapshot(
            packageName = node.packageName?.toString(),
            className = node.className?.toString(),
            viewId = node.viewIdResourceName,
            bounds = ScreenBounds(rect.left, rect.top, rect.right, rect.bottom),
            visibleToUser = node.isVisibleToUser,
            descriptionCategory = category,
            children = children,
        )
    }
}
