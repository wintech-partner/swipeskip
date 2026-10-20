package io.github.wintech_partner.swipeskip.platform.youtube

import io.github.wintech_partner.swipeskip.core.model.AdState
import io.github.wintech_partner.swipeskip.core.model.Confidence
import io.github.wintech_partner.swipeskip.core.model.ScreenState
import io.github.wintech_partner.swipeskip.core.model.ScrollDirection
import io.github.wintech_partner.swipeskip.core.platformapi.NodeSnapshot
import io.github.wintech_partner.swipeskip.core.platformapi.ScreenBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeShortsAdapterTest {
    private val adapter = YouTubeShortsAdapter()
    private val screen = ScreenBounds(0, 0, 1000, 2000)

    private fun node(id: String? = null, category: String? = null, visible: Boolean = true, vararg children: NodeSnapshot) =
        NodeSnapshot(
            packageName = YouTubeMatchers.PACKAGE,
            className = "android.view.View",
            viewId = id?.let { "${YouTubeMatchers.PACKAGE}:id/$it" },
            bounds = screen,
            visibleToUser = visible,
            descriptionCategory = category,
            children = children.toList(),
        )

    private fun root(vararg children: NodeSnapshot) = node(null, null, true, *children)

    // ---- classify ----

    @Test fun `feed marker means InTargetFeed`() =
        assertEquals(ScreenState.InTargetFeed, adapter.classify(root(node("reel_recycler"))))

    @Test fun `comments panel wins over feed`() =
        assertEquals(
            ScreenState.CommentsOpen,
            adapter.classify(root(node("reel_recycler"), node("app_engagement_panel"))),
        )

    @Test fun `invisible markers are ignored`() =
        assertEquals(ScreenState.Unknown, adapter.classify(root(node("reel_recycler", visible = false))))

    @Test fun `positive non feed marker on a real tree means OtherScreen`() {
        val tree = root(node("watch_player"), node(), node(), node(), node())
        assertEquals(ScreenState.OtherScreen, adapter.classify(tree))
    }

    @Test fun `tiny tree with a non feed marker stays Unknown`() =
        assertEquals(ScreenState.Unknown, adapter.classify(root(node("watch_player"))))

    @Test fun `no markers means Unknown not OtherScreen`() =
        assertEquals(ScreenState.Unknown, adapter.classify(root(node("something_else"))))

    @Test fun `other packages are Unknown`() {
        val foreign = root().copy(packageName = "com.example.other")
        assertEquals(ScreenState.Unknown, adapter.classify(foreign))
    }

    @Test fun `short id handles ids without a package prefix`() =
        assertEquals(ScreenState.InTargetFeed, adapter.classify(root(node().copy(viewId = "reel_recycler"))))

    // ---- gesture ----

    @Test fun `UP swipes from lower to upper part of the window`() {
        val g = adapter.nextItemGesture(screen, ScrollDirection.UP)
        assertEquals(500f, g.startX, 0.01f)
        assertEquals(1500f, g.startY, 0.01f)
        assertEquals(500f, g.endY, 0.01f)
        assertTrue(g.durationMs in 200..300)
    }

    @Test fun `DOWN is the reverse swipe`() {
        val up = adapter.nextItemGesture(screen, ScrollDirection.UP)
        val down = adapter.nextItemGesture(screen, ScrollDirection.DOWN)
        assertEquals(up.startY, down.endY, 0.01f)
        assertEquals(up.endY, down.startY, 0.01f)
    }

    @Test fun `gesture respects window offset`() {
        val g = adapter.nextItemGesture(ScreenBounds(100, 200, 1100, 2200), ScrollDirection.UP)
        assertEquals(600f, g.startX, 0.01f)
        assertEquals(1700f, g.startY, 0.01f)
    }

    @Test fun `gesture is deterministic`() =
        assertEquals(adapter.nextItemGesture(screen, ScrollDirection.UP), adapter.nextItemGesture(screen, ScrollDirection.UP))

    // ---- description categories (one fixture per supported language) ----

    private val skipLabels = mapOf(
        "en" to "Skip ad", "tr" to "Reklamı atla", "de" to "Anzeige überspringen", "es" to "Omitir anuncio",
        "fr" to "Passer l'annonce", "pt-BR" to "Pular anúncio", "ru" to "Пропустить рекламу",
        "ar" to "تخطي الإعلان", "id" to "Lewati iklan",
    )
    private val badgeLabels = mapOf(
        "en" to "Ad", "tr" to "Reklam", "de" to "Anzeige", "es" to "Anuncio", "fr" to "Annonce",
        "pt-BR" to "Anúncio", "ru" to "Реклама", "ar" to "إعلان", "id" to "Iklan",
    )

    @Test fun `skip labels are categorised in every supported language`() =
        skipLabels.forEach { (lang, text) ->
            assertEquals(lang, YouTubeMatchers.Categories.SKIP_AD, adapter.categorizeDescription(text))
        }

    @Test fun `ad badge labels are categorised in every supported language`() =
        badgeLabels.forEach { (lang, text) ->
            assertEquals(lang, YouTubeMatchers.Categories.AD_BADGE, adapter.categorizeDescription(text))
        }

    @Test fun `categorisation ignores case and whitespace`() =
        assertEquals(YouTubeMatchers.Categories.SKIP_AD, adapter.categorizeDescription("  SKIP AD "))

    @Test fun `ordinary descriptions are not categorised`() {
        assertNull(adapter.categorizeDescription("Like this video"))
        assertNull(adapter.categorizeDescription("Add to playlist")) // contains "ad" but is not "ad"
    }

    @Test fun `every language has both categories`() {
        YouTubeMatchers.labels.values.forEach { assertTrue(it.size >= 9) }
    }

    @Test fun `skip control category matches the skip label category`() {
        assertEquals(adapter.skipControlCategory, adapter.categorizeDescription("Skip ad"))
        assertTrue(adapter.skipControlCategory != adapter.categorizeDescription("Ad"))
    }

    // ---- ad monitor ----

    @Test fun `feed without ad signals is NoAdEvidence`() =
        assertEquals(AdState.NoAdEvidence, adapter.adMonitor.evaluate(root(node("reel_recycler"))))

    @Test fun `no feed and no signals is Unknown not NoAdEvidence`() =
        assertEquals(AdState.Unknown, adapter.adMonitor.evaluate(root(node("other"))))

    @Test fun `ad id plus badge is AdLikely with medium confidence`() {
        val state = adapter.adMonitor.evaluate(root(node("reel_recycler"), node("reel_player_delegated_overlay", YouTubeMatchers.Categories.AD_BADGE)))
        assertTrue(state is AdState.AdLikely)
        assertEquals(Confidence.HIGH, (state as AdState.AdLikely).evidence.confidence)
        assertTrue("id:reel_player_delegated_overlay" in state.evidence.signals)
    }

    @Test fun `ad label alone is low confidence and overlay id alone is medium`() {
        val labelOnly = adapter.adMonitor.evaluate(root(node("reel_recycler"), node(null, YouTubeMatchers.Categories.AD_BADGE)))
        assertEquals(Confidence.LOW, (labelOnly as AdState.AdLikely).evidence.confidence)
        val idOnly = adapter.adMonitor.evaluate(root(node("reel_recycler"), node("reel_player_delegated_overlay")))
        assertEquals(Confidence.MEDIUM, (idOnly as AdState.AdLikely).evidence.confidence)
    }

    @Test fun `skip label is never matched on the last line of a longer description`() {
        assertNull(adapter.categorizeDescription("Creator title\nSkip ad"))
        assertNull(adapter.categorizeDescription("Great video - skip ad"))
        assertEquals(YouTubeMatchers.Categories.SKIP_AD, adapter.categorizeDescription("Skip ad"))
    }

    @Test fun `ad containers are the ad marker ids`() =
        assertEquals(YouTubeMatchers.Ids.skipContainers, adapter.adContainerIds)

    @Test fun `ad label is found on the last line of a multi line description`() {
        assertEquals(YouTubeMatchers.Categories.AD_BADGE, adapter.categorizeDescription("Some Brand - great game\nAnzeige"))
        assertNull(adapter.categorizeDescription("Anzeige for something else entirely"))
        assertNull(adapter.categorizeDescription("Some title\nSome other last line"))
    }

    @Test fun `single weak signal is low confidence`() {
        val state = adapter.adMonitor.evaluate(root(node("reel_recycler"), node(null, YouTubeMatchers.Categories.AD_BADGE)))
        assertEquals(Confidence.LOW, (state as AdState.AdLikely).evidence.confidence)
    }

    @Test fun `skip category is SkipLikelyAvailable`() {
        val state = adapter.adMonitor.evaluate(root(node("reel_recycler"), node(null, YouTubeMatchers.Categories.SKIP_AD)))
        assertTrue(state is AdState.SkipLikelyAvailable)
        assertTrue(state.isAdPresent)
    }

    @Test fun `invisible ad nodes do not count`() =
        assertEquals(
            AdState.NoAdEvidence,
            adapter.adMonitor.evaluate(root(node("reel_recycler"), node("reel_player_delegated_overlay", visible = false))),
        )

    @Test fun `evidence never contains free text`() {
        val state = adapter.adMonitor.evaluate(root(node("reel_player_delegated_overlay", YouTubeMatchers.Categories.AD_BADGE))) as AdState.AdLikely
        state.evidence.signals.forEach { assertTrue(it.startsWith("id:") || it.startsWith("desc:")) }
    }
}
