#!/usr/bin/env bash
# Render the extension icons and the site logo from the SVG sources in assets/.
# Requires rsvg-convert (librsvg). Run from anywhere: paths are repo-relative.
set -euo pipefail

cd "$(dirname "$0")/.."

command -v rsvg-convert >/dev/null || {
  echo "render-icons: rsvg-convert not found (install librsvg2-bin)" >&2
  exit 1
}

mkdir -p extension/icons site/assets

# 16 and 32 px use the heavier small-size drawing; 48 and 128 use the full icon.
rsvg-convert -w 16  -h 16  assets/icon-small.svg -o extension/icons/icon-16.png
rsvg-convert -w 32  -h 32  assets/icon-small.svg -o extension/icons/icon-32.png
rsvg-convert -w 48  -h 48  assets/icon.svg       -o extension/icons/icon-48.png
rsvg-convert -w 128 -h 128 assets/icon.svg       -o extension/icons/icon-128.png

# Transparent logo for the website and store listing.
rsvg-convert -w 512 -h 512 assets/logo.svg -o site/assets/logo-512.png

ls -l extension/icons/icon-*.png site/assets/logo-512.png
