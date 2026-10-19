# YouTube view ids actually observed

Source: `adb shell uiautomator dump` on an Android 15 (API 35) emulator, YouTube **19.17.42** (system image build, logged out), 2026-10-19. Fixture: `platform/youtube/src/test/resources/fixtures/youtube_19.17.42_shorts_tab_error.xml` (text removed).

## What was reachable
Only the shell and the Shorts tab in an **error state** ("There was a problem with the server [400]"). This YouTube build is too old for the Shorts backend and no account was available, so **no real Shorts feed, comments panel or ad was ever seen**. I did not download third-party APKs.

## Ids seen (package `com.google.android.youtube:id/`)
Shell: `watch_while_layout_coordinator_layout`, `pane_fragment_container`, `pivot_bar` (bottom nav, present on the Shorts tab too), `bottom_bar_container`, `toolbar`, `toolbar_container`, `menu_search`, `youtube_logo`, `appbar_layout`, `action_bar_root`, `more_drawer_container`, `next_gen_watch_container_layout`, `inline_player_container`, `accessibility_layer_container`, `fab_container`, `slim_status_bar_player_container`.
Error/loading: `error_full_view`, `error_message_text`, `error_icon`, `error_retry_button`, `loading_layout`.
Content descriptions: `Home`, `Shorts`, `Subscriptions`, `Library`, `Search`, `More options`, `YouTube`.

## Consequences for `YouTubeMatchers.kt`
- **Not observed, still unverified candidates:** every feed marker (`reel_recycler`, `reel_player_page_container`, `reel_watch_fragment_root`), comments markers, ad markers, and the "other screen" markers (`watch_player`, `results`, `browse_results`).
- `pivot_bar` must **not** be used as a non-feed marker: it is present on the Shorts tab.
- The content description `Shorts` (nav tab) and the others above do not collide with the `AD_BADGE`/`SKIP_AD` label sets (checked by `RealDumpFixtureTest`). Note the label `ad` is an exact match, so "Add to playlist" does not match.
- Result with the adapter today: this tree classifies as `Unknown` (not the feed) and the ad monitor says `Unknown`, which is the honest outcome.

## How to finish verifying (needs a current YouTube with Shorts working)
Real phone with USB debugging, or an emulator image with Play Store and a signed-in account:
1. Open Shorts, `adb shell uiautomator dump`, save the ids; repeat with comments open, on a Shorts ad (wait for one), and on a normal watch page.
2. Replace the candidates in `YouTubeMatchers.kt`, add the dumps as fixtures, record the version in `TESTED_VERSIONS.md`.
