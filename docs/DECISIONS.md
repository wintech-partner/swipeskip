# Decisions (ADRs)

## ADR 1: Module layout
Pure-Kotlin modules for everything that can be: `core/model`, `core/platform-api`, `core/automation`, `platform/youtube`, `billing/api`, `billing/noop`. Android modules only where needed: `app`, `data/settings`, `billing/play`. Reason: the engine, state machine and adapter run in plain JVM tests with a fake clock and recorded-style fixtures. `NodeSnapshot` is the only boundary type.

## ADR 2: Two automation modes, one flag
`AutomationPolicy` (model) lists allowed modes and the default; `AUTO_TIMER_ENABLED` (BuildConfig, per flavor) selects it. `foss`: both modes, default AUTO_TIMER. `play`: MANUAL_TAP only. `SettingsRepository` applies the policy on every read and write, so a stale stored mode cannot escape it. MANUAL_TAP never dispatches a gesture on a timer; the timer only reminds (haptic).

## ADR 3: Accessibility service scope
`android:packageNames` = YouTube only (debug overlay adds the fake test app). Event types: window state/content changed, view scrolled/clicked, touch interaction start. `canPerformGestures` for the swipe. `canRetrieveWindowContent` is needed because ad/screen detection reads node structure (view ids, class names, bounds, visibility); `flagReportViewIds` exposes ids. Content descriptions are turned into a category by the adapter and the raw string is dropped inside `SnapshotMapper`. `isAccessibilityTool` is not set: Play policy lists automation tools as NOT eligible. Unverified: whether `TYPE_TOUCH_INTERACTION_START` is delivered without touch-exploration mode; user-interaction pause therefore also listens to scroll/click events and filters out the app's own gesture (in flight + 1 s grace).

## ADR 4: Ongoing notification, no foreground service
The accessibility service is kept alive by the system while enabled, so a separate foreground service adds nothing. The ongoing notification (Stop, Pause/Resume, and "Next" in manual mode) is posted by `SessionNotifier`. Consequence: it needs POST_NOTIFICATIONS on Android 13+; without it the Stop button is only in the app, which onboarding and diagnostics call out. Not verified on a device whether every OEM keeps an ongoing non-foreground notification undismissable.

## ADR 5: Never swipe over another app
The service has no foreground-change events for other apps (package filter). Before every automatic gesture the active window's package must be YouTube, otherwise the gesture is `SKIPPED` (neither success nor failure), the engine learns "not in foreground", the watchdog is suspended and the 60 s foreground timeout runs.

## ADR 6: Unknown over guessing
`classify` returns `Unknown` unless it sees positive markers; `NoAdEvidence` only when the Shorts feed is positively recognised. The engine treats Unknown as "keep working" and the UI says "Ad detection unavailable". All matcher ids/labels are unverified candidates until tested on a device.

## ADR 7: Restricted settings onboarding
Android 13+ blocks accessibility access for apps not installed through a session-based installer (APK from a file). Secondary sources (not official docs; none found): https://bayton.org/android/android-13-restricted-permissions/ , https://androidauthority.com/android-15-restricted-settings-sideloading-3481098 . The "Allow restricted settings" menu item (App info, overflow menu) reportedly appears only after the user tried to enable the service once. Onboarding therefore offers both the accessibility settings and the app-info deep link with plain instructions. Checked 2026-10-19.

## ADR 8: compileSdk 37, targetSdk 36
Current Compose/Navigation artifacts require compileSdk 37. targetSdk stays 36 until behavior changes of 37 are reviewed (lint flags OldTargetApi).

## ADR 9: Skip-ad control and motion-to-next (added 2026-10-19 at the human's request)
The original brief prohibited any Skip tapping. The human then asked for a "skip ad" option and, at the follow-up question, chose to include automatic skipping. Decision:
- **Manual button** ("Skip ad" on Home and in the ad notification) appears only while a skip control is *likely* visible.
- **Automatic tap** is an opt-in setting, default off, allowed only when `AutomationPolicy.adSkipAllowed` is true (`foss`; the `play` flavor is forced off by the repository).
- Safety: only a live node whose label category equals the adapter's `skipControlCategory` (and is visible, in the YouTube window) is clicked, via `ACTION_CLICK` on it or a clickable ancestor (max 4 hops). Nothing else is ever clicked. `AdSkipGate` allows at most 3 taps per ad, 2 s apart. The onboarding disclosure says this explicitly in all languages.
- Risk, stated plainly: this reverses a prohibition written for Play-policy and platform-terms reasons. Automating YouTube's own UI may conflict with YouTube's terms, and Play will likely reject it, hence `foss` only. The tap is on YouTube's own control, never on the ad, and does not fake engagement, but a human should still confirm they accept this.
- The skip label ids/texts are unverified candidates; it may not work until tested on a device.
- **Motion-to-next:** `MotionDetector` (pure) over `TYPE_ACCELEROMETER` (no permission). The sensor is registered only while a session is live and the setting is on, unregistered otherwise. A flick performs one user-initiated scroll in either mode (also while an ad pause holds) and restarts the interval. Thresholds (8/11/14 m/s^2 after gravity filtering, 1.2 s cooldown) are first guesses.
