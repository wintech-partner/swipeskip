# Open items (need a human, a device or a store account)

## Release blockers
- [x] App name SwipeSkip and `applicationId` io.github.wintech_partner.swipeskip chosen by the human 2026-10-20. The private GitHub repo wintech-partner/swipeskip exists (make it public before any release; SECURITY/PRIVACY point at its issues and security advisories). Still open: check the name on Play, F-Droid and GitHub.
- [x] License: Apache-2.0, confirmed by the human 2026-10-20.
- [~] Launcher icon: blue rounded square with a white double chevron (>>), chosen by the human 2026-10-20; it is still a simple vector, a designer's logo may replace it.

## Distribution
- [~] F-Droid: merge request https://gitlab.com/fdroid/fdroiddata/-/merge_requests/53461 (opened 2026-10-20, tag v0.1.1, recipe in docs/fdroid/). CI pipeline green after two fixes (trailing newline, `scanignore: billing/play`). Waiting for human review; the reviewers may ask to delete `billing/play` or question the Skip tap (ADR 9/10). Next release: tag v0.1.x, F-Droid auto-updates from tags.
  - 2026-10-21: reviewer Licaon_Kter asked why no NonFreeDep. Verified: the foss runtime classpath has no proprietary library (only billing:noop/api), but F-Droid counts a required proprietary app (YouTube) as NonFreeDep, so we added `AntiFeatures: NonFreeDep` (fork commit 5854e8336). Reply to NonFreeDep was posted on the MR the same day (14:38).
  - 2026-10-23: Licaon_Kter also asked about reproducible builds and APK signing. Replied on the MR (note 4076424514): F-Droid builds the tag and signs with its own key; reproducibility not verified yet, offered to build the tag twice and compare, then set `Binaries:` / `AllowedAPKSigningKeys`. Fastlane metadata (en-US + tr, icon, screenshots, changelogs 1-2) is in the v0.1.1 tag. Waiting for a reply (label `waiting-for-upstream`). Optional follow-up: do the two-build comparison locally.

## Needs a physical device with real YouTube
- [~] Matchers verified from real dumps on YouTube 21.41.525 (German, 2026-10-19): feed, comments, Home, Shorts ad. Still open: other versions/languages, regular watch page, skippable ads, and running the app itself on a phone. Original item: every matcher in `platform/youtube/.../YouTubeMatchers.kt` is an unverified candidate (feed, comments, other-screen and ad ids; localized labels).
- [~] Real phone (Galaxy A04s, Android 14, YouTube 21.41.525), 2026-10-20, human-observed: swipe advances a Short at 5 s, a touch pauses the session. Also observed: pauses on an ad, pauses while comments are open (same Short keeps looping), pauses on Home and resumes when back in Shorts. Notification Stop works (needs notification permission; Home now warns when it is off). 60 s away ends the session (after closing the PiP window). Screen-off ends the session. Still open: restricted-settings flow, skippable-ad tap.
- [ ] Does `TYPE_TOUCH_INTERACTION_START` arrive? Is the 1 s own-gesture grace right? Does the swipe geometry (75%→25% height, 250 ms) reliably advance a Short?
- [ ] Notification behavior on OEMs; restricted-settings flow on a real sideloaded install.
- [ ] Battery numbers (method in COMPETITOR_RESEARCH checklist). None published.
- [x] `FakeShortsEndToEndTest` run on an API 35 emulator: 3/3 pass (2026-10-19). Run it again on other API levels.
- [ ] RTL and long-string visual check on a device (only automated checks exist).

- [ ] Motion thresholds (8/11/14 m/s^2, 1.2 s cooldown) and whether walking causes false triggers.
- [~] Watch mode (ADR 10): the real regular-video Skip button was dumped (`skip_ad_button`, 2026-10-20); the human confirmed on the phone (2026-10-20, German UI) that Watch mode skips a regular-video ad (the first attempt failed only because the mode had not been selected). Open: `ACTION_CLICK` on it, other languages/versions, other ad types.
- [ ] Skip control: verify the real Skip button label/ids and that `ACTION_CLICK` works on it. `tapsSkipOnlyWhenTheUserOptedIn` passes against the fake app only.

## Decisions
- [x] The human accepted the ADR 9 and ADR 10 risk on 2026-10-20 (automatic Skip tapping may conflict with YouTube's terms; Play will likely reject it, so it exists in `foss` only).
- [ ] Play submission; whether `AUTO_TIMER` may ship there (`AUTO_TIMER_ENABLED` flag in `app/build.gradle.kts`); accessibility-tool declaration (default: no).
- [~] Release signing key created 2026-10-23 (keystore + password in `~/.config/swipeskip-release/`, back them up; cert SHA-256 `c5d9c032e8e97d32d0eeb2747f0e80ef77dde72d4d30b501250e29a755d0656f`). v0.1.1 GitHub release published, signed by hand from the tag. Still open: add repo secrets `RELEASE_KEYSTORE_B64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` (=`swipeskip`) so the release workflow signs future tags (needs admin on wintech-partner/swipeskip; our account has push only).
- [ ] IzzyOnDroid submission, pricing, Play product id (`-PpremiumProductId`). The purchase flow UI is not implemented, only restore.
- [ ] Optional floating on-screen indicator/control is not implemented (the notification is the control).

## Accessibility (see ACCESSIBILITY_STATEMENT.md)
- [ ] Test the app UI with TalkBack and Switch Access; check 200% font scale, contrast ratios and touch-target sizes.
- [ ] Ask people with motor/visual disabilities for feedback before making any accessibility claim in listings.
- [ ] Human decision: keep `isAccessibilityTool` unset (default) unless the audience truly changes.

## Translations
All non-English strings (tr, de, es, fr, pt-BR, ru, ar, id, hi, ja, vi, ko, th, fil, bn, ur; the last eight were added 2026-10-20, Urdu is a second right-to-left language with no RTL check yet) are **machine-drafted** and need native review. The Turkish text was written to read naturally but is unreviewed. YouTube's own localized labels in the matchers are also from general knowledge, unverified.

## Firefox load check (2026-10-23)
- [x] Firefox 157.0.1 (snap): `dist/stage-firefox` (copied to `~/snap/firefox/common/swipeskip-firefox/`) installs as a temporary add-on via geckodriver (`extension/test/firefox-load-check.py`), id swipeskip@wintech-partner.github.io. Cause of the earlier failure was snap confinement: pick folders under `~/snap/firefox/common/`, not hidden or `/tmp` paths. Engine unit tests 8/8 pass. Still untested: the ad fast-forward on a real YouTube ad.
