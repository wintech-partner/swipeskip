# YouTube view ids actually observed

Two sources, both `adb shell uiautomator dump`:

1. **Real phone, 2026-10-19:** Samsung Galaxy A04s, Android 14, UI language **German**, YouTube **21.41.525**, logged in. Fixtures `platform/youtube/src/test/resources/fixtures/youtube_21.41.525_de_*.xml` (text removed, advertiser names and counts masked; see the README there).
2. Emulator system image, YouTube 19.17.42, logged out: only the shell and an error screen (`youtube_19.17.42_shorts_tab_error.xml`).

## Verified on the real phone (package `com.google.android.youtube:id/`)
| Situation | Evidence |
|---|---|
| **Shorts feed** | `reel_recycler`, `reel_player_page_container`, `reel_watch_fragment_root` (also `reel_watch_player`, `reel_video_interactions`, `reel_time_bar`). None of the `reel_*` ids exist on Home. |
| **Comments open** | `app_engagement_panel`, `engagement_panel_content`, `panel_header` appear; `reel_recycler` stays underneath, so comments must be checked before the feed. `results` also appears here. |
| **Home (not Shorts)** | `watch_player`, `results`, `filter_bar`, `player_view`; no `reel_*`. |
| **Shorts ad** (2 of 2 ads seen) | `reel_player_delegated_overlay` and `reel_progress_bar` appear; absent on ~28 normal Shorts pages. A clickable ViewGroup carries the content-description `"<advertiser and title>\nAnzeige"`, i.e. the ad label is the **last line**. |
| Shell, present everywhere | `pivot_bar` (also on Shorts: never a non-feed marker), `watch_while_layout_coordinator_layout`, `pane_fragment_container`, `bottom_bar_container`. |
| Other Shorts-feed pages | `non_video_page_element_container`, `posts_content_root`, `element_container`, `reel_horizontal_recycler_view` appeared on a few pages in the feed; `reel_recycler` was still present. |

Navigation labels seen (German): Startseite, Shorts, Erstellen, Abos, Mein YouTube, Suchen, Mehr Aktionen, Speichern, Dieses Video teilen, Remixen, "N Kommentare anzeigen".

## Regular video (full screen), real phone 2026-10-20
| Situation | Evidence |
|---|---|
| Regular watch page | `watch_player`, `player_overlays`, `player_view`; no `reel_*`. |
| Ad (skippable, with the Skip button shown) | `ad_progress_text` ("Gesponsert Mein Anzeigen-Center"), `ad_details_container_layout`, `skip_ad_button` (clickable FrameLayout) with a child `skip_ad_button_container` whose content-description is "Werbung überspringen" and `skip_ad_button_text` ("Überspringen"). |
| Ad before the Skip button appears | `ad_progress_text` ("Gesponsert · 1 von 2 ..."), `ad_details_container_layout`; no `skip_ad_button`. |
Fixtures: `youtube_21.41.525_de_watch_{no_ad,ad_before_skip,skippable_ad}.xml`. Only 2 ads sampled.

## Important findings
- **Neither of the two Shorts ads had a Skip button.** The Skip control in `SKIP_AD` was never observed. Shorts ads seem not to be skippable by a button, so the opt-in "Tap Skip automatically" setting probably does nothing in Shorts (it may matter for skippable ads in regular videos, which this app does not target). The `SKIP_AD` labels remain unverified.
- The ad label is a last line of a longer description. `categorizeDescription` now compares the whole text or only the last line; nothing else of the description is kept.
- `reel_time_bar` is a `SeekBar` (clickable) and `reel_progress_bar` is a thin view at the bottom. A SeekBar's `rangeInfo` (current/max) may allow end-of-video detection; see `SPIKE_END_OF_VIDEO.md`.

## Still NOT verified
- Any other YouTube version, any UI language other than German (the non-German ad/skip labels are general-knowledge guesses), a regular full-screen watch page (the Home dump only had the mini-player ids), a skippable ad, a Shorts ad with a different layout, Shorts opened from search/channel pages.
- Only 2 ads were sampled; "absent on ~28 normal pages" is a small sample.
