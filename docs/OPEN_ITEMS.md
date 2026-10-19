# Open items (need a human, a device or a store account)

## Release blockers
- [ ] Final app name and `applicationId` (placeholder `org.example.autoshorts`; avoid "YouTube" and "Clicker"). Also update `url_issues`/`url_source` placeholders in `res/values/config.xml`, SECURITY/PRIVACY contacts.
- [ ] License: Apache-2.0 chosen as default; maybe GPL-3.0 is preferred.
- [ ] Launcher icon/logo is a placeholder vector.

## Needs a physical device with real YouTube
- [ ] Every matcher in `platform/youtube/.../YouTubeMatchers.kt` is an unverified candidate (feed, comments, other-screen and ad ids; localized labels).
- [ ] Does `TYPE_TOUCH_INTERACTION_START` arrive? Is the 1 s own-gesture grace right? Does the swipe geometry (75%→25% height, 250 ms) reliably advance a Short?
- [ ] Notification behavior on OEMs; restricted-settings flow on a real sideloaded install.
- [ ] Battery numbers (method in COMPETITOR_RESEARCH checklist). None published.
- [ ] Run `FakeShortsEndToEndTest` on an emulator.
- [ ] RTL and long-string visual check on a device (only automated checks exist).

## Decisions
- [ ] Play submission; whether `AUTO_TIMER` may ship there (`AUTO_TIMER_ENABLED` flag in `app/build.gradle.kts`); accessibility-tool declaration (default: no).
- [ ] Release signing keys, repo creation/push, F-Droid/IzzyOnDroid submissions, pricing, Play product id (`-PpremiumProductId`). The purchase flow UI is not implemented, only restore.
- [ ] Optional floating on-screen indicator/control is not implemented (the notification is the control).

## Translations
All non-English strings (tr, de, es, fr, pt-BR, ru, ar, id) are **machine-drafted** and need native review. The Turkish text was written to read naturally but is unreviewed. YouTube's own localized labels in the matchers are also from general knowledge, unverified.
