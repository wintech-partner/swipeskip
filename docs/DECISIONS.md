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
