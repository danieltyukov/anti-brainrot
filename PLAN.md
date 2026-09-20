# Unrot implementation plan

> For agentic workers: this is the working plan for the whole project. Read it
> before doing anything. Read the spec at
> `docs/specs/2026-09-20-unrot-design.md` for the reasoning behind every
> decision. Tick boxes as you finish steps. Do not change scope without
> writing the change here first.

**Goal:** Ship Unrot, an MV3 Chrome extension that permanently hides YouTube
Shorts, hides feeds and distractions behind a friction timer, and can restrict
playback to educational videos, with a public repo, CI, releases and a
one-page install site.

**Architecture:** No build step. Plain JS library files attach to
`globalThis.Unrot` and are shared by the popup, options page, service worker
and content scripts. All hiding is CSS keyed by `data-unrot-*` attributes on
`<html>` set at `document_start`. Shorts redirect is a static
declarativeNetRequest rule plus an SPA fallback. Educational gating uses a
main-world bridge script that reads YouTube's player response.

**Tech stack:** Chrome MV3, vanilla JS, CSS with `:has()`, Node 22 built-in
test runner, GitHub Actions, GitHub Pages, rsvg-convert for icons.

**Spec:** `docs/specs/2026-09-20-unrot-design.md`

## Global constraints

- Chrome 111 minimum (`minimum_chrome_version: "111"`).
- No dependencies at runtime. Dev dependencies only if unavoidable (none so far).
- No emojis anywhere. No em dashes or en dashes in prose. Plain, direct writing.
- Commit messages: conventional style (`feat:`, `fix:`, `docs:`, `chore:`,
  `test:`, `ci:`). No AI attribution lines, no session links, ever.
- Git identity: `danieltyukov <60662998+danieltyukov@users.noreply.github.com>`.
- Push to `main` after every task. Tag releases `vX.Y.Z` at milestones.
- Load the extension for testing only through `chrome-ext load extension`
  (see `chrome-ext --help`) or the chrome-devtools-ext MCP `install_extension`
  tool pointed at `extension/`. Never the user's main Chrome.
- Shorts hiding is unconditional. It must never depend on a setting.
- Everything the popup shows must work with the popup at 360 px wide.
- Log lines use the `[unrot]` prefix and only for real problems.

## File map

```
extension/manifest.json          MV3 manifest, version is the release version
extension/background.js          importScripts libs; seeds settings; badge text
extension/rules/shorts.json      DNR redirect /shorts/ID -> /watch?v=ID
extension/lib/features.js        Unrot.features: FEATURES registry + helpers
extension/lib/settings.js        Unrot.settings: defaults, normalize, load/save, isActive, activeAttributes
extension/lib/shorts.js          Unrot.shorts: URL helpers
extension/lib/education.js       Unrot.education: categories, channel parsing, decide()
extension/lib/countdown.js       Unrot.countdown: pure state machine + formatting
extension/content/hide.css       attribute-keyed hiding rules (+ unconditional shorts rules)
extension/content/content.js     isolated-world runtime
extension/content/page-bridge.js main-world metadata bridge
extension/popup/popup.html|css|js
extension/options/options.html|css|js
extension/icons/icon-{16,32,48,128}.png
assets/logo.svg, assets/icon.svg, assets/wordmark.svg
site/index.html, site/style.css, site/assets/*
scripts/build.sh                 zips extension/ to dist/unrot-<version>.zip
scripts/render-icons.sh          assets/icon.svg -> extension/icons/*.png
scripts/check.mjs                manifest + css attribute + version consistency checks
test/*.test.js                   node --test
.github/workflows/ci.yml, release.yml, pages.yml
README.md, CONTRIBUTING.md, CHANGELOG.md, PRIVACY.md, LICENSE, docs/E2E.md
```

## Shared interfaces (exact names)

