# Competitor research

These findings come from the project brief (public listings and aggregators, partial snapshots, not a full audit). They were **not re-verified** while building this version; re-check before quoting them.

| Finding | Source | Requirement |
|---|---|---|
| "Auto Scroll - Reels & Shorts" (`com.moumen.autoPlay`): 100k+ downloads, about 2.1/5 from roughly 645 ratings | https://www.appbrain.com/app/auto-scroll-reels-shorts/com.moumen.autoPlay | Stability is the headline feature |
| Reviews: premium not delivered, many ads, limited timer, no support | https://play.google.com/store/apps/details?id=com.moumen.autoPlay | Free tier fully usable, no ads, restore purchase, public issue tracker |
| Fixed timer ill-suited to Shorts of 5-20+ s | same | End-of-video spike (`SPIKE_END_OF_VIDEO.md`), custom interval |
| Users want intervals below 15 s; 35 s reported not working in free version | AppBrain listing | Presets 3/5/8/10/15/30 plus custom 3-120 s, all free |
| A Shorts-blocking app reviewed as periodically stopping, even after reinstall | https://play.google.com/store/apps/details?id=com.noscroll.antiscroll | Self-diagnostics, watchdog, "Why isn't it working?" |
| Browser extensions: silent stops after updates, lag, broken shortcuts | Chrome Web Store bmjbgjndjeinofbgoanhkdgeclbfdbpm; https://firefox-stats.com/d/auto-youtube-shorts-scroller | Never fail silently; keep per-scroll work minimal |
| Requests: pause with comments open, direction, creator blocking, watch-percentage scroll, skip ads | Chrome Web Store ckbnikemebopgknkpgjlkbffpkkhblbe; https://addons.mozilla.org/en-CA/firefox/addon/auto-youtube-shorts-scroller/ | Comments pause, direction setting; skip-ad automation and creator blocking out of scope |
| One extension caps free users at 15 scrolls/day and gates custom intervals | https://www.pomodotree.com/auto-scroll/ | Do not copy; core scrolling stays free |

"AdSkicker" could not be found in the available sources; nothing is claimed about it.

## Manual device test checklist (human)
- [ ] Install `foss` debug APK on a real device, complete onboarding, enable the service (also test the restricted-settings path with a sideloaded APK)
- [ ] Start with real YouTube Shorts: does it scroll at the chosen interval (3, 10, 35 s)?
- [ ] Verify/replace every id in `YouTubeMatchers.kt` using a view-hierarchy dump; record the YouTube version in `TESTED_VERSIONS.md`
- [ ] Open comments: pause? Leave Shorts feed: pause? Touch screen: pause and resume delay?
- [ ] Ad in Shorts: pause, "ad likely" wording, skip notification, never tapped
- [ ] Lock the screen, leave YouTube for 60 s, revoke the service mid-session: clean stop and notification gone
- [ ] Notification Stop works from the shade and lock screen; check it cannot be swiped away on your OEM
- [ ] Battery: `adb shell dumpsys batterystats --reset`, run a 30-minute session, `adb bugreport` or `dumpsys batterystats`; record real numbers only
- [ ] Install a competitor and compare behavior and ratings claims
