# SwipeSkip browser extension (Chrome, Edge, Firefox)

Desktop sibling of the Android app. Hands-free YouTube Shorts: moves to the next Short when one ends (or every N seconds), in bounded and visible sessions.

- Session length 15 to 240 min (no unlimited), optional Short cap, a visible badge with Stop on the page.
- Pauses while you use the page (wheel, keys, mouse; 8 s), while comments are open, while an ad is likely showing, while the video is paused or the tab is hidden. Ends after 60 s away from Shorts.
- Opt-in, off by default: click YouTube's own Skip button (max 3 clicks per ad). Same ADR 9/10 caveat as the app: this may conflict with YouTube's terms.
- Never clicks ads, never likes/subscribes/comments, reads no page text, no network requests, no analytics. Only permissions: `storage` and `www.youtube.com`.

No build step. `./build.sh` runs the tests and zips `dist/swipeskip-<version>-{chrome,firefox}.zip`.
Try it unpacked: Chrome `chrome://extensions` → Developer mode → Load unpacked → this folder; Firefox `about:debugging` → Load Temporary Add-on → `manifest.json`.

Status: engine unit-tested, `web-ext lint` clean. The YouTube selectors (next button, ad and comment panel markers) are unverified on live YouTube; see `docs/STORE_SUBMISSION.md`.
