# Browser extension store submission

Packages: `extension/build.sh` → `extension/dist/`. Each store needs the human's own developer account.

## Chrome: not a target
Decided 2026-10-21: no Chrome Web Store listing and no Chrome support promise. Targets are Edge and Firefox. The Chromium build (`-edge.zip`) exists only because Edge uses it.

## Edge Add-ons (Microsoft Partner Center)
Free. Upload `swipeskip-<v>-edge.zip` (it has the opt-in telemetry setting).
1. Sign in at https://partner.microsoft.com/dashboard/microsoftedge/ with a Microsoft account and register as an Edge extensions developer (individual, no fee).
2. Microsoft Edge → Overview → "Create new extension" → upload `swipeskip-<v>-edge.zip`.
3. Availability: public, all markets, no hide-from-store.
4. Properties: category Productivity; privacy policy URL https://github.com/wintech-partner/swipeskip/blob/main/PRIVACY.md. No login is needed to use the extension.
5. Store listings (add English first, then tr, de): description from `extension/_locales/en/messages.json`, logo 300x300 (`icons/icon-128.png` scaled up or a new one), at least one screenshot 1280x800 or 640x480, optional search terms. Do not use the word "YouTube" as a brand; "YouTube Shorts" only to describe compatibility, keep the not-affiliated notice.
6. Notes for certification (visible to testers only): "Open https://www.youtube.com/shorts, click the toolbar icon, Start. The extension swipes to the next Short. Only opt-in setting that touches ads: 'Move past ad Shorts' swipes on, never clicks. Usage stats are off by default."
7. Publish. Review usually takes up to 7 business days.

## Firefox (addons.mozilla.org)
1. Free account at https://addons.mozilla.org/developers/.
2. Submit a new add-on → "On this site" → upload `swipeskip-<v>-firefox.zip`. Upload the *firefox* zip only: it has no telemetry code and no network permission. Gecko id `swipeskip@wintech-partner.github.io`, `data_collection_permissions: none`.
3. Source code: not needed (no build step, nothing minified).
4. Same listing text and screenshots.

## Risks
- Auto-clicking Skip (opt-in) may be flagged by reviewers or conflict with YouTube's terms; if rejected, ship without it.
- Name/description avoid the YouTube trademark except "YouTube Shorts" to describe compatibility; keep the not-affiliated notice.
- Selectors need a check on live YouTube before the first submission.

## Collector (Edge stats)
- Code `telemetry-server/` (stdlib Python + SQLite), compose service `swipeskip-telemetry` in home-server-infra, public only through a Render relay (see home-server-infra `docs/RENDER_PROXY.md`) pointed at Caddy `:8082` (POST `/v1/event` only).
- Relay LIVE (2026-10-21) at https://swipeskip-telemetry.onrender.com (free plan: sleeps after 15 min idle, first event after a pause may be lost). Relay env: `TS_AUTHKEY`, `TS_HOSTNAME=swipeskip-telemetry-relay`, `TARGET_HOST=<home server Tailscale IP>`, `TARGET_PORT=8082`. The relay's public https URL must equal `TELEMETRY_URL` at build time (default `https://swipeskip-telemetry.onrender.com`).
- Read stats on the tailnet: `curl -H "Authorization: Bearer $SWIPESKIP_TELEMETRY_ADMIN_TOKEN" http://<tailscale-ip>:7870/stats?days=7`.

## Price and donations
Free everywhere (decided 2026-10-21). Donations via https://liberapay.com/wintech-partners (F-Droid `Liberapay:` field, `.github/FUNDING.yml`, README, extension popup). Do not mention a paid version in the F-Droid build (it would add the NonFreeAdd anti-feature).
