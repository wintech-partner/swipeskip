# Open items (need a human, a device or a store account)

## Release blockers
- [ ] Final app name and `applicationId` (placeholder `org.example.autoshorts`; avoid "YouTube" and "Clicker"). Also update `url_issues`/`url_source` placeholders in `res/values/config.xml`, SECURITY/PRIVACY contacts.
- [ ] License: Apache-2.0 chosen as default; maybe GPL-3.0 is preferred.
- [ ] Launcher icon/logo is a placeholder vector.

## Needs a physical device with real YouTube
- [~] Matchers verified from real dumps on YouTube 21.41.525 (German, 2026-10-19): feed, comments, Home, Shorts ad. Still open: other versions/languages, regular watch page, skippable ads, and running the app itself on a phone. Original item: every matcher in `platform/youtube/.../YouTubeMatchers.kt` is an unverified candidate (feed, comments, other-screen and ad ids; localized labels).
- [~] Real phone (Galaxy A04s, Android 14, YouTube 21.41.525), 2026-10-20, human-observed: swipe advances a Short at 5 s, a touch pauses the session. Also observed: pauses on an ad, pauses while comments are open (same Short keeps looping), pauses on Home and resumes when back in Shorts. Notification Stop works (needs notification permission; Home now warns when it is off). Still open: screen-off end, 60 s away end, restricted-settings flow, skippable-ad tap.
- [ ] Does `TYPE_TOUCH_INTERACTION_START` arrive? Is the 1 s own-gesture grace right? Does the swipe geometry (75%→25% height, 250 ms) reliably advance a Short?
- [ ] Notification behavior on OEMs; restricted-settings flow on a real sideloaded install.
- [ ] Battery numbers (method in COMPETITOR_RESEARCH checklist). None published.
- [x] `FakeShortsEndToEndTest` run on an API 35 emulator: 3/3 pass (2026-10-19). Run it again on other API levels.
- [ ] RTL and long-string visual check on a device (only automated checks exist).

- [ ] Motion thresholds (8/11/14 m/s^2, 1.2 s cooldown) and whether walking causes false triggers.
- [ ] Skip control: verify the real Skip button label/ids and that `ACTION_CLICK` works on it. `tapsSkipOnlyWhenTheUserOptedIn` passes against the fake app only.

## Decisions
- [ ] Confirm you accept the ADR 9 risk (automatic Skip tapping may conflict with YouTube's terms; Play will likely reject it, so it exists in `foss` only).
- [ ] Play submission; whether `AUTO_TIMER` may ship there (`AUTO_TIMER_ENABLED` flag in `app/build.gradle.kts`); accessibility-tool declaration (default: no).
- [ ] Release signing keys, repo creation/push, F-Droid/IzzyOnDroid submissions, pricing, Play product id (`-PpremiumProductId`). The purchase flow UI is not implemented, only restore.
- [ ] Optional floating on-screen indicator/control is not implemented (the notification is the control).

## Accessibility (see ACCESSIBILITY_STATEMENT.md)
- [ ] Test the app UI with TalkBack and Switch Access; check 200% font scale, contrast ratios and touch-target sizes.
- [ ] Ask people with motor/visual disabilities for feedback before making any accessibility claim in listings.
- [ ] Human decision: keep `isAccessibilityTool` unset (default) unless the audience truly changes.

## Translations
All non-English strings (tr, de, es, fr, pt-BR, ru, ar, id) are **machine-drafted** and need native review. The Turkish text was written to read naturally but is unreviewed. YouTube's own localized labels in the matchers are also from general knowledge, unverified.
