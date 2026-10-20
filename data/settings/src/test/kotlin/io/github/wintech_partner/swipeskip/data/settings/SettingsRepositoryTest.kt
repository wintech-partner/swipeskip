package io.github.wintech_partner.swipeskip.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import io.github.wintech_partner.swipeskip.core.model.AutomationMode
import io.github.wintech_partner.swipeskip.core.model.AutomationPolicy
import io.github.wintech_partner.swipeskip.core.model.ScrollDirection
import io.github.wintech_partner.swipeskip.core.model.SettingsLimits
import io.github.wintech_partner.swipeskip.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun TestScope.repo(policy: AutomationPolicy, file: File = File(tmp.root, "s.preferences_pb")) =
        SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope + UnconfinedTestDispatcher(testScheduler)) { file },
            policy,
        )

    private operator fun kotlinx.coroutines.CoroutineScope.plus(d: kotlin.coroutines.CoroutineContext) =
        kotlinx.coroutines.CoroutineScope(coroutineContext + d)

    @Test fun `defaults for the foss policy`() = runTest {
        val s = repo(SettingsRepository.BOTH_AUTO_DEFAULT).settings.first()
        assertEquals(AutomationMode.AUTO_TIMER, s.session.mode)
        assertEquals(SettingsLimits.DEFAULT_INTERVAL_SECONDS, s.session.intervalSeconds)
        assertEquals(SettingsLimits.DEFAULT_DURATION_MINUTES, s.session.maxDurationMinutes)
        assertNull(s.session.maxScrolls)
        assertFalse(s.consentGiven)
        assertTrue(s.haptics)
    }

    @Test fun `defaults for the play policy are manual tap`() = runTest {
        val s = repo(SettingsRepository.BOTH_MANUAL_DEFAULT).settings.first()
        assertEquals(AutomationMode.MANUAL_TAP, s.session.mode)
    }

    @Test fun `values round trip`() = runTest {
        val r = repo(SettingsRepository.BOTH_AUTO_DEFAULT)
        r.update {
            it.copy(
                session = it.session.copy(
                    intervalSeconds = 42, maxDurationMinutes = 60, maxScrolls = 200,
                    direction = ScrollDirection.DOWN, pauseOnTouch = false, resumeDelaySeconds = 0,
                ),
                theme = ThemeMode.DARK, adNotifications = false, haptics = false, consentGiven = true,
            )
        }
        val s = r.settings.first()
        assertEquals(42, s.session.intervalSeconds)
        assertEquals(60, s.session.maxDurationMinutes)
        assertEquals(200, s.session.maxScrolls)
        assertEquals(ScrollDirection.DOWN, s.session.direction)
        assertFalse(s.session.pauseOnTouch)
        assertEquals(0, s.session.resumeDelaySeconds)
        assertEquals(ThemeMode.DARK, s.theme)
        assertFalse(s.adNotifications)
        assertFalse(s.haptics)
        assertTrue(s.consentGiven)
    }

    @Test fun `settings persist across repository instances`() = runTest {
        val file = File(tmp.root, "persist.preferences_pb")
        fun open(job: kotlinx.coroutines.Job) = SettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher(testScheduler) + job),
            ) { file },
            SettingsRepository.BOTH_AUTO_DEFAULT,
        )
        val firstJob = kotlinx.coroutines.Job()
        open(firstJob).update { it.copy(session = it.session.copy(intervalSeconds = 77), consentGiven = true) }
        firstJob.cancel() // closes the first store, like a process restart

        val reread = open(kotlinx.coroutines.Job()).settings.first()
        assertEquals(77, reread.session.intervalSeconds)
        assertTrue(reread.consentGiven)
    }

    @Test fun `custom interval is clamped to 3 to 120`() = runTest {
        val r = repo(SettingsRepository.BOTH_AUTO_DEFAULT)
        r.update { it.copy(session = it.session.copy(intervalSeconds = 1)) }
        assertEquals(3, r.settings.first().session.intervalSeconds)
        r.update { it.copy(session = it.session.copy(intervalSeconds = 999)) }
        assertEquals(120, r.settings.first().session.intervalSeconds)
        r.update { it.copy(session = it.session.copy(intervalSeconds = 35)) }
        assertEquals(35, r.settings.first().session.intervalSeconds)
    }

    @Test fun `duration outside the offered options falls back to the default`() = runTest {
        val r = repo(SettingsRepository.BOTH_AUTO_DEFAULT)
        r.update { it.copy(session = it.session.copy(maxDurationMinutes = 100_000)) }
        assertEquals(SettingsLimits.DEFAULT_DURATION_MINUTES, r.settings.first().session.maxDurationMinutes)
    }

    @Test fun `a flavor that only allows manual tap rejects auto timer`() = runTest {
        val r = repo(SettingsRepository.MANUAL_ONLY)
        r.update { it.copy(session = it.session.copy(mode = AutomationMode.AUTO_TIMER)) }
        assertEquals(AutomationMode.MANUAL_TAP, r.settings.first().session.mode)
    }

    @Test fun `max scrolls can be cleared again`() = runTest {
        val r = repo(SettingsRepository.BOTH_AUTO_DEFAULT)
        r.update { it.copy(session = it.session.copy(maxScrolls = 5)) }
        r.update { it.copy(session = it.session.copy(maxScrolls = null)) }
        assertNull(r.settings.first().session.maxScrolls)
    }

    @Test fun `motion and ad skip defaults are off`() = runTest {
        val s = repo(SettingsRepository.BOTH_AUTO_DEFAULT).settings.first()
        assertFalse(s.motionNext)
        assertFalse(s.autoSkipAds)
    }

    @Test fun `motion and ad skip round trip where allowed`() = runTest {
        val r = repo(SettingsRepository.BOTH_AUTO_DEFAULT)
        r.update { it.copy(motionNext = true, autoSkipAds = true, motionSensitivity = io.github.wintech_partner.swipeskip.core.model.MotionSensitivity.HIGH) }
        val s = r.settings.first()
        assertTrue(s.motionNext)
        assertTrue(s.autoSkipAds)
        assertEquals(io.github.wintech_partner.swipeskip.core.model.MotionSensitivity.HIGH, s.motionSensitivity)
    }

    @Test fun `auto skip is forced off in a flavor that does not allow it`() = runTest {
        val r = repo(SettingsRepository.MANUAL_ONLY)
        r.update { it.copy(autoSkipAds = true) }
        assertFalse(r.settings.first().autoSkipAds)
    }
}