```js
// lib/features.js
Unrot.features.FEATURES // Array<{id, label, default, attr|null, parent?, mode?: 'when-parent-off'|'when-parent-on', locked?: true}>
Unrot.features.byId(id) // feature or undefined
Unrot.features.roots()  // features without parent, in display order
Unrot.features.children(parentId) // features whose parent === parentId

// lib/settings.js
Unrot.settings.VERSION            // 1
Unrot.settings.DELAY_CHOICES      // [0, 30, 60, 300, 600, 1800, 3600]
Unrot.settings.defaults()         // fresh Settings object
Unrot.settings.normalize(raw)     // Settings (merges with defaults, coerces, clamps)
Unrot.settings.load()             // Promise<Settings> from chrome.storage.sync key 'settings'
Unrot.settings.save(settings)     // Promise<void>
Unrot.settings.update(patch)      // Promise<Settings>, deep merge one level per section
Unrot.settings.onChange(cb)       // cb(newSettings) on storage change of 'settings'
Unrot.settings.isActive(settings, featureId) // boolean, false when filter off
Unrot.settings.activeAttributes(settings)    // string[] of attr names (without data-unrot- prefix)

// Settings shape
{
  version: 1,
  focus: { enabled: true, unlockDelaySec: 300 },
  theme: 'system' | 'light' | 'dark',
  features: { [featureId]: boolean },
  educational: { allowedCategories: string[], allowedChannels: string[] }
}

// lib/shorts.js
Unrot.shorts.videoIdFromPath(pathname) // 'abc' | null
Unrot.shorts.rewriteUrl(href)          // 'https://www.youtube.com/watch?v=abc' | null

// lib/education.js
Unrot.education.CATEGORIES        // 15 YouTube category names
Unrot.education.DEFAULT_ALLOWED   // ['Education', 'Science & Technology', 'Howto & Style']
Unrot.education.normalizeChannel(entry)   // lowercased key without '@', URL prefixes, whitespace
Unrot.education.parseChannelList(text)    // string[] from newline/comma separated text
Unrot.education.decide(meta, educational) // { allow: boolean, reason: 'channel'|'category'|'blocked'|'unknown' }
// meta = { videoId, title, category, channelId, channelName, channelHandle }

// lib/countdown.js
Unrot.countdown.start(totalMs, now)  // { status: 'counting', totalMs, startedAt: now, remainingMs: totalMs }
Unrot.countdown.tick(state, now)     // new state; status becomes 'done' at 0
Unrot.countdown.format(ms)           // '4:59', '0:07', '1:00:00'
Unrot.countdown.delayLabel(seconds)  // 'Instant', '30 seconds', '1 minute', '5 minutes', '10 minutes', '30 minutes', '1 hour'

// DOM events between worlds (detail is always a JSON string)
'unrot:video'          main -> isolated, detail = JSON of meta above
'unrot:request-video'  isolated -> main, asks the bridge to re-emit for the current page
```

## Feature registry (source of truth for popup order and CSS attributes)

| id | label | default | attr | parent | mode |
| --- | --- | --- | --- | --- | --- |
| homeFeed | Hide Home Feed | true | home-feed | | |
| redirectHome | Redirect to Subscriptions | true | null | homeFeed | when-parent-on |
| sidebar | Hide Video Sidebar | false | sidebar | | |
| sidebarRecommended | Hide Recommended | true | sidebar-recommended | sidebar | when-parent-off |
| liveChat | Hide Live Chat | true | live-chat | sidebar | when-parent-off |
| playlist | Hide Playlist | false | playlist | sidebar | when-parent-off |
| fundraiser | Hide Fundraiser | true | fundraiser | sidebar | when-parent-off |
| endScreenFeed | Hide End Screen Feed | true | end-screen-feed | | |
| endScreenCards | Hide End Screen Cards | true | end-screen-cards | | |
| shorts | Hide Shorts | true | null (unconditional CSS) | | locked |
| comments | Hide Comments | true | comments | | |
| mixes | Hide Mixes | true | mixes | | |
| merch | Hide Merch, Tickets, Offers | true | merch | | |
| videoInfo | Hide Video Info | false | video-info | | |
| topHeader | Hide Top Header | false | top-header | | |
| notifications | Hide Notifications | true | notifications | topHeader | when-parent-off |
| inaptSearch | Hide Inapt Search Results | true | inapt-search | | |
| explore | Hide Explore, Trending | true | explore | | |
| moreFromYouTube | Hide More from YouTube | true | more-from-youtube | | |
| subscriptions | Hide Subscriptions | false | subscriptions | | |
| autoplay | Disable Autoplay | true | null (JS) | | |
| annotations | Disable Annotations | true | annotations | | |
| educational | Educational videos only | false | educational | | |

