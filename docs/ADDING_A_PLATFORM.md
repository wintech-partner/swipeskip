# Adding a platform (not done in v1)

1. Add an id to `PlatformId` (`core/model`).
2. Create `platform/<name>` with a class implementing `PlatformAdapter` (`classify`, `nextItemGesture`, `adMonitor`, `categorizeDescription`). Keep ids and localized label sets in one data file like `YouTubeMatchers.kt`.
3. Work only on `NodeSnapshot`; write JVM fixtures per supported language.
4. Wire it in `AppContainer`, add the package to the service `packageNames` and `<queries>`, and update `docs/PERMISSIONS.md`, `PRIVACY.md` and the disclosure strings in all locales.
5. Re-read the target platform's terms and the Play policy first. All hard prohibitions in `CLAUDE.md` apply unchanged.
Instagram Reels and TikTok adapters must not be added without that review.
