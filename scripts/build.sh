#!/usr/bin/env bash
# Packages extension/ into dist/anti-brainrot-<version>.zip (load-unpacked
# friendly zip, manifest at the root) and, when the signing key is around,
# dist/anti-brainrot-<version>.crx for the policy install. The key is read
# from $ABR_CRX_KEY or ~/.config/anti-brainrot/key.pem.
set -euo pipefail
cd "$(dirname "$0")/.."
version=$(node -p "require('./extension/manifest.json').version")
mkdir -p dist
out="dist/anti-brainrot-${version}.zip"
rm -f "$out"
# _metadata is Chrome's indexed ruleset cache, written next to an unpacked
# extension after it has been loaded; it is not part of the source.
(cd extension && zip -qr "../$out" . -x '*.DS_Store' -x '__MACOSX/*' -x '_metadata/*')
echo "wrote $out ($(du -h "$out" | cut -f1))"

key="${ABR_CRX_KEY:-$HOME/.config/anti-brainrot/key.pem}"
crx="dist/anti-brainrot-${version}.crx"
rm -f "$crx"
if [ -f "$key" ]; then
  node scripts/pack-crx.mjs "$out" "$crx" "$key"
else
  echo "no signing key at $key, skipping the crx"
fi
