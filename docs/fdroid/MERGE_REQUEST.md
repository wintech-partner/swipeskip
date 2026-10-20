# Draft merge request text for gitlab.com/fdroid/fdroiddata

Title: New App: io.github.wintech_partner.swipeskip

SwipeSkip is a free (Apache-2.0) companion that scrolls YouTube Shorts on a user-chosen timer and can optionally tap YouTube's own Skip button, using Android's accessibility service. Source: https://github.com/wintech-partner/swipeskip (tag v0.1.1).

- Built with the `foss` flavor: no INTERNET permission, no analytics, no proprietary dependencies, no Play Services or Billing.
- The accessibility service is limited to the YouTube package, only runs during a session the user starts, shows an ongoing notification with Stop, and never reads or stores screen text.
- It never taps an advertisement itself; the optional Skip tap targets YouTube's own Skip control only.
- Developed with AI assistance and human review. Tested on one real phone (Android 14); other devices untested.
- Anti-features: none known. Happy to add any the reviewers think apply.
- I am the author.
