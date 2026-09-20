# Unrot design spec

Date: 2026-09-20
Status: approved (autonomous build, decisions recorded here)

## 1. Purpose

Unrot is a Chrome (Manifest V3) extension that replaces three extensions the
author runs today:

- Remove YouTube Shorts 2.1.7 (hides Shorts everywhere)
- Youtube-shorts block 1.5.5 (opens /shorts/ID as a normal /watch?v=ID page)
- Unhook 1.6.9 (per-feature hiding of the home feed, sidebar, comments, end
  screens, header, and so on)

It adds two things those extensions do not have:

1. A friction timer. Turning the distraction filter off is not instant. A
   countdown of a length chosen when the filter was turned on has to run to
   the end inside the popup. Closing the popup, or clicking away, cancels the
   countdown and the filter stays on.
2. An educational-only mode. When on, only videos whose YouTube category is in
   an allow list (default: Education, Science & Technology, Howto & Style) can
   play. Anything else is paused and covered by a plain block screen.

Shorts blocking is unconditional. It is not a setting. As long as the
extension is installed and enabled in Chrome, Shorts shelves, guide entries and
results are hidden and /shorts/ URLs are rewritten to /watch URLs.

## 2. Naming and identity

- Product name: Unrot
- Tagline: YouTube without the brain rot
- Repository: github.com/danieltyukov/yt-anti-brain-rot (public, MIT)
- Website: https://danieltyukov.github.io/yt-anti-brain-rot/
- Visual theme: borrowed from flappingairplanes.com. Sky-blue gradient
  (#73b8ee to #ffffff), hand-drawn brush-stroke illustration and clouds,
  DIN-style sans (Bahnschrift, with Barlow as the web fallback), navy ink text
  (#1a1a2e), a dark variant with #0a1220 background and cream (#eceade) text.
- Logo: a single hand-drawn leaf whose silhouette is a play triangle, with one
  midrib stroke. Navy ink on sky blue. The icon is the same mark on a rounded
  sky-blue square. No text in the icon.

## 3. Scope

In scope:

- Desktop YouTube (www.youtube.com). m.youtube.com only gets the Shorts URL
  redirect rule.
- Chrome and Chromium-based browsers that support MV3 (Chrome 111+ because of
  main-world content scripts and `:has()` in CSS).
- Popup, options page, unit tests, GitHub Actions CI, release packaging, and
  a one-page install website.

Out of scope for v1:

- Firefox build.
- Syncing settings across browsers beyond what chrome.storage.sync provides.
- Classifying feed or search results as educational. Only watch pages are
  gated, because category data is only available there without extra network
  requests.

## 4. Feature list (settings)

Everything below except Shorts sits under one master switch called the
filter (`focus.enabled`). When the filter is off none of these apply.

| Key | Popup label | Default | Mechanism |
| --- | --- | --- | --- |
| homeFeed | Hide Home Feed | on | CSS |
| redirectHome | Redirect to Subscriptions (child of homeFeed) | on | JS navigation |
| sidebar | Hide Video Sidebar | off | CSS |
| sidebarRecommended | Hide Recommended (child) | on | CSS |
| liveChat | Hide Live Chat (child) | on | CSS |
| playlist | Hide Playlist (child) | off | CSS |
| fundraiser | Hide Fundraiser (child) | on | CSS |
| endScreenFeed | Hide End Screen Feed | on | CSS |
| endScreenCards | Hide End Screen Cards | on | CSS |
| shorts | Hide Shorts | always on, not editable | CSS + DNR redirect + JS SPA redirect |
| comments | Hide Comments | on | CSS |
| mixes | Hide Mixes | on | CSS |
| merch | Hide Merch, Tickets, Offers | on | CSS |
| videoInfo | Hide Video Info | off | CSS |
| topHeader | Hide Top Header | off | CSS |
| notifications | Hide Notifications (child of topHeader) | on | CSS |
| inaptSearch | Hide Inapt Search Results | on | CSS |
| explore | Hide Explore, Trending | on | CSS |
| moreFromYouTube | Hide More from YouTube | on | CSS |
| subscriptions | Hide Subscriptions | off | CSS |
| autoplay | Disable Autoplay | on | JS |
| annotations | Disable Annotations | on | CSS |
| educational | Educational videos only | off | JS (main-world bridge + overlay) |

Child rows only take effect when the parent is off, matching Unhook: the
parent hides the whole region, the children hide parts of it.

Educational mode settings (options page):

- allowedCategories: list of YouTube category names. Default
  ["Education", "Science & Technology", "Howto & Style"].
- allowedChannels: free-text list of channel handles (@name), channel IDs
  (UC...) or exact channel names that are always allowed.

Filter settings:

- focus.enabled: boolean.
- focus.unlockDelaySec: integer seconds. Choices in the popup: 0 (instant),
  30, 60, 300, 600, 1800, 3600. Editable only while the filter is off. Locked
  while the filter is on.

Theme: system, light, dark (popup and options page).

## 5. Architecture

```
extension/
  manifest.json           MV3; permissions: storage, declarativeNetRequest;
                          host_permissions: *://www.youtube.com/*, *://m.youtube.com/*
  background.js           installs defaults on first run, migrates settings
  rules/shorts.json       static DNR redirect: /shorts/ID -> /watch?v=ID
  lib/features.js         feature registry (id, label, parent, default, html attribute)
  lib/settings.js         defaults, load/save, migration, attribute derivation
  lib/shorts.js           pure URL rewrite helpers
  lib/education.js        pure category / channel policy
  lib/countdown.js        pure countdown state machine used by the popup
  content/hide.css        all hiding rules, keyed by attributes on <html>
  content/content.js      isolated world: applies attributes, SPA navigation,
                          home redirect, autoplay, educational overlay
  content/page-bridge.js  main world: reads player response, emits DOM events
  popup/                  toggles tree, filter switch, countdown, delay picker
  options/                educational settings, import/export, reset
  icons/                  16/32/48/128 PNG rendered from assets/icon.svg
```

Library files are plain scripts that attach one object each to
`globalThis.Unrot`. That keeps the extension loadable unpacked with no build
step, lets the popup and options page include them with script tags, lets the
service worker use `importScripts`, and lets Node tests `require` them.

### 5.1 Hiding pipeline

`hide.css` is injected at `document_start`. Every rule is scoped to an
attribute on the root element, for example
`html[data-unrot-home-feed] ytd-browse[page-subtype="home"] #contents`.
Shorts rules carry no attribute and always apply. `content.js` reads settings
from storage at `document_start` and sets the attributes before YouTube renders.
It listens to `chrome.storage.onChanged` so popup changes apply to open tabs
without reload.

### 5.2 Navigation

YouTube is a single-page app. `content.js` listens to `yt-navigate-start` and
`yt-navigate-finish` on `document` plus `popstate`, and re-runs URL rules:

- `/shorts/ID` -> `location.replace('/watch?v=ID')` (belt and braces beside
  the DNR rule, which only covers full navigations).
- `/` when homeFeed and redirectHome are on -> `/feed/subscriptions`.

### 5.3 Autoplay

On every navigate-finish on a watch page, poll briefly for
`.ytp-autonav-toggle-button` and click it if `aria-checked="true"`.

### 5.4 Educational gate

`page-bridge.js` runs in the main world. On load and on `yt-navigate-finish`
it resolves the current watch page's player response from, in order:
the navigate event detail, `ytd-page-manager.getCurrentData()`,
`window.ytInitialPlayerResponse` (only when its videoId matches the URL), and
finally a `fetch` of the watch page HTML parsed with a regex. It dispatches a
`unrot:video` CustomEvent on `document` with
`{videoId, title, category, channelId, channelName}` serialised as a JSON
string in `detail`.

`content.js` receives it, evaluates `Unrot.education.decide(meta, settings)`,
and when the decision is block: pauses the `<video>`, keeps it paused on any
`play` event, and inserts a full-viewport overlay in the theme with the title,
the category, and two links (Back, Search). The overlay is removed on the next
navigation and on settings change.

### 5.5 Friction timer

The countdown runs inside the popup only. State machine (`lib/countdown.js`):
idle -> counting(remaining) -> done. The popup calls `tick()` every 250 ms.
When done it writes `focus.enabled = false`. If the popup unloads for any
reason the countdown is simply gone, which is the required behaviour. When the
filter is turned on, the delay select value is written to
`focus.unlockDelaySec` and the select becomes disabled.

### 5.6 Storage

`chrome.storage.sync` holds one key `settings`. Version field enables
migrations. A settings write from the popup fans out to all tabs via
`storage.onChanged`.

## 6. Website

`site/` is a static page deployed by GitHub Actions to GitHub Pages. Sections:
hero (logo, name, tagline, Install button), how to install (Chrome Web Store
link when published, otherwise download the release zip and load unpacked),
what it does, the timer, educational mode, privacy (no network calls, no
analytics), source link. Same palette and drifting hand-drawn clouds as the
reference site, animated with CSS only, honouring prefers-reduced-motion.

## 7. Testing

- Unit: Node's built-in test runner over `extension/lib/*.js`.
- Manifest check: a script validates the manifest and that every attribute
  referenced in `hide.css` is produced by the feature registry.
- End to end: driven live through Chrome DevTools against www.youtube.com for
  every feature, the timer, and educational mode. Results recorded in
  docs/E2E.md with the date and the YouTube layout observed.

## 8. Error handling

- Storage read failure: fall back to defaults, log once with the `[unrot]`
  prefix.
- Player response unavailable: educational mode fails closed after the fetch
  fallback also fails (block with a message saying the category could not be
  determined) so that an unknown video cannot slip through.
- Selectors that no longer match: nothing breaks, the element is just visible.
  The E2E checklist is the detection mechanism.

## 9. Release process

- Semantic versions. `manifest.json` version is the source of truth.
- `npm run build` writes `dist/unrot-<version>.zip`.
- Pushing a tag `v*` runs the release workflow, which builds the zip and
  attaches it to a GitHub release.
