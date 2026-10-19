# Distribution (preparation only; the human submits)

| Channel | State |
|---|---|
| GitHub Releases | Release workflow builds a signed APK from secrets, writes `SHA256SUMS`, attaches both. Needs: repo, signing keystore as secrets. |
| Obtainium | Asset name `autoshorts-<version>-foss-release.apk` is stable and parseable; README has instructions. |
| F-Droid | `foss` flavor: no Play Services/Billing/trackers, no INTERNET permission, `dependenciesInfo` excluded. `fastlane/metadata/android/` present for en-US and tr (screenshots missing). Reproducibility is **not verified**. Draft request below. |
| IzzyOnDroid | Its AppInclusionPolicy states strong opposition to apps created fully or partly with generative AI and wants the code free of LLM output (https://izzyondroid.org/docs/general/AppInclusionPolicy/, read 2026-10-19). Rejection is likely; do not promise inclusion. |
| Google Play | `play` flavor, optional, manual-tap only. Prepare Accessibility declaration, prominent disclosure and data-safety answers (see `POLICY_NOTES.md`); approval uncertain, `AUTO_TIMER` likely rejected. Not submitted. |

## F-Droid inclusion request draft
> Autoshorts is a free (Apache-2.0) companion that scrolls YouTube Shorts on a user-chosen timer using Android's accessibility service. Only the `foss` flavor is proposed: no network permission, no analytics, no proprietary dependencies. It is session-bound, shows an ongoing notification with Stop, never taps ads or skip controls and never stores screen text. Anti-features: none known (`NonFreeNet`/`Tracking` do not apply). Note: developed with AI assistance and human review.
