# Anti-Brainrot design spec

Date: 2026-09-20
Status: approved (autonomous build, decisions recorded here)

## 1. Purpose

Anti-Brainrot is a Chrome (Manifest V3) extension that replaces three extensions the
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

- Product name: Anti-Brainrot (wordmark: anti-brainrot)
- Tagline: The anti brain rot extension for Chrome. (Changed 2026-09-20 from a YouTube-only line because the site blocker works everywhere.)
- Repository: github.com/danieltyukov/anti-brainrot (public, MIT)
- Website: https://danieltyukov.github.io/anti-brainrot/
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

## 4b. Adult site blocker

Added 2026-09-20 on request. Feature id `adultSites`, popup label
"Block adult sites", shown in a separate "Everywhere" group under the same
filter switch, so turning it off goes through the countdown like everything
else. Default off, because it needs a host permission grant that Chrome only
allows from a user click.

Mechanism: a static declarativeNetRequest ruleset `adult`
(`extension/rules/adult.json`) that redirects main-frame requests to the
extension's block page for:

- a bundled domain list (`requestDomains`) generated by
  `scripts/build-adult-list.mjs` from public blocklists (Sinfonietta hostfiles
  pornography list and blocklistproject porn list, both permissively licensed)
  intersected with the Tranco top 1M list and merged with a hand-curated core
  list, deduplicated and pruned to registrable domains. The generated file is
  committed so builds are reproducible and reviewable.
- hostname keyword regex rules for unambiguous terms (porn, xxx, xvideos,
  xnxx, xhamster, hentai, redtube, youporn, brazzers, chaturbate, stripchat,
  livejasmin, bongacams, onlyfans, fansly, rule34, spankbang, eporner,
  camsoda, myfreecams, erome, motherless, fapello, thothub). Short generic
  words such as sex, nude or adult are not used because of false positives
  (sussex, essex, adulteducation).

The background worker enables the ruleset with
`chrome.declarativeNetRequest.updateEnabledRulesets` when
`isActive(settings, 'adultSites')` and disables it otherwise.

Host permission: `<all_urls>` is declared under `optional_host_permissions`
and requested from the popup when the toggle is switched on. If the user
declines, the toggle stays off.

Custom lists (options page): `blocker.blockedDomains` (user additions,
applied as dynamic redirect rules) and `blocker.allowedDomains` (dynamic
allow rules with higher priority, for false positives). Adding a blocked
domain is allowed at any time. Removing a blocked domain or changing the allow
list is only allowed while the filter is off, otherwise it would be a way
around the timer.

Block page: `extension/blocked/blocked.html` in the site theme. Shows
"Blocked by Anti-Brainrot", the blocked hostname (from the `u` query
parameter that the regex substitution appends), one line explaining that the
filter and its timer control this, and a Back button.

Pinned extension id: `manifest.json` carries a `key` so the id is
`ibcicobbbpfmonjbhpmllnjgdkedneop` for every install. The redirect rules need
the absolute chrome-extension:// URL of the block page, which is only stable
with a pinned id. The private key lives outside the repository.

## 4d. Distracting sites (added 2026-09-20, v1.1.0)

Feature id `distractions`, label "Block distracting sites", section
"Everywhere", default off (same optional permission as the adult blocker).

Patterns: `host` (host and subdomains, all paths), `host/` (front page
only), `host/path` (path prefix). Presets in `lib/distractions.js` cover
the brain rot surfaces of the major platforms with feed-only and whole-site
variants. `distractions.custom` adds patterns, `distractions.exceptions`
keeps parts open (priority 3 allow rules, checked first by the watcher).

Modes: `block` redirects to the block page. `pause` redirects to the pause
view: a countdown of `pauseSeconds` that restarts when the tab is hidden, an
optional intention line, then a pass of `passMinutes` charged against
`dailyBudgetMinutes`, followed by a cooldown of `cooldownMinutes` for that
pattern. Passes are session allow rules (priority 4) plus an entry in
`chrome.storage.local`, revoked by an alarm. The worker serialises pass
grants so two tabs cannot double spend.