---

## Task 1: Repository scaffold and remote

**Files:** `package.json`, `.gitignore`, `.editorconfig`, `LICENSE`,
`README.md` (stub), `CLAUDE.md`, `PLAN.md`, `docs/specs/...`

- [x] Step 1: package.json with `test`, `check`, `build`, `icons` scripts and no dependencies.
- [x] Step 2: git init on `main`, verify `git config user.email` is the global noreply address.
- [x] Step 3: `gh repo create danieltyukov/yt-anti-brain-rot --public --source . --push`.
- [x] Step 4: commit `chore: scaffold repository`.

## Task 2: Feature registry and settings library (TDD)

**Files:** `extension/lib/features.js`, `extension/lib/settings.js`,
`test/features.test.js`, `test/settings.test.js`, `test/helpers/chrome-mock.js`

- [x] Step 1: write `test/features.test.js`: every id unique; every parent exists; `roots()` preserves table order; `children('sidebar')` returns the four children in order; `shorts` is `locked`.
- [x] Step 2: run `node --test` and see it fail (module missing).
- [x] Step 3: implement `features.js` from the table above.
- [x] Step 4: write `test/settings.test.js` using a `chrome.storage.sync` mock (in-memory object with `get`, `set`, `onChanged.addListener`):
  - `defaults()` matches the table defaults, `focus.unlockDelaySec === 300`, `educational.allowedCategories` equals `DEFAULT_ALLOWED`.
  - `normalize({})` equals `defaults()`; `normalize({features:{mixes:'no'}})` ignores non-boolean values (keeps default); unknown feature keys are dropped; `unlockDelaySec` not in `DELAY_CHOICES` falls back to 300; `theme: 'purple'` falls back to `'system'`.
  - `isActive`: filter off gives false for everything; `sidebarRecommended` false when `sidebar` true; `redirectHome` false when `homeFeed` false; `shorts` is always... not part of isActive (locked features return true while filter on, but CSS does not depend on it).
  - `activeAttributes(defaults())` equals the exact expected array in table order.
  - `load()` on empty storage returns defaults; `save()` then `load()` round trips; `update({focus:{enabled:false}})` keeps `unlockDelaySec`.
- [x] Step 5: implement `settings.js`. `load` must not throw when `chrome` is undefined (returns defaults, logs once).
- [x] Step 6: `node --test` passes. Commit `feat: add feature registry and settings library`.

## Task 3: Shorts redirect, manifest, background, first load in Chrome

**Files:** `extension/lib/shorts.js`, `test/shorts.test.js`,
`extension/rules/shorts.json`, `extension/manifest.json`,
`extension/background.js`, placeholder `extension/icons/*.png`

