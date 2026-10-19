package org.example.autoshorts.core.platformapi

/** Screen rectangle in pixels. */
data class ScreenBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val isEmpty: Boolean get() = width <= 0 || height <= 0
}

/**
 * Immutable, text-free copy of one accessibility node.
 *
 * Privacy rule: no titles, comments, usernames or any other on-screen text lives here.
 * The mapper turns a content description into a [descriptionCategory] (via the adapter's
 * label sets) and drops the raw string immediately.
 */
data class NodeSnapshot(
    val packageName: String?,
    val className: String?,
    val viewId: String?,
    val bounds: ScreenBounds,
    val visibleToUser: Boolean,
    val descriptionCategory: String? = null,
    val children: List<NodeSnapshot> = emptyList(),
) {
    /** Depth-first traversal including this node. */
    fun walk(): Sequence<NodeSnapshot> = sequence {
        yield(this@NodeSnapshot)
        for (child in children) yieldAll(child.walk())
    }

    val nodeCount: Int get() = walk().count()
}
