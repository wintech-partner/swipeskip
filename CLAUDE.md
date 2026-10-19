# Guardrails for AI sessions on this repo

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
- No automatic Skip-ad tapping, no bypassing ads or timers. The app may only notify the user.
- No anti-detection or human-mimicking behavior (no evasion jitter, no hiding the service or notification).
- No collection of credentials, tokens, watch history or on-screen content.

## Policy design rules
1. Explicit bounded sessions: user-chosen interval, max duration 15/30/60/120/240 min (default 30, no unlimited), optional max scrolls. Ends on limits, screen off, 60 s out of YouTube, or Stop.
2. Passive when idle: no gestures, no window processing without an active session.
3. Visible: ongoing notification with Stop while a session runs.
4. Two modes (`AUTO_TIMER`, `MANUAL_TAP`) behind one interface, chosen per flavor by ONE build flag (`AUTO_TIMER_ENABLED`). `play` stays manual-only until a human decides.
5. Never set `isAccessibilityTool="true"` unless a human truthfully decides it is one.
6. Minimum scope: service limited to `com.google.android.youtube`; never read, store or log screen text; only structural info in memory.
7. Dedicated disclosure + explicit consent before sending the user to system settings.
8. Trademarks: no YouTube name/logo in app name or icon; keep the "not affiliated" notices.

## Architecture
MVVM, coroutines/StateFlow, DataStore. Modules: `app`, `core/model`, `core/automation` (pure, unit-tested engine), `core/platform-api`, `platform/youtube` (only adapter; matchers are data in `YouTubeMatchers.kt`), `data/settings`, `billing/{api,noop,play}`, `testing/fake-shorts-app`.

## Privacy
No analytics, crash SDKs or ads. No INTERNET permission in `foss`. Settings only in local DataStore. Logs contain no screen text.

## Human-only decisions
Final name and applicationId (placeholder `org.example.autoshorts` is a release blocker), license, Play submission and whether AUTO_TIMER is acceptable there, accessibility-tool declaration, signing keys, repo creation/push, F-Droid/IzzyOnDroid submission, pricing, native translation review, real-device tests.

## Build
`export JAVA_HOME=<jdk17> ANDROID_HOME=<sdk>` then `./gradlew test lint assembleFossDebug`.