- [x] Step 1: tests for `videoIdFromPath('/shorts/AbC-12_')` -> `'AbC-12_'`, `'/shorts/'` -> null, `'/watch'` -> null; `rewriteUrl('https://www.youtube.com/shorts/x1?feature=share')` -> `'https://www.youtube.com/watch?v=x1'`; `rewriteUrl('https://m.youtube.com/shorts/x1')` -> `'https://m.youtube.com/watch?v=x1'`; non-shorts -> null.
- [x] Step 2: implement, tests pass.
- [x] Step 3: `rules/shorts.json` single rule: regexFilter `^https?://(www\.|m\.)?youtube\.com/shorts/([A-Za-z0-9_-]+)`, regexSubstitution `https://\1youtube.com/watch?v=\2`, resourceTypes `["main_frame"]`. Note: the empty group for `www.` must still produce a valid host, so use two rules instead if RE2 substitution of an unmatched optional group is a problem (test it live).
- [x] Step 4: manifest per spec section 5, version `0.1.0`, temporary icons generated with rsvg-convert from a simple placeholder SVG.
- [x] Step 5: `background.js`: `importScripts('lib/features.js','lib/settings.js')`; on `onInstalled` normalize and save; on storage change and on startup set badge text `OFF` (grey) when filter off, empty when on.
- [x] Step 6: `chrome-ext load extension` (from repo root) and open `https://www.youtube.com/shorts/<any id>`; confirm the URL becomes `/watch?v=`. Check the service worker console has no errors.
- [x] Step 7: Commit `feat: manifest, background worker and shorts redirect rule`, tag nothing yet.

## Task 4: Hiding CSS and content runtime

**Files:** `extension/content/hide.css`, `extension/content/content.js`

- [ ] Step 1: `hide.css`. Group rules by feature with a comment header per feature. Unconditional Shorts block first. Use `display: none !important`. Selectors to start from (verify live and adjust):
  - Shorts: `ytd-guide-entry-renderer:has(a[title="Shorts"])`, `ytd-mini-guide-entry-renderer[aria-label="Shorts"]`, `ytd-rich-shelf-renderer[is-shorts]`, `ytd-rich-section-renderer:has(ytd-rich-shelf-renderer[is-shorts])`, `ytd-reel-shelf-renderer`, `ytd-item-section-renderer:has(> #contents > ytd-reel-shelf-renderer:only-child)`, `grid-shelf-view-model`, `ytd-rich-item-renderer:has(ytm-shorts-lockup-view-model, ytm-shorts-lockup-view-model-v2, a[href^="/shorts/"])`, `ytd-video-renderer:has(a[href^="/shorts/"])`, `ytd-grid-video-renderer:has(a[href^="/shorts/"])`, `ytd-reel-item-renderer`, `ytd-notification-renderer:has(a[href*="/shorts/"])`, `yt-tab-shape[tab-title="Shorts"]`, `tp-yt-paper-tab:has(> .tab-content:is([title="Shorts"]))`, `ytd-compact-video-renderer:has(a[href^="/shorts/"])`, `yt-lockup-view-model:has(a[href^="/shorts/"])`.
  - Home feed: `html[data-unrot-home-feed] ytd-browse[page-subtype="home"] #primary > ytd-rich-grid-renderer`, `... ytd-feed-filter-chip-bar-renderer`.
  - Sidebar: `html[data-unrot-sidebar] ytd-watch-flexy #secondary`.
  - Sidebar recommended: `html[data-unrot-sidebar-recommended] ytd-watch-next-secondary-results-renderer`.
  - Live chat: `html[data-unrot-live-chat] #chat, ytd-live-chat-frame`.
  - Playlist: `html[data-unrot-playlist] ytd-playlist-panel-renderer#playlist`.
  - Fundraiser: `html[data-unrot-fundraiser] ytd-donation-shelf-renderer`.
  - End screen feed: `html[data-unrot-end-screen-feed] .ytp-endscreen-content, .html5-endscreen.ytp-player-content`.
  - End screen cards: `html[data-unrot-end-screen-cards] .ytp-ce-element`.
  - Comments: `html[data-unrot-comments] ytd-comments#comments`.
  - Mixes: `html[data-unrot-mixes] ytd-radio-renderer, ytd-compact-radio-renderer, ytd-rich-item-renderer:has(a[href*="list=RD"]), yt-lockup-view-model:has(a[href*="list=RD"])`.
  - Merch: `html[data-unrot-merch] ytd-merch-shelf-renderer, #ticket-shelf, ytd-ticket-shelf-renderer, ytd-product-details-renderer, #offer-module`.
  - Video info: `html[data-unrot-video-info] ytd-watch-metadata #bottom-row, #description` (keep title and channel visible).
  - Top header: `html[data-unrot-top-header] #masthead-container` plus `ytd-app #page-manager { margin-top: 0 !important }` and `html[data-unrot-top-header] ytd-app { --ytd-masthead-height: 0px }`.
  - Notifications: `html[data-unrot-notifications] ytd-notification-topbar-button-renderer`.
  - Inapt search: `html[data-unrot-inapt-search] ytd-search ytd-shelf-renderer, ytd-search ytd-horizontal-card-list-renderer, ytd-search ytd-secondary-search-container-renderer, ytd-search ytd-promoted-sparkles-web-renderer, ytd-search ytd-ad-slot-renderer`.
  - Explore: `html[data-unrot-explore] ytd-guide-section-renderer:has(a[href="/feed/trending"]), ytd-mini-guide-entry-renderer[aria-label="Explore"]`.
  - More from YouTube: `html[data-unrot-more-from-youtube] ytd-guide-section-renderer:has(a[href="/premium"])`.
  - Subscriptions: `html[data-unrot-subscriptions] ytd-guide-section-renderer:has(a[href="/feed/channels"]), ytd-guide-entry-renderer:has(a[href="/feed/subscriptions"]), ytd-mini-guide-entry-renderer[aria-label="Subscriptions"]`.
  - Annotations: `html[data-unrot-annotations] .ytp-cards-teaser, .ytp-cards-button, .iv-branding, .ytp-paid-content-overlay`.
