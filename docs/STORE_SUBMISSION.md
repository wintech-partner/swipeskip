# Browser extension store submission

Packages: `extension/build.sh` → `extension/dist/`. Both stores need the human's own developer account.

## Chrome Web Store
1. Developer account at https://chrome.google.com/webstore/devconsole (one-time 5 USD fee).
2. New item → upload `swipeskip-<v>-chrome.zip`.
3. Listing: use the description in `extension/_locales/en/messages.json`; category Productivity; screenshots 1280x800 (to be taken).
4. Privacy tab: single purpose = "Automatically advance to the next YouTube Short in a user-started session." Permission justifications: `storage` = saves the user's settings locally; host `www.youtube.com` = the content script that advances Shorts. Data usage: collects nothing. Privacy policy URL: https://github.com/wintech-partner/swipeskip/blob/main/PRIVACY.md
5. Submit for review.

## Firefox (addons.mozilla.org)
1. Free account at https://addons.mozilla.org/developers/.
2. Submit a new add-on → "On this site" → upload `swipeskip-<v>-firefox.zip`. Gecko id `swipeskip@wintech-partner.github.io`, `data_collection_permissions: none`.
3. Source code: not needed (no build step, nothing minified).
4. Same listing text and screenshots.

## Risks
- Auto-clicking Skip (opt-in) may be flagged by reviewers or conflict with YouTube's terms; if rejected, ship without it.
- Name/description avoid the YouTube trademark except "YouTube Shorts" to describe compatibility; keep the not-affiliated notice.
- Selectors need a check on live YouTube before the first submission.
