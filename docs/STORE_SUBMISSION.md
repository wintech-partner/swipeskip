# Browser extension store submission

Packages: `extension/build.sh` → `extension/dist/`. Both stores need the human's own developer account.

## Chrome Web Store
1. Developer account at https://chrome.google.com/webstore/devconsole (one-time 5 USD fee).
2. New item → upload `swipeskip-<v>-chrome.zip`.
3. Listing: use the description in `extension/_locales/en/messages.json`; category Productivity; screenshots 1280x800 (to be taken).
4. Privacy tab: single purpose = "Automatically advance to the next YouTube Short in a user-started session." Permission justifications: `storage` = saves the user's settings locally; host `www.youtube.com` = the content script that advances Shorts. Data usage (Chrome build, opt-in): tick "Website content"? NO. Tick only what applies: none of the listed personal data categories is collected; the anonymous counters are usage statistics sent with the user's consent. Certify: not sold, not used for unrelated purposes, not used for creditworthiness. Single purpose of the data: keep the extension working and fix bugs. Privacy policy URL: https://github.com/wintech-partner/swipeskip/blob/main/PRIVACY.md
5. Submit for review.

## Firefox (addons.mozilla.org)
1. Free account at https://addons.mozilla.org/developers/.
2. Submit a new add-on → "On this site" → upload `swipeskip-<v>-firefox.zip`. Upload the *firefox* zip only: it has no telemetry code and no network permission. Gecko id `swipeskip@wintech-partner.github.io`, `data_collection_permissions: none`.
3. Source code: not needed (no build step, nothing minified).
4. Same listing text and screenshots.

## Risks
- Auto-clicking Skip (opt-in) may be flagged by reviewers or conflict with YouTube's terms; if rejected, ship without it.
- Name/description avoid the YouTube trademark except "YouTube Shorts" to describe compatibility; keep the not-affiliated notice.
- Selectors need a check on live YouTube before the first submission.

## Collector (Chrome stats)
- Code `telemetry-server/` (stdlib Python + SQLite), compose service `swipeskip-telemetry` in home-server-infra, public only through a Render relay (see home-server-infra `docs/RENDER_PROXY.md`) pointed at Caddy `:8082` (POST `/v1/event` only).
- Relay LIVE (2026-10-21) at https://swipeskip-telemetry.onrender.com (free plan: sleeps after 15 min idle, first event after a pause may be lost). Relay env: `TS_AUTHKEY`, `TS_HOSTNAME=swipeskip-telemetry-relay`, `TARGET_HOST=<home server Tailscale IP>`, `TARGET_PORT=8082`. The relay's public https URL must equal `TELEMETRY_URL` at build time (default `https://swipeskip-telemetry.onrender.com`).
- Read stats on the tailnet: `curl -H "Authorization: Bearer $SWIPESKIP_TELEMETRY_ADMIN_TOKEN" http://<tailscale-ip>:7870/stats?days=7`.

## Price and donations
Free everywhere (decided 2026-10-21). Donations via https://liberapay.com/wintech-partners (F-Droid `Liberapay:` field, `.github/FUNDING.yml`, README, extension popup). Do not mention a paid version in the F-Droid build (it would add the NonFreeAdd anti-feature).