- [ ] Step 2: `content.js` per spec 5.1 to 5.3. Structure:
  ```js
  (() => {
    const { settings: S, shorts } = globalThis.Unrot;
    const root = document.documentElement;
    let settings = null;
    function applyAttributes() { /* remove all data-unrot-* then set activeAttributes */ }
    function redirectIfNeeded() { /* shorts -> watch; home -> subscriptions when isActive(redirectHome) */ }
    function fixAutoplay() { /* poll up to 5s for .ytp-autonav-toggle-button[aria-checked="true"], click once */ }
    function onNavigate() { redirectIfNeeded(); }
    function onNavigateFinish() { if (S.isActive(settings, 'autoplay')) fixAutoplay(); }
    // boot
    redirectIfNeeded(); // shorts only, settings not loaded yet
    S.load().then(s => { settings = s; applyAttributes(); redirectIfNeeded(); });
    S.onChange(s => { settings = s; applyAttributes(); redirectIfNeeded(); });
    document.addEventListener('yt-navigate-start', onNavigate);
    document.addEventListener('yt-navigate-finish', () => { onNavigate(); onNavigateFinish(); });
    window.addEventListener('popstate', onNavigate);
  })();
  ```
- [ ] Step 3: Load in Chrome. Walk the E2E list in `docs/E2E.md` for every CSS feature, toggling via the popup once it exists (until then via `chrome-ext eval <id> "chrome.storage.sync.set(...)"`). Fix selectors as needed. Record findings in `docs/E2E.md`.
- [ ] Step 4: Commit `feat: hiding rules and content runtime`, tag `v0.1.0`, push tag.

## Task 5: Countdown library and popup

**Files:** `extension/lib/countdown.js`, `test/countdown.test.js`,
`extension/popup/popup.html`, `popup.css`, `popup.js`

