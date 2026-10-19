package org.example.autoshorts

import android.app.UiAutomation
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import org.example.autoshorts.core.model.AutomationMode
import org.example.autoshorts.core.model.SessionState
import org.example.autoshorts.core.model.StopReason
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
    private lateinit var device: UiDevice
    private val fake = "org.example.autoshorts.fakeshorts"

    @Before fun setUp() {
        // Without this flag UiAutomation suppresses every other accessibility service, including ours.
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        device = UiDevice.getInstance(instrumentation)
        val service = "${BuildConfig.APPLICATION_ID}/org.example.autoshorts.service.AutoScrollService"
        automation.executeShellCommand("settings put secure enabled_accessibility_services $service").close()
        automation.executeShellCommand("settings put secure accessibility_enabled 1").close()
        runBlocking {
            container.settings.update {
                it.copy(
                    consentGiven = true,
                    session = it.session.copy(intervalSeconds = 3, mode = AutomationMode.AUTO_TIMER, maxScrolls = 2, resumeDelaySeconds = 3),
                )
            }
        }
        waitUntil(10_000) { container.host.serviceConnected.value }
        val launch = instrumentation.context.packageManager.getLaunchIntentForPackage(fake)!!
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        instrumentation.context.startActivity(launch)
        device.wait(Until.hasObject(By.pkg(fake)), 5_000)
    }

    @After fun tearDown() {
        instrumentation.runOnMainSync { container.host.stop() }
    }

    @Test fun scrollsTheFakePagerAndStopsAtTheScrollLimit() {
        instrumentation.runOnMainSync { container.host.start() }
        waitUntil(20_000) { container.host.engine.stats.value.lastStopReason == StopReason.SCROLL_LIMIT_REACHED }
        assertEquals(SessionState.Idle, container.host.engine.model.value.state)
        val page = device.findObject(By.res(fake, "page_counter")).text
        assertEquals("Page 3", page) // started on page 1, two scrolls
    }

    @Test fun pausesOnAnAdAndNeverTapsSkip() {
        instrumentation.runOnMainSync { container.host.start() }
        device.findObject(By.res(fake, "toggle_ad")).click()
        waitUntil(10_000) { container.host.engine.model.value.state == SessionState.PausedByAd }

        Thread.sleep(8_000) // more than two intervals
        assertEquals("Page 1", device.findObject(By.res(fake, "page_counter")).text)
        assertEquals("Skip clicks: 0", device.findObject(By.res(fake, "skip_clicks")).text)
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