Enforcement: dynamic redirect rules (ids 3000 to 3499), exception rules
(3500 to 3999), session pass rules (4000 and up). Two content scripts are
registered with `chrome.scripting` only for the enabled hosts: a main-world
hook that raises a DOM event on `history.pushState` and `replaceState`, and
an isolated watcher that redirects on in-app navigation, applies grayscale
(during a pass, or always when `grayscaleAlways`), warns 30 seconds before a
pass ends and sends the tab back to the pause page when it expires. The
block page is web accessible because a content-script navigation to it is
web-initiated.

Loosening: block to pause, removing a preset or custom pattern, adding an
exception, a longer pass, a shorter pause, a bigger budget, a shorter
cooldown, switching off grayscale or the intention line.

## 4e. Locked hours and locks (added 2026-09-20, v1.1.0)

Feature id `schedule`, label "Locked hours". `schedule.days`, `start`,
`end`. While inside the window the worker forces `focus.enabled` on (on
every settings change and once a minute by alarm) and `settings.update`
refuses turning it off with a LockedError naming the end time. The popup
disables the power button. `focus.lockUntil` is an ad hoc lock set from the
popup ("Lock for N hours") with the same effect; extending a lock is
tightening, shortening it is loosening. Enforcement writes use
`settings.patch`, which shares the update queue but skips the guards.

## 4f. More YouTube options (added 2026-09-20, v1.1.0)

`thumbnails` (with child `thumbnailsBlur`), `metrics`, `chips`,
`richSections`, `searchSuggestions`, `grayscale`. All CSS, keyed like the
rest. `chips` and `richSections` default on.

## 4g. Nudges (added 2026-09-20, v1.1.0)

`focus.reason` is a line the user writes once; it is shown on every block
and pause page and in the popup. `chrome.storage.local.stats` counts block
page loads and passes for the current day and is shown in the popup.

## 4c. Tighten any time, loosen only while off

Added 2026-09-20. A friction timer on the master switch alone would be
pointless if every individual toggle could be flipped off instantly. So one
rule applies everywhere (popup, options, import, reset): while the filter is
on, any change that restricts more is applied immediately, and any change that
restricts less is refused until the filter is off. Turning the filter off is
what the countdown protects.

"Restricts less" means: turning a feature off, shortening the unlock delay,
adding an allowed category or channel while educational mode is on, removing a
blocked domain or adding an allowed domain while the site blocker is on.
Theme changes are always free. `settings.isLoosening(current, next)` is the
single implementation and `settings.update()` enforces it by throwing
`LockedError`.

## 5. Architecture

```
extension/
  manifest.json           MV3; permissions: storage, declarativeNetRequest;
                          host_permissions: *://www.youtube.com/*, *://m.youtube.com/*
  background.js           installs defaults on first run, migrates settings
  rules/shorts.json       static DNR redirect: /shorts/ID -> /watch?v=ID
  rules/adult.json        static DNR redirect of adult sites to blocked/blocked.html
  lib/blocker.js          pure domain list parsing and dynamic rule builders
  lib/distractions.js     presets, pattern grammar, matching, rule builders, passes
  content/distractions.js         watcher for enabled distracting hosts (isolated world)
  content/distractions-main.js    history hook for the watcher (main world)
  blocked/                themed block page with adult, block and pause views
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
`globalThis.AntiBrainrot`. That keeps the extension loadable unpacked with no build
step, lets the popup and options page include them with script tags, lets the
service worker use `importScripts`, and lets Node tests `require` them.

### 5.1 Hiding pipeline

`hide.css` is injected at `document_start`. Every rule is scoped to an
attribute on the root element, for example
`html[data-abr-home-feed] ytd-browse[page-subtype="home"] #contents`.
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
`abr:video` CustomEvent on `document` with
`{videoId, title, category, channelId, channelName}` serialised as a JSON
string in `detail`.

`content.js` receives it, evaluates `Anti-Brainrot.education.decide(meta, settings)`,
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

- Storage read failure: fall back to defaults, log once with the `[anti-brainrot]`
  prefix.
- Player response unavailable: educational mode fails closed after the fetch
  fallback also fails (block with a message saying the category could not be
  determined) so that an unknown video cannot slip through.
- Selectors that no longer match: nothing breaks, the element is just visible.
  The E2E checklist is the detection mechanism.

## 9. Release process

- Semantic versions. `manifest.json` version is the source of truth.
- `npm run build` writes `dist/anti-brainrot-<version>.zip`.
- Pushing a tag `v*` runs the release workflow, which builds the zip and
  attaches it to a GitHub release.