- [ ] Step 1: tests: `start(5000, 1000)` -> remaining 5000; `tick(state, 3500)` -> 2500 counting; `tick(state, 6000)` -> 0 done; `format(299000)` -> `'4:59'`; `format(7000)` -> `'0:07'`; `format(3600000)` -> `'1:00:00'`; `delayLabel(0)` -> `'Instant'`, `delayLabel(300)` -> `'5 minutes'`, `delayLabel(3600)` -> `'1 hour'`.
- [ ] Step 2: implement, tests pass, commit `feat: countdown state machine`.
- [ ] Step 3: popup. Layout 360 px wide, max height 560 px, scrollable list.
  - Header: icon, "Unrot", theme button (cycles system, light, dark), power button.
  - Filter-off panel: text "Filter is off", select of `DELAY_CHOICES` labelled by `delayLabel`, primary button "Turn on". Selecting writes `unlockDelaySec`; button writes `focus.enabled=true`.
  - Filter-on: list of toggles rendered from `Unrot.features.roots()` with nested children. Shorts row: checked, disabled, hint "always on". Each change calls `S.update({features:{[id]:checked}})`.
  - Power button while on: if `unlockDelaySec === 0` turn off immediately, else show countdown panel: big time, "Keep this popup open. Closing it cancels.", Cancel button. Uses `setInterval` 250 ms and `Unrot.countdown`. On done: `S.update({focus:{enabled:false}})`.
  - Footer links: Options (`chrome.runtime.openOptionsPage()`), GitHub, Report issue.
  - Theme: `data-theme` on `<html>`; `system` uses `prefers-color-scheme`.
  - Colours: light bg #e9f4fd, surface #ffffff, ink #1a1a2e, muted #2c3e50, accent #3d8fd1; dark bg #0a1220, surface #16213a, ink #eceade, accent #73b8ee.
- [ ] Step 4: E2E in Chrome using `trigger_extension_action` or by opening `chrome-extension://<id>/popup/popup.html` in a tab: toggles persist and apply to an open YouTube tab without reload; countdown runs; closing the tab mid-countdown leaves the filter on; countdown completion turns it off; delay select is disabled while on.
- [ ] Step 5: Commit `feat: popup with toggles and friction timer`, tag `v0.2.0`.

## Task 6: Educational mode

**Files:** `extension/lib/education.js`, `test/education.test.js`,
`extension/content/page-bridge.js`, changes to `content.js` and `hide.css`

- [ ] Step 1: tests: `normalizeChannel('@Veritasium ')` -> `'veritasium'`; `normalizeChannel('https://www.youtube.com/@3blue1brown')` -> `'3blue1brown'`; `normalizeChannel('UCabc')` -> `'ucabc'`; `parseChannelList('a\nb, c')` -> `['a','b','c']`; `decide({category:'Education'}, {allowedCategories:['Education'], allowedChannels:[]})` allow/category; blocked category with allowlisted channel handle -> allow/channel; missing category -> `{allow:false, reason:'unknown'}`; case-insensitive category match.
- [ ] Step 2: implement, tests pass, commit `feat: educational policy`.
- [ ] Step 3: `page-bridge.js` per spec 5.4. Resolution order: navigate event detail (`event.detail?.response?.playerResponse` or `event.detail?.pageData?.playerResponse`), `document.querySelector('ytd-page-manager')?.getCurrentData?.()?.playerResponse`, `window.ytInitialPlayerResponse` when `videoDetails.videoId` matches the URL, else `fetch(location.href, {credentials:'include'})` and regex `"category":"([^"]*)"` and `"ownerChannelName":"([^"]*)"`, `"channelId":"([^"]*)"`, `"title":"..."`. Owner handle from `ytInitialData` is optional; include `channelHandle` when `document.querySelector('ytd-watch-metadata #owner a[href^="/@"]')` exists (isolated side can read it too).
- [ ] Step 4: `content.js`: on `unrot:video`, if `isActive(settings,'educational')`, decide, and block or unblock. Overlay id `unrot-block`, `position:fixed; inset:0; z-index:2147483647`, theme colours, content: heading "Not on your list", body "This video is filed under <category>. Educational mode only plays: <allowed list>.", buttons "Back" and "Search". Pause the video and re-pause on `play` while blocked. Remove overlay on `yt-navigate-start`.
- [ ] Step 5: E2E: open an Education video (allowed), an Entertainment video (blocked), navigate between them via search without reload, toggle the feature in the popup while on a blocked page (overlay disappears), reload on blocked page (overlay returns).
- [ ] Step 6: Commit `feat: educational videos only mode`, tag `v0.3.0`.

