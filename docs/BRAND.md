# Unrot brand

## The mark

The Unrot mark is a single leaf whose silhouette is a play triangle pointing
right. The blade is a rounded triangle with a gentle belly about a third of
the way from the base, a short stem leaves the middle of the base, and one
midrib runs from the stem toward the tip. The midrib is not drawn on top of
the leaf; it is cut out of it, so whatever sits behind the mark shows
through. The shape reads as "play" at a glance and as "a leaf" on a second
look: growth as the opposite of rot.

The drawing borrows its manner from the hand-drawn airplane at
flappingairplanes.com: one confident filled shape, slightly uneven edges,
no gradients, no outline, no text inside the mark, plenty of space around it.
Keep it that way. Do not add a brain, a crossed-out symbol, a YouTube play
button or any second colour inside the mark.

## Palette

| Name      | Hex     | Use                                              |
|-----------|---------|--------------------------------------------------|
| Navy ink  | #1a1a2e | The mark and text on light backgrounds           |
| Sky blue  | #73b8ee | Icon tile, page backgrounds, accents             |
| Light sky | #b2dcfa | Hand-drawn clouds, hover states, soft panels     |
| Cream     | #eceade | The mark on dark backgrounds, light page surface |
| Dark      | #0a1220 | Dark page surface, footer                        |

Pair navy on sky, navy on cream, or cream on dark. Avoid navy on dark and
cream on sky: both fall below a comfortable contrast.

## Files

All sources live in `assets/` and are hand-written SVG paths with no fonts,
rasters or external references.

| File                    | What it is                                        | Use it for                                     |
|-------------------------|---------------------------------------------------|------------------------------------------------|
| `assets/logo.svg`       | Mark alone, navy, 512 viewBox, transparent        | Light backgrounds: README, site header, store  |
| `assets/logo-cream.svg` | Same mark in cream                                | Dark backgrounds                               |
| `assets/icon.svg`       | Mark on a sky rounded square, 128 viewBox         | 48 px and 128 px extension icons, store tile   |
| `assets/icon-small.svg` | Heavier stem and midrib, larger mark on the tile  | 16 px and 32 px toolbar icons only             |
| `assets/wordmark.svg`   | Mark plus "Unrot" in Inter SemiBold, as outlines  | Site header, README banner, social cards       |

The rounded square in the icons uses a corner radius of 22 percent of the
tile. The mark spans about 63 percent of the tile in `icon.svg` and about
72 percent in `icon-small.svg`.

## Rendering the PNGs

`npm run icons` (which runs `scripts/render-icons.sh`) uses rsvg-convert to
write `extension/icons/icon-16.png`, `icon-32.png`, `icon-48.png` and
`icon-128.png`, plus `site/assets/logo-512.png`. Edit the SVGs, run the
script and commit the PNGs alongside them. The 16 and 32 px files come from
`icon-small.svg`; the 48 and 128 px files come from `icon.svg`.

## Editing the mark

The blade and the midrib share one path in `logo.svg`, `logo-cream.svg` and
`icon.svg`, so a change to the blade must be copied to all three (and to
`wordmark.svg`). `icon-small.svg` shares the blade but has its own stem and
midrib coordinates. Check any edit at 16, 32, 48 and 128 px before
committing: the leaf and play reading must survive at 16 px and the midrib
must still be visible at 32 px.
