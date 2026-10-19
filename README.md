# Autoshorts

Free, open-source Android companion that scrolls **YouTube Shorts** hands-free, on a timer you choose, with a Start/Stop control that always works. Turkish: [README.tr.md](README.tr.md).

> Autoshorts is not affiliated with or endorsed by YouTube or Google. "YouTube" is a trademark of Google LLC, used here only to describe what the app works with.
>
> This project was developed with AI assistance and human review.

**Status: pre-release (0.1.0).** It builds, and the pure logic and UI are unit-tested. It has **not** been run on a real device against the real YouTube app yet; see [docs/OPEN_ITEMS.md](docs/OPEN_ITEMS.md). The name and application id are placeholders.

## What it does

- Sessions you start: interval 3 to 120 s (presets 3/5/8/10/15/30 or custom), length 15 to 240 min (no unlimited), optional scroll cap, direction.
- Pauses when you touch the screen, when comments are open, when you leave the Shorts feed, and when an ad is *likely* showing.
- Ongoing notification with a Stop button. Screen off, 60 s away from YouTube, or any limit ends the session.
- Diagnostics screen and a copyable report with no screen content.
- Languages: English, Turkish, German, Spanish, French, Brazilian Portuguese, Russian, Arabic, Indonesian (non-English texts are machine-drafted, see open items).

## Extras

- **Motion:** flick the phone sideways or back and forth to go to the next Short (off by default).
- **Skip ad:** a "Skip ad" button taps YouTube's own Skip control when it is likely visible. An opt-in "Tap Skip automatically" setting exists in the `foss` build only (off by default). See ADR 9; unverified on a real device.

## What it will never do

No tapping ads (only YouTube's own Skip control, and only as above), no automated likes/subscriptions/comments/views, no bypassing ads, no anti-detection tricks, no collecting credentials, history or screen content. No analytics, no ads, and the `foss` build has no network permission.

## Install

1. Download the APK from GitHub Releases (when one exists) and check the `SHA-256` file.
2. Open it. If Android refuses to let you turn the accessibility service on, open **App info → ⋮ → Allow restricted settings** (the option may only appear after you have tried to switch the service on once), then try again.
3. Follow the in-app setup: read the disclosure, consent, enable the service, allow notifications.

Obtainium: add the repository URL; release assets are named `autoshorts-<version>-foss-release.apk`.

## Build

```
export JAVA_HOME=/path/to/jdk17 ANDROID_HOME=/path/to/Android/Sdk
./gradlew test lint assembleFossDebug     # APK: app/build/outputs/apk/foss/debug/
./gradlew assemblePlayDebug               # Play flavor (manual-tap only until decided)
```

Open the folder in Android Studio to work on it. Instrumented end-to-end test (needs an emulator or device):
`./gradlew :testing:fake-shorts-app:installDebug :app:connectedFossDebugAndroidTest`.

## Docs

[PLAN](docs/PLAN.md) · [DECISIONS](docs/DECISIONS.md) · [POLICY_NOTES](docs/POLICY_NOTES.md) · [PERMISSIONS](docs/PERMISSIONS.md) · [COMPETITOR_RESEARCH](docs/COMPETITOR_RESEARCH.md) · [SPIKE_END_OF_VIDEO](docs/SPIKE_END_OF_VIDEO.md) · [DISTRIBUTION](docs/DISTRIBUTION.md) · [ADDING_A_PLATFORM](docs/ADDING_A_PLATFORM.md) · [TESTED_VERSIONS](docs/TESTED_VERSIONS.md) · [OPEN_ITEMS](docs/OPEN_ITEMS.md)

License: Apache-2.0 (choice flagged in open items).