## Task 7: Options page

**Files:** `extension/options/options.html|css|js`

- [ ] Step 1: Sections: Educational mode (enable toggle, category checkboxes from `CATEGORIES`, channel textarea), Filter (delay select disabled while on with hint), Appearance (theme radios), Data (Export JSON download, Import JSON file input, Reset with confirm via inline two-step button, not `window.confirm`). All changes save immediately and show a small "Saved" note.
- [ ] Step 2: E2E: change categories and verify a watch page decision changes without reload; import a file; reset.
- [ ] Step 3: Commit `feat: options page`.

## Task 8: Logo and icons

**Files:** `assets/logo.svg`, `assets/icon.svg`, `assets/wordmark.svg`,
`extension/icons/*.png`, `scripts/render-icons.sh`

- [ ] Step 1: draw the leaf-as-play-triangle mark by hand as SVG paths, slightly irregular edges, one midrib stroke, navy #1a1a2e on sky #73b8ee rounded square (icon) and on transparent (logo).
- [ ] Step 2: render with `rsvg-convert -w N` for 16, 32, 48, 128 and view each. At 16 px the mark must still read as a leaf/play shape: simplify strokes for the small sizes if needed (a second `icon-small.svg` is acceptable).
- [ ] Step 3: load in Chrome, screenshot the toolbar and `chrome://extensions` card, iterate until clean.
- [ ] Step 4: Commit `feat: logo and icons`.

## Task 9: Website

**Files:** `site/index.html`, `site/style.css`, `site/assets/*`,
`.github/workflows/pages.yml`

- [ ] Step 1: static page in the reference theme: sky gradient, drifting hand-drawn clouds (inline SVG, CSS keyframes, paused under `prefers-reduced-motion`), Barlow from Google Fonts with the Bahnschrift stack first, sections per spec 6. Install section links to the latest GitHub release zip and explains load unpacked in five steps, plus a Chrome Web Store placeholder marked "coming soon".
- [ ] Step 2: screenshots of the popup and a blocked page (taken in Chrome, saved to `site/assets/`).
- [ ] Step 3: `pages.yml` deploys `site/` with `actions/upload-pages-artifact` and `actions/deploy-pages`. Enable Pages with `gh api -X POST repos/danieltyukov/yt-anti-brain-rot/pages -f build_type=workflow`.
- [ ] Step 4: verify the live URL renders, commit `feat: install website`.

## Task 10: CI, release packaging, docs, v1.0.0

**Files:** `scripts/build.sh`, `scripts/check.mjs`, `.github/workflows/ci.yml`,
`.github/workflows/release.yml`, `README.md`, `CONTRIBUTING.md`,
`CHANGELOG.md`, `PRIVACY.md`, `docs/E2E.md`, `docs/ARCHITECTURE.md`

- [ ] Step 1: `check.mjs`: manifest parses, `manifest.version === package.json version`, every `data-unrot-<attr>` in `hide.css` exists in the registry and vice versa (except JS-only), CHANGELOG has a heading for the version.
- [ ] Step 2: `build.sh`: `zip -r dist/unrot-<v>.zip extension -x '*.DS_Store'`.
- [ ] Step 3: `ci.yml` on push and PR: `npm test`, `npm run check`, `npm run build`, upload zip artifact. `release.yml` on `v*` tags: build and `gh release create` with the zip and notes from CHANGELOG.
- [ ] Step 4: README: what it is, screenshot, install (store placeholder, release zip, load unpacked), features table, timer explanation, educational mode explanation and its limits, privacy, development (test, check, build, load), contributing, license.
- [ ] Step 5: Final full E2E pass, record in `docs/E2E.md`, bump to 1.0.0, commit `chore: release 1.0.0`, tag `v1.0.0`, push, verify the release workflow attached the zip.
