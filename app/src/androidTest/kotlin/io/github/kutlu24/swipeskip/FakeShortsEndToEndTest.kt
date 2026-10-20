package io.github.kutlu24.swipeskip

import android.app.UiAutomation
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.runBlocking
import io.github.kutlu24.swipeskip.core.model.AutomationMode
import io.github.kutlu24.swipeskip.core.model.SessionState
import io.github.kutlu24.swipeskip.core.model.StopReason
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End to end on an emulator or device, against the fake Shorts app only. Never touches YouTube or a real ad.
 *
 * Run: ./gradlew :testing:fake-shorts-app:installDebug :app:connectedFossDebugAndroidTest
 * Needs both APKs installed. Not run in the authoring environment (no emulator): see docs/OPEN_ITEMS.md.
 */
@RunWith(AndroidJUnit4::class)
class FakeShortsEndToEndTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val container get() = ApplicationProvider.getApplicationContext<AutoshortsApp>().container
    private lateinit var automation: UiAutomation
    private val fake = "io.github.kutlu24.swipeskip.fakeshorts"

    @Before fun setUp() {
        // Without this flag UiAutomation suppresses every other accessibility service, including ours.
        // Do not use UiDevice here: it re-registers UiAutomation with flags=0 and suppresses our service again.
        automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val service = "${BuildConfig.APPLICATION_ID}/io.github.kutlu24.swipeskip.service.AutoScrollService"
        // Toggle off and on: after the instrumentation restart the system may have marked the service as crashed.
        shell(automation, "settings put secure enabled_accessibility_services ''")
        shell(automation, "settings put secure enabled_accessibility_services $service")
        shell(automation, "settings put secure accessibility_enabled 1")
        runBlocking {
            container.settings.update {
                it.copy(
                    consentGiven = true,
                    session = it.session.copy(intervalSeconds = 3, mode = AutomationMode.AUTO_TIMER, maxScrolls = 2, resumeDelaySeconds = 3),
                )
            }
        }
        waitUntil(20_000) { container.host.serviceConnected.value }
        val launch = instrumentation.context.packageManager.getLaunchIntentForPackage(fake)!!
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        instrumentation.context.startActivity(launch)
        waitUntil(10_000) { automation.rootInActiveWindow?.packageName == fake }
    }

    @After fun tearDown() {
        instrumentation.runOnMainSync { container.host.stop() }
    }

    @Test fun scrollsTheFakePagerAndStopsAtTheScrollLimit() {
        instrumentation.runOnMainSync { container.host.start() }
        waitUntil(20_000) { container.host.engine.stats.value.lastStopReason == StopReason.SCROLL_LIMIT_REACHED }
        assertEquals(SessionState.Idle, container.host.engine.model.value.state)
        val page = text("page_counter")
        assertEquals("Page 3", page) // started on page 1, two scrolls
    }

    @Test fun pausesOnAnAdAndDoesNotTapSkipByDefault() {
        instrumentation.runOnMainSync { container.host.start() }
        click("toggle_ad")
        waitUntil(10_000) { container.host.engine.model.value.state == SessionState.PausedByAd }

        Thread.sleep(8_000) // more than two intervals
        assertEquals("Page 1", text("page_counter"))
        assertEquals("Skip clicks: 0", text("skip_clicks"))
    }

    @Test fun tapsSkipOnlyWhenTheUserOptedIn() {
        runBlocking { container.settings.update { it.copy(autoSkipAds = true) } }
        instrumentation.runOnMainSync { container.host.start() }
        click("toggle_ad")
        waitUntil(10_000) { text("skip_clicks") != "Skip clicks: 0" }
        runBlocking { container.settings.update { it.copy(autoSkipAds = false) } }
    }

    /** Reads the output to the end: closing the stream early can cancel the command before it ran. */
    private fun shell(automation: UiAutomation, command: String) {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
    }

    private fun node(id: String): AccessibilityNodeInfo? =
        automation.rootInActiveWindow?.findAccessibilityNodeInfosByViewId("$fake:id/$id")?.firstOrNull()

    private fun text(id: String): String? = node(id)?.text?.toString()

    private fun click(id: String) {
        waitUntil(5_000) { node(id) != null } // the first frame may not be laid out yet
        assertTrue("view $id not found", node(id)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(250)
        }
        assertTrue("condition not met within ${timeoutMs}ms", condition())
    }
}
