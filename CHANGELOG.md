# Changelog

## 0.1.2
- Reproducible build: AGP no longer embeds git revision info (`vcsInfo.include = false`), so the APK is identical across checkouts.

## 0.1.1
- First run on a real phone (Android 14, YouTube 21.41.525, German): scrolling, pauses, notification Stop, screen-off and 60 s-away end work.
- Watch mode: a bounded session that only taps YouTube's own Skip button during regular videos (`foss` only, ADR 10).
- Sixteen languages (Hindi, Japanese, Vietnamese, Korean, Thai, Filipino, Bengali and Urdu added; all machine-drafted).
- Name SwipeSkip, application id `io.github.wintech_partner.swipeskip`, new double-chevron icon.
- Home warns when notifications are off; Settings says changes apply from the next session.
- Skip tap hardened: whole-string label, only inside an ad container.

## 0.1.0 (tag only, superseded)
- Session engine and state machine (pure Kotlin, unit tested).
- YouTube Shorts adapter with unverified matcher candidates.
- Accessibility service, ongoing notification, Compose UI, onboarding with consent, diagnostics.
- Nine languages (machine-drafted except English).
- `foss` and `play` flavors; billing abstraction with `noop` and Play implementations.
