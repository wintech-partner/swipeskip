#!/usr/bin/env bash
# Builds store packages into dist/. The same files serve Chrome and Firefox (MV3).
set -euo pipefail
cd "$(dirname "$0")"
node --test test/engine.test.js >/dev/null
rm -rf dist && mkdir dist
v=$(node -p "require('./manifest.json').version")
zip -qr "dist/swipeskip-$v-chrome.zip" manifest.json src icons _locales -x '*.DS_Store'
zip -qr "dist/swipeskip-$v-firefox.zip" manifest.json src icons _locales -x '*.DS_Store'
# AMO reviewers need to rebuild from source: this extension has no build step, so the source is the package.
ls -l dist
