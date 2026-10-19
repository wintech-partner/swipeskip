package org.example.autoshorts.platform.youtube

import org.example.autoshorts.core.model.AdState
import org.example.autoshorts.core.model.ScreenState
import org.example.autoshorts.core.platformapi.NodeSnapshot
import org.example.autoshorts.core.platformapi.ScreenBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/** Runs the adapter on a REAL view-hierarchy dump (structure only), not on hand-built nodes. */
class RealDumpFixtureTest {
    private val adapter = YouTubeShortsAdapter()

    private fun load(name: String): NodeSnapshot {
        val stream = javaClass.getResourceAsStream("/fixtures/$name")!!
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
        // A uiautomator dump wraps the window in <hierarchy>; the accessibility service's root is the window node itself.
        val windowRoot = (0 until doc.documentElement.childNodes.length).map { doc.documentElement.childNodes.item(it) }
            .filterIsInstance<Element>().first()
        return map(windowRoot)
    }

    private fun map(e: Element): NodeSnapshot {
        val b = Regex("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]").find(e.getAttribute("bounds"))?.groupValues?.drop(1)?.map { it.toInt() } ?: listOf(0, 0, 0, 0) // the <hierarchy> root has no bounds
        val kids = (0 until e.childNodes.length).map { e.childNodes.item(it) }.filterIsInstance<Element>().map(::map)
        return NodeSnapshot(
            packageName = e.getAttribute("package").ifEmpty { null },
            className = e.getAttribute("class").ifEmpty { null },
            viewId = e.getAttribute("resource-id").ifEmpty { null },
            bounds = ScreenBounds(b[0], b[1], b[2], b[3]),
            visibleToUser = true,
            descriptionCategory = e.getAttribute("content-desc").takeIf { it.isNotEmpty() }?.let(adapter::categorizeDescription),
            children = kids,
        )
    }

    private val tree get() = load("youtube_19.17.42_shorts_tab_error.xml")

    @Test fun `fixture is a real youtube tree`() {
        assertEquals("com.google.android.youtube", tree.packageName)
        assertTrue(tree.nodeCount > 20)
    }

    @Test fun `the Shorts tab error screen is not mistaken for the feed`() =
        assertEquals(ScreenState.Unknown, adapter.classify(tree))

    @Test fun `no ad evidence is claimed without the feed`() =
        assertEquals(AdState.Unknown, adapter.adMonitor.evaluate(tree))

    @Test fun `navigation labels such as Shorts and Home are not ad categories`() {
        val categories = tree.walk().mapNotNull { it.descriptionCategory }.toList()
        assertEquals(emptyList<String>(), categories)
    }

    @Test fun `observed structural ids are present`() {
        val ids = tree.walk().mapNotNull { it.viewId?.substringAfter(":id/") }.toSet()
        listOf("pivot_bar", "watch_while_layout_coordinator_layout", "pane_fragment_container").forEach { assertNotNull(it, ids.find { id -> id == it }) }
    }

    // ---- real phone dumps: YouTube 21.41.525, Android 14, German UI ----

    private val feed get() = load("youtube_21.41.525_de_shorts_feed.xml")
    private val comments get() = load("youtube_21.41.525_de_shorts_comments_open.xml")
    private val home get() = load("youtube_21.41.525_de_home.xml")
    private val ad1 get() = load("youtube_21.41.525_de_shorts_ad_1.xml")
    private val ad2 get() = load("youtube_21.41.525_de_shorts_ad_2.xml")

    @Test fun `real Shorts feed is recognised and shows no ad evidence`() {
        assertEquals(ScreenState.InTargetFeed, adapter.classify(feed))
        assertEquals(AdState.NoAdEvidence, adapter.adMonitor.evaluate(feed))
    }

    @Test fun `real open comments panel is recognised even though the feed is underneath`() =
        assertEquals(ScreenState.CommentsOpen, adapter.classify(comments))

    @Test fun `real Home screen is another screen, not the feed`() =
        assertEquals(ScreenState.OtherScreen, adapter.classify(home))

    @Test fun `real Shorts ads are recognised with high confidence`() {
        listOf(ad1, ad2).forEach { ad ->
            assertEquals(ScreenState.InTargetFeed, adapter.classify(ad))
            val state = adapter.adMonitor.evaluate(ad)
            assertTrue(state is AdState.AdLikely)
            assertEquals(org.example.autoshorts.core.model.Confidence.HIGH, (state as AdState.AdLikely).evidence.confidence)
        }
    }

    @Test fun `real Shorts ads showed no skip control`() =
        listOf(ad1, ad2).forEach { assertTrue(adapter.adMonitor.evaluate(it) !is AdState.SkipLikelyAvailable) }

    @Test fun `real Home and comments have no ad evidence`() {
        assertEquals(AdState.Unknown, adapter.adMonitor.evaluate(home))
        assertTrue(!adapter.adMonitor.evaluate(comments).isAdPresent)
    }
}
