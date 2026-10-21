#!/usr/bin/env bash
# Builds two store packages into dist/ from the same source:
#   firefox: no telemetry code at all, no network permission (AMO: data collection "none")
#   edge:    adds edge-only/ (opt-in anonymous stats sent to TELEMETRY_URL by a background worker)
# TELEMETRY_URL must be the public https address of the collector (the Render relay in front of the home server).
set -euo pipefail
cd "$(dirname "$0")"
TELEMETRY_URL="${TELEMETRY_URL:-https://swipeskip-telemetry.onrender.com}"
case "$TELEMETRY_URL" in https://*) ;; *) echo "TELEMETRY_URL must be https" >&2; exit 1;; esac
node --test test/engine.test.js >/dev/null
rm -rf dist && mkdir -p dist/stage-firefox dist/stage-edge
v=$(node -p "require('./manifest.json').version")
for t in firefox edge; do cp -r manifest.json src icons _locales "dist/stage-$t/"; done

# Edge build: telemetry files, manifest changes, popup checkbox
c=dist/stage-edge
cp edge-only/telemetry-content.js edge-only/popup-telemetry.js "$c/src/"
sed "s#__TELEMETRY_URL__#$TELEMETRY_URL#" edge-only/background.js > "$c/src/background.js"
python3 - "$c" "$TELEMETRY_URL" <<'PY'
import json, sys
d, url = sys.argv[1:]
m = json.load(open(f"{d}/manifest.json"))
m["content_scripts"][0]["js"] = ["src/engine.js", "src/telemetry-content.js", "src/content.js"]
m["host_permissions"].append(url.rstrip("/") + "/*")
m["background"] = {"service_worker": "src/background.js"}
m.pop("browser_specific_settings", None)
json.dump(m, open(f"{d}/manifest.json", "w"), indent=2)
html = open(f"{d}/src/popup.html").read().replace("<!--TELEMETRY_UI-->", open("edge-only/popup-telemetry.html").read().rstrip())
open(f"{d}/src/popup.html", "w").write(html)
PY

# Firefox build: strip the marker, assert it cannot send anything anywhere
f=dist/stage-firefox
sed -i '/<!--TELEMETRY_UI-->/d' "$f/src/popup.html"
if grep -rnE "fetch\(|XMLHttpRequest|sendBeacon|WebSocket|service_worker|https?://[^\"' ]*(onrender|workers)" "$f/src" "$f/manifest.json"; then
  echo "network code leaked into the Firefox build" >&2; exit 1
fi
if [ -n "$(ls "$f/src" | grep -iE 'telemetry|background')" ]; then echo "telemetry file leaked into the Firefox build" >&2; exit 1; fi

(cd dist/stage-edge && zip -qr "../swipeskip-$v-edge.zip" .)
(cd dist/stage-firefox && zip -qr "../swipeskip-$v-firefox.zip" .)
ls -l dist/*.zip
