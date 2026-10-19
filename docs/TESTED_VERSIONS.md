# Tested versions

| What | Version | Result |
|---|---|---|
| Real YouTube app on a real device | none | **Not tested.** |
| Emulator (Pixel 6 AVD, Android 15 / API 35, google_apis x86_64) + fake Shorts app, `FakeShortsEndToEndTest` | 2026-10-19 | **3/3 pass**: the service really dispatches swipes and stops at the scroll limit; pauses on an ad and does not tap Skip by default; taps the fake Skip only with the opt-in on. This proves the pipeline, not the YouTube matchers. |
| JVM unit tests, Robolectric UI tests (Android API 35 simulation) | n/a | 156 tests, 0 failures on 2026-10-19 (`./gradlew test`) |

Add a row per YouTube version and Android version actually tried on hardware.
