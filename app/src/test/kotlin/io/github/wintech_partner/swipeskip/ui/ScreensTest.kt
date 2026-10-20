package io.github.wintech_partner.swipeskip.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.wintech_partner.swipeskip.AutoshortsApp
import io.github.wintech_partner.swipeskip.core.model.AppSettings
import io.github.wintech_partner.swipeskip.core.model.AutomationMode
import io.github.wintech_partner.swipeskip.core.model.SessionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = AutoshortsApp::class)
class ScreensTest {
    @get:Rule val compose = createComposeRule()

    private fun vm() = MainViewModel(ApplicationProvider.getApplicationContext<Application>())
    private val consented = UiState(settings = AppSettings(consentGiven = true), serviceConnected = true)

    // ---- Onboarding ----

    @Test fun onboardingShowsTheDisclosureAndHidesSystemSettingsUntilConsent() {
        compose.setContent { OnboardingScreen(vm(), UiState(settings = AppSettings()), onDone = {}) }
        compose.onNodeWithText("What it can see").assertIsDisplayed()
        compose.onNodeWithText("What it never does").assertIsDisplayed()
        compose.onNodeWithText("I understand and agree").assertIsDisplayed()
        compose.onNodeWithText("Open accessibility settings").assertDoesNotExist()
    }

    @Test fun onboardingShowsSetupStepsAfterConsent() {
        compose.setContent { OnboardingScreen(vm(), UiState(settings = AppSettings(consentGiven = true)), onDone = {}) }
        compose.onNodeWithText("I understand and agree").assertDoesNotExist()
        compose.onNodeWithText("Open accessibility settings").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Open app info").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("The service is not on yet.").performScrollTo().assertIsDisplayed()
    }

    // ---- Home ----

    @Test fun homeOffersSetupInsteadOfStartWhenTheServiceIsOff() {
        compose.setContent { HomeScreen(vm(), consented.copy(serviceConnected = false), {}, {}, {}, {}) }
        compose.onNodeWithText("Continue setup").assertIsDisplayed()
        compose.onNodeWithText("Start").assertDoesNotExist()
    }

    @Test fun homeShowsStartWhenReady() {
        compose.setContent { HomeScreen(vm(), consented, {}, {}, {}, {}) }
        compose.onNodeWithText("Ready").assertIsDisplayed()
        compose.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test fun homeShowsStopAndPauseReasonWhileRunning() {
        compose.setContent { HomeScreen(vm(), consented.copy(session = SessionState.PausedByAd), {}, {}, {}, {}) }
        compose.onNodeWithText("Stop").assertIsDisplayed()
        compose.onNodeWithText("Paused: an ad is likely showing").assertIsDisplayed()
    }

    @Test fun homeNeverOverclaimsAdDetection() {
        compose.setContent { HomeScreen(vm(), consented.copy(session = SessionState.Running), {}, {}, {}, {}) }
        compose.onNodeWithText("Ad detection unavailable").assertIsDisplayed()
    }

    @Test fun homeHidesPremiumWhenNoStoreIsConfigured() {
        compose.setContent { HomeScreen(vm(), consented, {}, {}, {}, {}) }
        compose.onNodeWithText("Premium").assertDoesNotExist()
    }

    @Test fun homeShowsPremiumWhenStoreIsAvailable() {
        compose.setContent { HomeScreen(vm(), consented.copy(storeAvailable = true), {}, {}, {}, {}) }
        compose.onNodeWithText("Premium").assertIsDisplayed()
    }

    @Test fun manualModeShowsTheScrollButtonOnlyWhileRunning() {
        val manual = consented.copy(settings = AppSettings(consentGiven = true).let { it.copy(session = it.session.copy(mode = AutomationMode.MANUAL_TAP)) })
        compose.setContent { HomeScreen(vm(), manual.copy(session = SessionState.Running), {}, {}, {}, {}) }
        compose.onNodeWithText("Scroll to next Short").assertIsDisplayed()
    }

    // ---- Settings ----

    @Test fun settingsListsEveryPresetAndTheCustomRange() {
        compose.setContent { SettingsScreen(vm(), consented, {}, {}, {}) }
        listOf(3, 5, 8, 10, 15, 30).forEach { compose.onNodeWithText("$it seconds").assertIsDisplayed() }
        compose.onNodeWithText("Custom: 10 s").assertIsDisplayed()
    }

    @Test fun settingsOffersNoUnlimitedDuration() {
        compose.setContent { SettingsScreen(vm(), consented, {}, {}, {}) }
        listOf(15, 30, 60, 120, 240).forEach { compose.onNodeWithText("$it min").assertIsDisplayed() }
    }

    @Test fun settingsHidesTheModePickerWhenTheFlavorAllowsOneMode() {
        compose.setContent { SettingsScreen(vm(), consented.copy(allowedModes = setOf(AutomationMode.MANUAL_TAP)), {}, {}, {}) }
        compose.onNodeWithText("Mode").assertDoesNotExist()
    }

    @Test fun settingsShowsTheModePickerWhenBothModesAreAllowed() {
        compose.setContent { SettingsScreen(vm(), consented, {}, {}, {}) }
        compose.onNodeWithText("Mode").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Automatic timer").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Manual tap").performScrollTo().performClick()
    }

    @Test fun settingsShowsMotionAndAutoSkipOnlyWhereAllowed() {
        compose.setContent { SettingsScreen(vm(), consented.copy(adSkipAllowed = true), {}, {}, {}) }
        compose.onNodeWithText("Move the phone for next Short").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tap Skip automatically").performScrollTo().assertIsDisplayed()
    }

    @Test fun settingsHidesAutoSkipWhenNotAllowed() {
        compose.setContent { SettingsScreen(vm(), consented.copy(adSkipAllowed = false), {}, {}, {}) }
        compose.onNodeWithText("Move the phone for next Short").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tap Skip automatically").assertDoesNotExist()
    }
}
