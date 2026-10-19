# Tested versions

| What | Version | Result |
|---|---|---|
| Real YouTube 19.17.42 (emulator system image, logged out) | 2026-10-19 | Shell and Shorts tab in an error state dumped, see `YOUTUBE_OBSERVED_IDS.md`. **No Shorts feed, comments or ad seen.** Adapter on that dump: `Unknown`. |
| Real YouTube on a real device with Shorts working | none | **Not tested.** |
| Emulator (Pixel 6 AVD, Android 15 / API 35, google_apis x86_64) + fake Shorts app, `FakeShortsEndToEndTest` | 2026-10-19 | **3/3 pass**: the service really dispatches swipes and stops at the scroll limit; pauses on an ad and does not tap Skip by default; taps the fake Skip only with the opt-in on. This proves the pipeline, not the YouTube matchers. |
| JVM unit tests, Robolectric UI tests (Android API 35 simulation) | n/a | 156 tests, 0 failures on 2026-10-19 (`./gradlew test`) |

Add a row per YouTube version and Android version actually tried on hardware.
