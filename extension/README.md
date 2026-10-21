# SwipeSkip browser extension (Edge, Firefox)

Desktop sibling of the Android app. Hands-free YouTube Shorts: moves to the next Short when one ends (or every N seconds), in bounded and visible sessions.

- Session length 15 to 240 min (no unlimited), optional Short cap, a visible badge with Stop on the page.
- Pauses while you use the page (wheel, keys, mouse; 8 s), while comments are open, while an ad is likely showing, while the video is paused or the tab is hidden. Ends after 60 s away from Shorts.
- Opt-in, off by default: click YouTube's own Skip button (max 3 clicks per ad). Same ADR 9/10 caveat as the app: this may conflict with YouTube's terms.
- Edge build only: an opt-in (off by default) anonymous usage-stats setting, see `PRIVACY.md`. The Firefox build contains no telemetry code and no network permission (`build.sh` fails if that changes).
- Never clicks ads, never likes/subscribes/comments, reads no page text, no analytics unless the Edge user opts in. Permissions: `storage`, `www.youtube.com` (Edge adds the collector's address).

`./build.sh` runs the tests and builds two different packages (edge, firefox) into `dist/` (set `TELEMETRY_URL` to the collector's public https address for the Edge one).
Try it unpacked from `dist/stage-edge` (Edge: `edge://extensions` → Developer mode → Load unpacked) or `dist/stage-firefox` (`about:debugging` → Load Temporary Add-on → `manifest.json`).

Status: engine unit-tested, `web-ext lint` clean. The YouTube selectors (next button, ad and comment panel markers) are unverified on live YouTube; see `docs/STORE_SUBMISSION.md`.

Opt-in "Move past ad Shorts" (ADR 11) swipes on, never clicks the ad; its code path is covered by unit tests of the setting only, not yet run live.

Live check (2026-10-21, Chrome, desktop YouTube, content script injected with a stubbed `chrome`): the Next button `#navigation-button-down button` and `#shorts-player video` exist; a click moves to the next Short but the address changes about 4 s later, so the script waits for the address change before advancing again (an earlier 1.2 s fallback skipped a Short). An in-feed ad Short was detected through `ytd-in-feed-ad-layout-renderer` and paused the session. Same day, same method: end-of-video mode advanced after the Short ended (Next clicked, address changed about 3.5 s later), and an open comments panel (`ENGAGEMENT_PANEL_VISIBILITY_EXPANDED`) paused the session. Not yet checked live: Skip button, the popup, "Move past ad Shorts", the Firefox build.
