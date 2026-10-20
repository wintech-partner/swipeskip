# Guardrails for AI sessions on this repo

Update 2026-10-20 (ADR 10): at the human's explicit request the opt-in Skip tap also works on regular YouTube videos through a bounded `WATCH_ADS` session (`foss` only). Scrolling Shorts is still the main feature. Reels/TikTok are still out of scope.

Open-source Android app (Kotlin, Compose, Material 3, Gradle KTS) that scrolls YouTube Shorts on a user-chosen timer. v1 = YouTube Shorts only. Reels/TikTok are NOT to be implemented yet.

## Operating rules
- Preserve working code. Small verifiable steps; conventional commits.
- Never push, create a remote, publish a release or submit to a store without explicit human confirmation.
- Never commit secrets, keystores, passwords, API keys. Never hardcode product ids, prices or credentials.
- Resolve dependency versions from the real repositories into `gradle/libs.versions.toml`, never from memory.
- Report only what actually happened. Don't claim a feature works or give an APK path unless it was built and verified.
- Anything needing a device, store account or human decision goes to `docs/OPEN_ITEMS.md`.

## Product scope and hard prohibitions (every flavor, forever)
- No automated tapping of ads, sponsored links or advertiser pages.
- No artificial engagement: no automated likes, subscriptions, comments, views or watch-time inflation.
- No bypassing ads or timers. **Amended 2026-10-19 at the human's explicit request (ADR 9):** the app may tap the platform's own Skip control only when the user switched on the opt-in "Tap Skip automatically" setting (off by default, `foss` only). There is no manual Skip button (removed 2026-10-19: it added nothing over YouTube's own button). It never taps the ad itself, a badge, a sponsored link or anything but a node categorised as the skip control, at most 3 taps per ad, 2 s apart.
- No anti-detection or human-mimicking behavior (no evasion jitter, no hiding the service or notification).
- No collection of credentials, tokens, watch history or on-screen content.

## Policy design rules
1. Explicit bounded sessions: user-chosen interval, max duration 15/30/60/120/240 min (default 30, no unlimited), optional max scrolls. Ends on limits, screen off, 60 s out of YouTube, or Stop.
2. Passive when idle: no gestures, no window processing without an active session.
3. Visible: ongoing notification with Stop while a session runs.
4. Motion-to-next (accelerometer, user-initiated flick, only during a live session, off by default) is allowed: each flick is one user action.
5. Two modes (`AUTO_TIMER`, `MANUAL_TAP`) behind one interface, chosen per flavor by ONE build flag (`AUTO_TIMER_ENABLED`). `play` stays manual-only until a human decides.
6. Never set `isAccessibilityTool="true"` unless a human truthfully decides it is one. Be honest about audience: the primary goal is convenience; helping people with visual, motor or other physical disabilities is a welcome secondary goal, not the primary one (`docs/ACCESSIBILITY_STATEMENT.md`). Never call the app "accessible" or a disability tool in listings until it has been tested with TalkBack/Switch Access and real users.
7. Minimum scope: service limited to `com.google.android.youtube`; never read, store or log screen text; only structural info in memory.
8. Dedicated disclosure + explicit consent before sending the user to system settings.
9. Trademarks: no YouTube name/logo in app name or icon; keep the "not affiliated" notices.

## Architecture
MVVM, coroutines/StateFlow, DataStore. Modules: `app`, `core/model`, `core/automation` (pure, unit-tested engine), `core/platform-api`, `platform/youtube` (only adapter; matchers are data in `YouTubeMatchers.kt`), `data/settings`, `billing/{api,noop,play}`, `testing/fake-shorts-app`.

## Privacy
No analytics, crash SDKs or ads. No INTERNET permission in `foss`. Settings only in local DataStore. Logs contain no screen text.

## Human-only decisions
Final name and applicationId (placeholder `io.github.wintech_partner.swipeskip` is a release blocker), license, Play submission and whether AUTO_TIMER is acceptable there, accessibility-tool declaration, signing keys, repo creation/push, F-Droid/IzzyOnDroid submission, pricing, native translation review, real-device tests.

## Build
`export JAVA_HOME=<jdk17> ANDROID_HOME=<sdk>` then `./gradlew test lint assembleFossDebug`.
