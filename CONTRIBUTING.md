# Contributing

- Read `CLAUDE.md` first: the guardrails there (no ad tapping, no engagement automation, no evasion, no screen text) are not negotiable and apply to every contribution.
- Small PRs, conventional commits (`feat:`, `fix:`, `docs:`, `test:`, `ci:`).
- `./gradlew test lint assembleFossDebug` must pass. New logic needs unit tests; YouTube matcher changes need a fixture in `platform/youtube` tests and an entry in `docs/TESTED_VERSIONS.md`.
- When YouTube changes and detection breaks, the fix belongs in `platform/youtube/.../YouTubeMatchers.kt` only.
- Translations: edit `app/src/main/res/values-<locale>/strings.xml`; Lint fails on missing translations. Native review of the machine-drafted texts is very welcome.
- Disclose AI assistance in the PR if you used it. Do not paste screen recordings or logs containing personal content.
