#!/usr/bin/env bash
# Packages extension/ into dist/anti-brainrot-<version>.zip (load-unpacked friendly zip, manifest at the root).
set -euo pipefail
cd "$(dirname "$0")/.."
version=$(node -p "require('./extension/manifest.json').version")
mkdir -p dist
out="dist/anti-brainrot-${version}.zip"
rm -f "$out"
(cd extension && zip -qr "../$out" . -x '*.DS_Store' -x '__MACOSX/*')
echo "wrote $out ($(du -h "$out" | cut -f1))"
