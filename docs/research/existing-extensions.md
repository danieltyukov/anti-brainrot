# Existing YouTube declutter extensions: technical research

Date of research: 2026-09-20.

This document covers three Chrome Web Store extensions in depth (Unhook 1.6.9, Remove YouTube Shorts 2.1.7, Youtube-shorts block 1.5.5) and then consolidates the YouTube desktop DOM selectors used by the best-maintained open-source projects into one table, with each selector marked as live-verified or source-only.

## Method

- The three CRX packages were downloaded from the Chrome Web Store update endpoint and unpacked, so every selector quoted for them comes from the shipped code, not from store descriptions. The endpoint is `https://clients2.google.com/service/update2/crx?response=redirect&prodversion=131.0.0.0&acceptformat=crx2,crx3&x=id%3D<EXTENSION_ID>%26uc`; a CRX3 file is a `Cr24` header followed by a plain zip.
- Remove YouTube Shorts ships webpack source maps with the original sources embedded, so its logic is quoted from `content.js.map`. Youtube-shorts block is MIT licensed on GitHub and the shipped bundle matches the repository. Unhook is closed source but not minified beyond readability.
- Open-source references: insin/control-panel-for-youtube v1.35.2 (`page.js`), gijsdev/ublock-hide-yt-shorts v2026.4.29 (`list.txt`), the tadwohlrapp uBlock Origin gist, lawrencehook/remove-youtube-suggestions v4.3.83 (`main.css`), ImprovedTube (code-charity/youtube), and uBlockOrigin/uAssets.
- Live verification: a logged-out dev Chrome profile (NL region) loaded www.youtube.com Home, Search (`minecraft`, `#shorts funny cat`, and the same with the Shorts filter chip active), Watch (`jNQXAC9IVRw` driven to its end screen, and `dQw4w9WgXcQ`), the `@MrBeast` channel page and its Shorts tab, a Shorts player page (`/shorts/90ab3E3Ek-M`), and the old `/feed/trending` and `/feed/explore` URLs. Candidate selectors were counted with `querySelectorAll`, and a census of custom element tag names was taken on each page.
- Caveat: logged-out Home renders only a "Your YouTube history is off" nudge and no feed, so Home-feed Shorts shelf selectors are confirmed from sources, not live. Notifications and Subscriptions surfaces also need a login and were not verified live.

## 1. Unhook - Remove YouTube Recommended & Shorts v1.6.9

### Listing

- Chrome Web Store id `khncfooichmfjbepaaaebmommgaepoid`, https://chromewebstore.google.com/detail/unhook-remove-youtube-rec/khncfooichmfjbepaaaebmommgaepoid
- Updated March 22, 2026. 1,000,000 users. 4.9 stars (4.5K ratings). 37.32 KiB. Developer contact removerecs@gmail.com. Site https://unhook.app/ (the welcome page at https://unhook.app/welcome has install and donation notes only; there is no changelog page).
- Closed source. The package contains a LICENSE.txt but no repository link.
- Also on Firefox (https://addons.mozilla.org/en-US/firefox/addon/youtube-recommended-videos/) and Edge (https://microsoftedge.microsoft.com/addons/detail/unhook-remove-youtube-r/hebpjnnclppdnfghdnmhgdljmjpfhggk). Mobile support is via Firefox Android on m.youtube.com or Kiwi Browser.
- Version history from the Firefox listing (https://addons.mozilla.org/en-US/firefox/addon/youtube-recommended-videos/versions/):
  - 1.6.9 (Mar 19, 2026): "Fix Hide Explore/Trending (which was incorrectly hiding 'You' section)", fixes for Hide End Screen Feed and for the new video controller buttons when autoplay is disabled.
  - 1.6.7 (Apr 12, 2024): stop hiding the Downloads page under Hide Home Feed; fix recommendations visibility in the updated watch UI.
  - 1.6.3 (Dec 18, 2023): annotations fix.
  - 1.6.2 (Jan 2, 2023): annotations and Hide Subscriptions fixes.
  - 1.6.1 (Apr 14, 2022): Shorts videos not hidden when Shorts Tab selected.
  - 1.6.0 (Apr 4, 2022): stop hiding the search sidebar under Hide Video Sidebar.

### Manifest

- Manifest V3. `permissions: ["storage", "webRequest"]`. `host_permissions: ["https://www.youtube.com/*", "https://m.youtube.com/*"]`.
- One content script: `css/content.css` and `js/content.js`, `run_at: document_start`, `all_frames: true`, matching www and m hosts.
- `web_accessible_resources: js/unhook-yt.js` (injected into the page main world).
- Background service worker `js/background.js`. `minimum_chrome_version: 88`. Popup `popup.html`.
- No declarativeNetRequest.

### Options (exact popup labels, storage key, default)

```
Hide Home Feed                      hide_feed          true
  Redirect to Subscriptions         hide_redirect_home false   (shown only when Home Feed is on and Subscriptions is not hidden)
Hide Video Sidebar                  hide_sidebar       true
  Hide Recommended                  hide_recommended   true
  Hide Live Chat                    hide_chat          true
  Hide Playlist                     hide_playlists     true
  Hide Fundraiser                   hide_donate        true
Hide End Screen Feed                hide_endscreen     true
Hide End Screen Cards               hide_cards         true
Hide Shorts                         hide_shorts        false
Hide Comments                       hide_comments      false
  Hide Profile Photos               hide_prof          false
Hide Mixes                          hide_mix           false
Hide Merch, Tickets, Offers         hide_merch         true
Hide Video Info                     hide_meta          false
  Hide Buttons Bar                  hide_bar           false
  Hide Channel                      hide_channel       false
  Hide Description                  hide_desc          false
Hide Top Header                     hide_header        false
  Hide Notifications                hide_notifs        true
Hide Inapt Search Results           hide_search        true    (store listing calls it "Hide Irrelevant Search Results")
Hide Explore, Trending              hide_trending      false
Hide More from YouTube              hide_moreyt        true
Hide Subscriptions                  hide_subs          false
Disable Autoplay                    hide_autoplay      true
Disable Annotations                 hide_annotations   true
Global on/off                       yt_on              true
Popup only: dark_mode, and tree collapse flags sidebar_tree, comment_tree, meta_tree, header_tree (popup_settings)
```

### How it works

- `content.js` (isolated world, document_start) reads `chrome.storage.sync` (falls back to `storage.local`) and writes each `hide_*` key as an attribute on the `<html>` element, for example `<html hide_feed="true">`. All hiding is pure CSS gated on those attributes, so there is no per-mutation JavaScript and little flash. When `yt_on` is false the attributes are removed. A `storage.onChanged` listener updates attributes live. Inside iframes it only runs if a `#player` element exists after DOMContentLoaded (embeds).
- It then injects `js/unhook-yt.js` into the page main world through a `<script>` tag for things CSS cannot do:
  - Disable Autoplay: clicks `.ytp-autonav-toggle-button` while its `aria-checked` is `true`, re-armed by a MutationObserver on `ytd-watch-flexy`. On mobile it clicks `.ytm-autonav-toggle-button-container` when `aria-pressed` is `true`.
  - Disable Annotations: opens the `.ytp-settings-button` menu twice, finds the last `.ytp-menuitem[role=menuitemcheckbox]` whose text is not "ambient mode", tags it with class `annOption` (which the CSS then hides) and clicks it off. Also works on channel trailers (`ytd-browse[page-subtype=channels] ytd-channel-video-player-renderer`) and embeds.
  - Hide Notifications: strips a leading `(N) ` from `document.title` with a MutationObserver on the `<title>` element.
  - Redirect to Subscriptions: rewrites `a#logo` to `https://www.youtube.com/feed/subscriptions` and adds capture-phase `click` and `touchend` handlers that stop propagation so YouTube's SPA router does not intercept it.
  - Hooks: `window` `load`, `yt-page-data-updated`, `state-navigateend` (mobile), and a MutationObserver on the `<html>` attributes. Desktop versus mobile is detected by the presence of `window.Polymer`.
- `background.js` registers a non-blocking `webRequest.onBeforeRequest` listener for `main_frame`, `sub_frame` and `xmlhttprequest` on `https://*.youtube.com/`, `/?*`, `/feed/trending*`, `/feed/explore*` and `/feed/subscriptions*`. When Hide Explore, Trending is on and the URL matches `/feed/(trending|explore)`, or Hide Subscriptions is on and the URL contains "subscriptions", it calls `chrome.tabs.update(tabId, {url: "https://www.youtube.com"})`. When Redirect to Subscriptions applies and the URL is the site root, it sends the tab to `/feed/subscriptions`. Redirects are therefore tab navigations issued after the request has started, not declarativeNetRequest rules. It opens `https://unhook.app/welcome` on install and sets `https://unhook.app/uninstall` as the uninstall URL.
- Unhook never rewrites `/shorts/ID` to `/watch?v=ID`. With Hide Shorts on, a Shorts page simply gets `#player-shorts-container` and `.ytd-shorts` hidden.

### Desktop selectors by option (from css/content.css; every rule is `display: none !important` unless noted)

```
always on:        #masthead-ad.ytd-rich-grid-renderer.style-scope, div#home-page-skeleton,
                  paper-dialog>ytd-single-option-survey-renderer[dialog][dialog][dialog],
                  ytd-mealbar-promo-renderer[dialog], ytd-primetime-promo-renderer
                  #content>#page-manager.ytd-app { overflow-y: hidden !important }
hide_feed:        ytd-browse[page-subtype=home] .ytd-rich-grid-renderer,
                  ytd-browse[role=main]:not([page-subtype]):not(:has(#header #page-header-container)):has(ytd-feed-filter-chip-bar-renderer) .ytd-rich-grid-renderer
hide_redirect_home (with hide_feed=true and hide_subs=false):
                  .yt-simple-endpoint[href="/"], .yt-simple-endpoint[href^="/index"]
hide_sidebar:     #secondary.ytd-watch-flexy
                  ytd-watch-flexy[flexy][is-two-columns_]:not([fullscreen]):not([theater]) {
                    --ytd-watch-flexy-max-player-width: calc(var(--ytd-watch-flexy-chat-max-height)*var(--ytd-watch-flexy-width-ratio)/var(--ytd-watch-flexy-height-ratio)) !important }
hide_recommended: #items.ytd-watch-next-secondary-results-renderer, .ytd-watch-grid>#contents, .ytp-pause-overlay
                  #secondary.ytd-watch-grid { overflow-x: hidden; overflow-y: auto; min-height: 300px }
hide_chat:        ytd-live-chat-frame#chat
hide_playlists:   #playlist
hide_donate:      #donation-shelf
hide_endscreen:   .html5-endscreen:not(.mweb-endscreen), .ytp-fullscreen-grid
hide_cards:       .ytp-ce-element, .ytp-ce-hide-button-container
hide_shorts:      .yt-simple-endpoint[title=Shorts], .ytd-search grid-shelf-view-model,
                  #player-shorts-container, .ytd-shorts,
                  yt-chip-cloud-chip-renderer:has(yt-formatted-string[title=Shorts]),
                  yt-tab-shape[tab-title=Shorts],
                  ytd-compact-video-renderer:has(a[href*="/shorts/"]),
                  ytd-notification-renderer:has(>a[href^="/shorts/"]),
                  ytd-reel-shelf-renderer, ytd-rich-grid-renderer[is-shorts-grid],
                  ytd-rich-item-renderer:has(a[href*="/shorts/"]),
                  ytd-rich-shelf-renderer[is-shorts],
                  ytd-video-renderer:has(a[href*="/shorts/"])
hide_comments:    #comment-teaser, #comments
hide_prof:        .ytd-comment-renderer yt-img-shadow
hide_mix:         .ytp-videowall-still[data-is-mix=true], a[href*="start_radio=1"],
                  ytd-browse[page-subtype=home] ytd-video-meta-block[radio-meta],
                  ytd-compact-radio-renderer, ytd-radio-renderer,
                  ytd-rich-item-renderer:has(a[href*="start_radio=1"])
hide_merch:       #clarify-box, #offer-module, #ticket-shelf, .ytp-drawer, yt-alert-with-actions-renderer,
                  ytd-merch-shelf-renderer, ytd-metadata-row-container-renderer>#always-shown
hide_meta:        #primary-inner>#info, ytd-watch-metadata
hide_bar:         #actions.ytd-watch-metadata, #info>#menu-container
hide_channel:     #owner.ytd-watch-metadata, #top-row.ytd-video-secondary-info-renderer
                  with hide_desc: #primary-inner>#meta
                  without hide_desc: ytd-video-primary-info-renderer { border: 0 !important }
hide_desc:        #description.ytd-watch-metadata, ytd-expander.ytd-video-secondary-info-renderer
hide_header:      #guide-spacer, #masthead-container
                  #header.ytd-c4-tabbed-header-renderer, #page-manager.ytd-app { margin-top: 0 !important }
                  ytd-mini-guide-renderer.ytd-app { top: 0 !important }
hide_notifs:      #buttons.ytd-masthead>ytd-notification-topbar-button-renderer.ytd-masthead,
                  ytd-notification-topbar-button-shape-renderer
hide_search:      #primary>.ytd-two-column-search-results-renderer ytd-horizontal-card-list-renderer,
                  #primary>.ytd-two-column-search-results-renderer ytd-shelf-renderer
hide_trending:    .yt-simple-endpoint[href^="/feed/explore"], .yt-simple-endpoint[href^="/feed/trending"],
                  ytd-browse[page-subtype=trending],
                  ytd-guide-section-renderer:has(.yt-simple-endpoint[href^="/feed/storefront"])
hide_moreyt:      #sections>ytd-guide-section-renderer:nth-last-child(2)
hide_subs:        .yt-simple-endpoint[href^="/feed/subscriptions"], ytd-browse[page-subtype=subscriptions],
                  ytd-guide-section-renderer:has(.yt-simple-endpoint[href^="/feed/subscriptions"])
hide_autoplay:    .autonav-endscreen, .ytp-next-button, .ytp-prev-button,
                  button[data-tooltip-target-id=ytp-autonav-toggle-button],
                  ytd-watch-flexy:not([playlist]) .ytp-chrome-controls .ytp-next-button
hide_annotations: .annOption   (the settings menu item tagged by unhook-yt.js)
```

### Mobile (m.youtube.com) selectors

```
always on:          #surveys, .mealbar-promo-renderer
hide_feed:          div[tab-identifier=FEwhat_to_watch]
hide_redirect_home: ytm-pivot-bar-renderer>ytm-pivot-bar-item-renderer:has(.pivot-w2w)
hide_subs:          div[tab-identifier=FEsubscriptions], ytm-pivot-bar-renderer>ytm-pivot-bar-item-renderer:has(.pivot-subs)
hide_trending:      div[tab-identifier=FEexplore], div[tab-identifier=FEtrending], ytm-pivot-bar-renderer>ytm-pivot-bar-item-renderer:has(.pivot-trending)
hide_shorts:        ytm-reel-shelf-renderer, ytm-rich-grid-renderer.is_shorts, ytm-video-with-context-renderer:has(a[href*="/shorts/"]),
                    ytm-pivot-bar-renderer>ytm-pivot-bar-item-renderer:has(.pivot-shorts)
hide_recommended:   ytm-item-section-renderer[section-identifier=related-items]
hide_playlists:     ytm-playlist
hide_comments:      ytm-comment-section-renderer, ytm-comments-entry-point-header-renderer, ytm-engagement-panel
hide_prof:          .comment-icon-container
hide_mix:           ytm-compact-radio-renderer, ytm-radio-renderer
hide_merch:         ytm-compact-offer-module-renderer
hide_donate:        ytm-donation-shelf-renderer-outer
hide_meta:          ytm-item-section-renderer[section-identifier=slim-video-metadata]
hide_bar:           .slim-video-metadata-actions
hide_channel:       ytm-slim-owner-renderer
hide_desc:          .slim-video-metadata-header-content>c3-icon, .slim-video-metadata-info
hide_header:        .mobile-topbar-header
                    #player-container-id, ytm-item-section-renderer[section-identifier=related-items] { top: 0 !important }
                    ytm-app.sticky-player { padding-top: 0 !important }
hide_endscreen:     .ytp-mweb-endscreen-play-next, .ytp-mweb-endscreen-play-previous
hide_autoplay:      .player-controls-middle>button:not(.player-control-play-pause-icon), .ytm-autonav-bar, .ytm-autonav-toggle-button-container
```

### Live check of Unhook's selectors (2026-09-20)

Still matching: `#secondary.ytd-watch-flexy`, `#items.ytd-watch-next-secondary-results-renderer`, `#comments`, `#comment-teaser`, `#playlist`, `#masthead-container`, `.html5-endscreen`, `.ytp-fullscreen-grid`, `.ytp-ce-element` (4 on `dQw4w9WgXcQ`), `button[data-tooltip-target-id=ytp-autonav-toggle-button]`, `.ytp-next-button`, `yt-tab-shape[tab-title=Shorts]` (channel), `.yt-simple-endpoint[title=Shorts]` (guide and mini guide), `.ytd-search grid-shelf-view-model` (2 Shorts shelves on search), `ytd-video-renderer:has(a[href*="/shorts/"])` (7 with the Shorts filter chip active), `ytd-guide-section-renderer:has(.yt-simple-endpoint[href^="/feed/storefront"])` (matches the Explore section), and `#sections>ytd-guide-section-renderer:nth-last-child(2)` (matches More from YouTube, but only because the logged-out guide order is primary, sign-in promo, Explore, More from YouTube, Report history; this rule is positional and fragile).

Broken or stale:

- `.ytd-comment-renderer yt-img-shadow` matches 0. Comments are now `ytd-comment-view-model` and the avatar is `#author-thumbnail`, so Hide Profile Photos does nothing.
- `yt-chip-cloud-chip-renderer:has(yt-formatted-string[title=Shorts])` matches 0 on the search chip bar. Chips are now `chip-shape` with a bare text div, so the Shorts filter chip stays visible.
- `ytd-compact-video-renderer:has(a[href*="/shorts/"])` is dead: the watch sidebar has 0 `ytd-compact-video-renderer` and 26 `yt-lockup-view-model`.
- `a[href*="start_radio=1"]` hides only the anchors inside a Mix lockup, not the lockup card. There were 16 Mix lockups in the test sidebar.
- `ytd-radio-renderer`, `ytd-compact-radio-renderer` and `ytd-reel-shelf-renderer` were 0 on every page tested.
- `/feed/trending` and `/feed/explore` now client-redirect to Home and the guide contains no such links, so the webRequest redirect and the two href selectors no longer do anything.

### Complaints and limitations

Reviews are overwhelmingly positive. The complaints that could be found: an August 2026 review, "No longer works... YouTube videos are blacked out now", and "can't tell when a video ends! It just goes black" (Hide End Screen Feed leaves a black player at the end; 1.6.9's "Fix Hide End Screen Feed" targets this), and a Firefox user reporting that it "keeps breaking YT in freshly-opened tabs". From the code: there is no Shorts to watch redirect at all; the Explore, Trending and Subscriptions redirects happen after navigation starts, so there is a visible flash; the Shorts chip on search and comment avatars are no longer hidden; More from YouTube relies on sidebar position; the Shorts notification rule only catches `ytd-notification-renderer` with a direct `/shorts/` child link. Mobile only works through Firefox Android or Kiwi.

## 2. Remove YouTube Shorts v2.1.7

### Listing

- Chrome Web Store id `mgngbgbhliflggkamjnpdmegbkidiapm`, https://chromewebstore.google.com/detail/remove-youtube-shorts/mgngbgbhliflggkamjnpdmegbkidiapm
- Updated August 10, 2026. 300,000 users. 4.6 stars (673 ratings). 630 KiB. Developer contact techyrecon@gmail.com (ArkTech). Site https://blockscroll.app (an Android app "BlockScroll" is promoted in the listing and popup).
- Store description: "Removes Shorts from search results. Eliminates Shorts from all viewing areas. Now Available as Mobile app, BlockScroll. Works with Kiwi Browser for mobile browser use."
- Not open source, but the package ships webpack source maps with the complete original sources embedded (`content.js.map`, `background.js.map`, `popup.js.map`). The project is internally named "web-summarizer-ai" and `popup.html` has `<title>Web Summarizer Ai</title>`. The package also contains a 241 KB `pageWorld.js` copied from `@inboxsdk/core` that nothing references.

### Manifest

- Manifest V3. `permissions: ["storage"]` (the storage API is never called; state lives in YouTube's page `localStorage`). `host_permissions: ["*://*.youtube.com/*"]`.
- One content script `content.js`, `run_at: document_end`, on `*://*.youtube.com/*` (covers m.youtube.com). The content script entry also declares a `content_security_policy` string that Chrome ignores.
- Background service worker (`type: module`) that only relays popup toggles to the active tab. `minimum_chrome_version: 92`.
- Popup: React 18, headlessui Switch, Tailwind (192 KB `popup.js`).

### Options

Exactly one user-facing toggle, "Enable/Disable Extension", which is disabled with the tooltip "Only on YouTube" unless the active tab is on youtube.com. Links: Support, Mobile App, Feedback. Toggling reloads the tab. Storage keys are `adEnabled` (the real switch) and a vestigial `autoSkipAds` / `adAutoSkipShorts` pair from an ad-skipper that is commented out.

### How it works

On `document_end`, if `adEnabled` is true, it appends a `<style>` element to `document.head` with this fixed block:

```
.ytd-reel-shelf-renderer, a[title="Shorts"], .navigation-container,
yt-chip-cloud-chip-renderer[title="Shorts"], yt-tab-shape[tab-title="Shorts"],
ytd-ad-slot-renderer, ytd-reel-shelf-renderer.ytd-item-section-renderer,
ytm-rich-section-renderer, ytm-reel-shelf-renderer, ytm-promoted-sparkles-web-renderer,
ytd-reel-item-renderer, ytd-rich-shelf-renderer[is-shorts], grid-shelf-view-model   { display: none !important }
#shorts-container        { overflow: hidden !important }
.reel-video-in-sequence  { height: 87vh !important }
/* commented out in source: .ytd-rich-section-renderer */
/* plus styling for its own .blocked-message and .blocked-parent classes */
```

Then a `MutationObserver` on `document.body` (childList, subtree), coalesced through `requestAnimationFrame`, runs `hideShortsElements()` on every mutation burst:

- `removeShortsFromHeader()`: removes every `yt-chip-cloud-chip-renderer` whose `#text` innerText is exactly "Shorts".
- `removeShortsFromSearch()`: removes `ytd-video-renderer:has([href*="/shorts/"])`.
- `modifyParentsOfShortsLinks()` (m.youtube.com only): sets opacity 0.2 and `pointer-events: none` on the grandparent of every `a[href*="/shorts"]` and adds a "Blocked by Remove YouTube Shorts" title.
- `replaceShortsWithWatchId()` (m.youtube.com only): redirects `/shorts` URLs to `/watch?v=`.
- Removes the parent of `.pivot-shorts` (mobile bottom nav).

Redirect on desktop: `setInterval(removeExtraElements, 1000)` polls `location.href` every second. If it contains `/shorts`, it takes `url.match(/shorts\/(.+)/)[1]` (greedy, so a `?feature=share` suffix ends up inside the video id), calls `history.replaceState` and sets `window.location.href = "https://www.youtube.com/watch?v=" + id`. There are no `yt-navigate-*` hooks, so a Short renders for up to a second and then the whole page reloads.

Dead code still shipped in the bundle: `removeShortsGrid` (`ytd-rich-grid-row[is-shorts-grid]`), `disableClickAndOpacity` (adds `.blocked-parent` and a "Shorts blocked by Remove Youtube Shorts" message to `ytd-video-renderer.style-scope.ytd-item-section-renderer` whose `a#thumbnail` points at `/shorts/`), `hideShortsByTitle` (hides `ytd-grid-video-renderer, ytd-rich-item-renderer, ytd-compact-video-renderer, ytd-video-renderer` whose title matches `#shorts` or whose duration is under 55 seconds), `hideShortsByThumbnail` (`[overlay-style="SHORTS"]`), `blockerMessage` (a fixed overlay on Shorts pages), `blockAds` (auto-clicks `.ytp-skip-ad-button` and `.ytp-ad-skip-button-modern`, plays unskippable ads at 6.7x, removes `#player-ads`), and a one-time "support us" update notice.

### Limitations from the code

- Hides every `grid-shelf-view-model` (not only the Shorts ones) and every `ytm-rich-section-renderer` on mobile.
- Hides `ytd-ad-slot-renderer` in-feed ads, which is outside its stated scope.
- Does not touch Home's inline Shorts items (`ytd-rich-item-renderer[is-slim-media]`, `ytd-rich-grid-group`), Shorts in the watch sidebar (`yt-lockup-view-model`), or guide entries in languages other than English (`a[title="Shorts"]`).
- Settings live in per-origin `localStorage`, so they vanish when site data is cleared and never sync.
- The one-second polling redirect causes a flash and a double page load.
- Store reviews are almost all short five-star notes. chrome-stats.com blocked scraping, so older negative reviews were not available.

## 3. Youtube-shorts block v1.5.5

### Listing

- Chrome Web Store id `jiaopdjbehhjgokpphdfgmapkobbnmjp`, https://chromewebstore.google.com/detail/youtube-shorts-block/jiaopdjbehhjgokpphdfgmapkobbnmjp
- Updated November 18, 2025. 400,000 users. 4.5 stars (1.1K ratings). 34.88 KiB. Developer doma_itachi (itachi_dev@outlook.jp). Eight locales (de, en, fr, it, ja, ru, tr, zh_TW).
- Source: https://github.com/doma-itachi/Youtube-shorts-block (MIT, TypeScript, custom build scripts under `build/`, separate Firefox build). The shipped `main.js` and `main.css` are byte-for-byte the compiled `src/` of the repository. Firefox listing: https://addons.mozilla.org/firefox/addon/youtube-shorts-block/
- Store release notes: 1.5.5 "Fix: Extension Breaks YouTube's New Replies UI #64"; 1.5.4 "Additional selector for Shorts shelf elements (#62)" and "Fix timing issue for richShelfFilter (#63)"; 1.5.3 fix a false video-id match when a channel id ends in "shorts"; 1.5.2 stop blocking the entire News page; 1.5.1 fix open-in-new-tab redirecting to the wrong video; 1.5.0 refactor and "blocking ytd-rich-shelf-renderer"; 1.4.1 support the new Subscriptions page; 1.4.0 mobile support and "Hide shorts video" enabled by default; 1.3.4 block Shorts reels; 1.3.0 "Open in regular player" button.

### Manifest

- Manifest V3. `permissions: ["storage"]` only. No `host_permissions`, no background script, no declarativeNetRequest (a comment on issue #18 says a declarativeNetRequest redirect was tried in one commit, but the shipped code redirects in-page).
- Content script `main.js` plus `main.css` on `*://*.youtube.com/*` and `*://m.youtube.com/*`, default `document_idle`, not `all_frames`.
- Popup `popup/index.html`. `web_accessible_resources: assets/to_normal.svg`.

### Options (popup)

- Master on/off toggle (storage key `isEnable`).
- `hide "shorts" tab` (`isHideTabs`, default false).
- `hide shorts video(unstable)` (`isHideVideos`, default true).
- A "Warning! May not work properly!" note with Feedback and Pull request links, and a version label linking to the GitHub release notes.
- Store feature list: Redirect shorts, Hide "shorts" tab, Hide shorts, Extended GUI for shorts player.

### How it works

Redirect: `convertToVideoURL(url)` runs `url.match(/shorts\/(.{11})\/?/)` and returns `https://www.youtube.com/watch?v=<id>`. On load, if the current URL matches, it sets `location.href`. For SPA navigation it listens on `document` for `yt-navigate-start`, reads the destination from `e.target.baseURI`, then calls `history.back()` followed by `location.href = watchURL`. On mobile it listens on `window` for `state-navigatestart` and reads `e.detail.href`. When redirect is disabled and a Shorts page is open, it injects an "inRegular" button (`<div id="block" class="youtube-shorts-block" title="Open in Regular Player(New Tab)">`) into `#actions.ytd-reel-player-overlay-renderer` after polling up to 20 times at 100 ms; clicking pauses every `<video>` and opens the watch URL in a new tab. Live today, `#actions.ytd-reel-player-overlay-renderer` matches 0 on a Shorts page because the action bar is now `reel-action-bar-view-model`, so that button no longer appears.

Hide Shorts: one `MutationObserver` (childList, subtree) on `#content` (desktop) or `#app` (mobile). On every mutation it runs three filters in sequence, and each filter removes matches from the DOM with `.remove()` rather than hiding them:

```
reelShelfFilter:   ytd-reel-shelf-renderer, ytm-reel-shelf-renderer
richShelfFilter:   ytd-rich-shelf-renderer:has(h2>yt-icon:not([hidden]))
                     (a rich shelf whose header has an icon is treated as the Shorts shelf; it does not use [is-shorts])
                   grid-shelf-view-model:has(ytm-shorts-lockup-view-model)
shortsFilter:      ytd-video-renderer ytd-thumbnail a, ytd-grid-video-renderer ytd-thumbnail a,
                   ytm-video-with-context-renderer a.media-item-thumbnail-container
                     if the href contains "shorts", walk up and remove the enclosing
                     YTD-VIDEO-RENDERER, YTD-GRID-VIDEO-RENDERER or YTM-VIDEO-WITH-CONTEXT-RENDERER
channelFilter:     ytd-rich-item-renderer containing span[aria-label='Shorts']   (defined, but not in the active filter list)
```

`richShelfFilter` uses a `querySelectorAllPromise` helper that waits up to 5 x 100 ms whenever nothing matches, on every mutation, which is expensive on busy pages.

Hide tab: adds the class `youtube-shorts-block` to `<body>`. CSS: `.youtube-shorts-block a[title='Shorts'], .youtube-shorts-block a[title='ショート'] { display: none !important; pointer-events: none !important }` (English and Japanese only) and, for mobile, `.youtube-shorts-block ytm-pivot-bar-item-renderer:has(.pivot-bar-item-tab.pivot-shorts)`.

Always-on CSS regardless of settings: `ytd-continuation-item-renderer:not(:last-child):not(#comments *) { display: none }`, meant to hide the loading spinners left behind after shelves are removed. This rule is the root cause of the "replies broken" and "loading breaks" reports below.

### Complaints and limitations (GitHub issues and store reviews)

- #67 (open, Feb 2026): the continuation-spinner CSS breaks loading new videos (https://github.com/doma-itachi/Youtube-shorts-block/issues/67).
- #66 (open, Dec 2025): replies and "Read more" broken in YouTube posts.
- #64: breaks YouTube's new replies UI. Closed by 1.5.5, but store reviews from July and August 2026 still report replies on posts being broken.
- #61 (open): Shorts still visible; the reporter's markup was `grid-shelf-view-model.ytGridShelfViewModelHostHasBottomButton` (YouTube India).
- #60: Hide shorts emptied `/feed/you` (fixed in 1.5.3).
- #59 (open): searching inside a channel at `/@name/search` redirected to `watch?v=search` because the regex hit "shorts" in the channel name; still reported on Brave after the fix.
- #57 (open): the new Home layout `ytd-rich-grid-group` (class `ytdRichGridGroupHost`) is not handled.
- #46 (open): Shorts in watch recommendations are not filtered.
- #36 (open): Shorts still in search results (older shelf variants).
- #54 (open): request for an option to exempt search results.
- #18 (open): the redirect happens after the Shorts page partially loads, leaving a black screen and a delay.
- #23 and #58: users unhappy that hiding is on by default.
- July and August 2026 reviews: "removes shorts from search results entirely... the addon doesn't have an options menu" (the popup is easy to miss), and "Mostly fine but the reply sections on posts mess up".
- Firefox builds lag the Chrome build.

## Consolidated current desktop selector table

Source abbreviations: CPFYT = insin/control-panel-for-youtube v1.35.2 (`page.js`, runs in the MAIN world at `document_start`, https://github.com/insin/control-panel-for-youtube); GIJS = gijsdev/ublock-hide-yt-shorts v2026.4.29 (https://github.com/gijsdev/ublock-hide-yt-shorts/blob/master/list.txt); TAD = tadwohlrapp uBlock Origin gist (https://gist.github.com/tadwohlrapp/722bbe97cb20bb34da8df73675415cae); RYS = lawrencehook/remove-youtube-suggestions v4.3.83 (https://github.com/lawrencehook/remove-youtube-suggestions); IT = ImprovedTube (https://github.com/code-charity/youtube); UH = Unhook; RYSh = Remove YouTube Shorts; YSB = Youtube-shorts block.

"Live" means counted in the real DOM on 2026-09-20. uBlockOrigin/uAssets (https://github.com/uBlockOrigin/uAssets) contains no YouTube Shorts cosmetic filters at all, only ad pruning. A repository named "lonelil/hide-youtube-shorts" does not exist (404); the similar projects are Vulpelo/hide-youtube-shorts, fvnky07/youtube-shorts-blocker, JiruGutema/Hide-Youtube-Shorts and devlulcas/remove-youtube-shorts. DeArrow only rewrites titles and thumbnails. UnTrap for YouTube (id `enboaomnljigfhfjfoalacienlhjlfil`, https://untrap.app/) is closed source.

| Area | Current selectors (desktop) | Used by | Live 2026-09-20 |
|---|---|---|---|
| Home feed (whole) | `ytd-browse[page-subtype="home"]`; `ytd-browse[page-subtype="home"] ytd-rich-grid-renderer`; infinite scroll `ytd-rich-grid-renderer > #contents > ytd-continuation-item-renderer`; chip bar `ytd-feed-filter-chip-bar-renderer` | CPFYT, RYS, UH (`.ytd-rich-grid-renderer`) | browse and grid present; logged-out Home shows only `ytd-feed-nudge-renderer` |
| Home feed items | `ytd-rich-item-renderer[rendered-from-rich-grid]` wrapping `yt-lockup-view-model` (new) or `ytd-rich-grid-media` (old); nudge tile `ytd-rich-item-renderer:has(> #content > ytd-feed-nudge-renderer)`; sections `ytd-rich-section-renderer` | CPFYT, TAD | not verifiable logged out |
| Shorts shelf on Home and Subscriptions | `ytd-rich-section-renderer:has(> #content > ytd-rich-shelf-renderer[is-shorts])`; `ytd-rich-shelf-renderer[is-shorts]`; newer inline forms `ytd-browse[page-subtype="home"] ytd-rich-grid-group` (class `ytdRichGridGroupHost`), `ytd-rich-item-renderer[is-slim-media][rendered-from-rich-grid]`, `ytd-rich-item-renderer:has(ytm-shorts-lockup-view-model-v2)`, `ytd-rich-item-renderer:has(a[href*="/shorts/"])`; YSB uses `ytd-rich-shelf-renderer:has(h2>yt-icon:not([hidden]))`; filtered subs grid `ytd-browse[page-subtype="filteredsubscriptions"] ytd-rich-grid-renderer[is-shorts-grid]` | CPFYT, GIJS, UH, RYSh, IT, YSB | shelf not verifiable logged out; the slim-media item structure confirmed on the channel Shorts tab (45 items) |
| Shorts shelf on Search | `ytd-search grid-shelf-view-model` (or `grid-shelf-view-model:has(ytm-shorts-lockup-view-model)`); legacy `ytd-search ytd-reel-shelf-renderer`; RYS pins classes `grid-shelf-view-model.ytGridShelfViewModelHostHasBottomButton.ytd-item-section-renderer.ytGridShelfViewModelHost` | CPFYT, YSB, UH, GIJS, RYS | 2 `grid-shelf-view-model` (only 1 carries the HasBottomButton class), 0 reel shelves, 17 `ytm-shorts-lockup-view-model-v2`; header is `yt-section-header-view-model > yt-shelf-header-layout h2.ytShelfHeaderLayoutTitle` with text "Shorts"; parent is `ytd-item-section-renderer > #contents` |
| Individual Shorts in search results | `ytd-search ytd-video-renderer:has(a[href^="/shorts"])`; badge forms `ytd-thumbnail-overlay-time-status-renderer[overlay-style="SHORTS"]` and `ytd-video-renderer:has(badge-shape[aria-label="Shorts"])` | CPFYT, UH, RYSh, GIJS, TAD | with the Shorts chip active: 7 by href but only 4 by badge, so the href test is the reliable one |
| Search Shorts filter chip | `ytd-search #chip-bar #chips yt-chip-cloud-chip-renderer[chip-style="STYLE_HOME_FILTER"] > #chip-shape-container > chip-shape > button[role=tab] > .ytChipShapeChip > div` with text "Shorts"; no `title` attribute and no `yt-formatted-string`, so it needs a JS text match (CPFYT adds class `HideShorts`) or the uBO form `:has(.ytChipShapeInactive:has-text(/^Shorts$/i))` | CPFYT, GIJS | UH and RYSh chip selectors match 0 |
| Search filler shelves ("People also watched", "For you", "Channels new to you", "Previously watched") | `ytd-search ytd-shelf-renderer[thumbnail-style]`; `ytd-search #contents.ytd-item-section-renderer > ytd-shelf-renderer`; `> ytd-horizontal-card-list-renderer` (people also search for); right column `ytd-secondary-search-container-renderer:has(ytd-universal-watch-card-renderer)`; promoted `ytd-ad-slot-renderer`, `ytd-search-pyv-renderer`; `ytd-movie-renderer` | TAD, CPFYT, UH, RYS | 3 shelves, 1 watch card, 2 ad slots; 0 horizontal card lists on the test queries |
| Channel page Shorts | tab `yt-tab-shape[tab-title="Shorts"]` inside `yt-tab-group-shape` (`tp-yt-paper-tab` is gone); channel-home shelf `[page-subtype="channels"] ytd-item-section-renderer:has(ytd-reel-shelf-renderer)`; Shorts tab grid `ytd-rich-grid-renderer[is-shorts-grid]` of `ytd-rich-item-renderer[is-slim-media][is-shorts-grid][lockup=true]` | all three, GIJS, TAD, CPFYT | tab 1; MrBeast home had no reel shelf (its shelves are `ytd-shelf-renderer > yt-horizontal-list-renderer > yt-lockup-view-model`); Shorts grid 45 items |
| Shorts guide entries | full guide `ytd-guide-entry-renderer:has(> a[title="Shorts"])` (markup `ytd-guide-entry-renderer > a#endpoint.yt-simple-endpoint[title="Shorts"]`, note the anchor has no href); mini guide `ytd-mini-guide-entry-renderer:has(> a[aria-label="Shorts"])` (the anchor also has `title="Shorts"` and `href="/shorts/"`); lazy forms `a[title="Shorts"]`, `.yt-simple-endpoint[title=Shorts]`; RYS and TAD use `ytd-mini-guide-entry-renderer[aria-label="Shorts"]` | CPFYT, GIJS, UH, RYSh, RYS, YSB | 1 each; the host-level `[aria-label]` form matches 0 because the attribute is on the anchor; the title text is locale dependent |
| Watch sidebar recommendations | whole column `#secondary.ytd-watch-flexy`; list `#related`; `#items.ytd-watch-next-secondary-results-renderer`; items `#related yt-lockup-view-model` (`.ytLockupViewModelHost.content-id-<VIDEOID>.ytLockupViewModelCompact`); Shorts in sidebar `#related ytd-reel-shelf-renderer`, `#related yt-lockup-view-model:has(a[href^="/shorts/"])`; width fix `ytd-watch-flexy[is-two-columns_]:not([fullscreen]):not([theater]) { --ytd-watch-flexy-max-player-width: ... }` | CPFYT, RYS, UH | 26 lockups, 0 `ytd-compact-video-renderer`; `#secondary-inner` children are `#panels`, `#chat-container`, `ytd-playlist-panel-renderer#playlist`, `#inline-panels`, `#persistent-panel-container`, `#donation-shelf`, `#related`; the `[flexy]` attribute is gone, `[is-two-columns_]` is present |
| Live chat | `#chat-container`; `ytd-live-chat-frame#chat`; replay teaser `#teaser-carousel.ytd-watch-metadata`; fullscreen panel `ytd-watch-flexy[live-chat-present-and-expanded] #panels-full-bleed-container` | CPFYT, UH, RYS | `#chat-container` 1 (empty on VOD); `#chat` needs a live stream to verify |
| Playlist panel | `ytd-playlist-panel-renderer#playlist` (`#playlist`) | UH, RYS | 1 (carries `[hidden]` when no playlist) |
| End screen video wall | `.html5-endscreen`; `.ytp-endscreen-content` (12 children `a.ytp-videowall-still[data-is-mix][data-is-list][data-is-live]`); `.ytp-endscreen-previous`, `.ytp-endscreen-next`; fullscreen `.ytp-fullscreen-grid`, `.ytp-fullscreen-grid-stills-container`; autonav `.ytp-autonav-endscreen-countdown-overlay`, `.ytp-autonav-endscreen-upnext-container`, `.autonav-endscreen` | CPFYT, UH, RYS | all present at video end; the player gains `ytp-autonav-endscreen-cancelled-state` |
| End cards and info cards | `#movie_player .ytp-ce-element` (types `.ytp-ce-video`, `.ytp-ce-playlist`, `.ytp-ce-website`, `.ytp-ce-channel`); YouTube's own hide button `.ytp-ce-hide-button-container`; info card button `.ytp-cards-button` and teaser `.ytp-cards-teaser`; annotations layer `.ytp-iv-video-content`; watermark `.annotation.iv-branding` | all | 4 ce-elements on `dQw4w9WgXcQ`; cards button 1 |
| Comments | `ytd-comments#comments` (`#comments`); teaser `#comment-teaser`; panel `ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-comments-section"]`; threads `ytd-comment-thread-renderer > ytd-comment-view-model`; avatar `ytd-comment-view-model #author-thumbnail`; replies `#replies.ytd-comment-thread-renderer`; Shorts comments button `reel-action-bar-view-model > button-view-model:nth-of-type(1)` | CPFYT, UH, RYS | 20 `ytd-comment-view-model`, 0 `ytd-comment-renderer` |
| Mixes | `ytd-rich-item-renderer:has(a[href*="start_radio=1"])` (Home); `yt-lockup-view-model:has(a[href*="start_radio=1"])` (sidebar; the hrefs also contain `list=RD`); `.ytp-videowall-still[data-is-mix="true"]`; legacy `ytd-radio-renderer`, `ytd-compact-radio-renderer`, `ytd-video-meta-block[radio-meta]`; TAD uses `ytd-rich-item-renderer:has(yt-collections-stack)` | CPFYT, UH, TAD | 16 Mix lockups in the test sidebar; legacy renderers 0 |
| Merch, tickets, offers, promos | `ytd-merch-shelf-renderer`, `#merch-shelf` (inside the structured description), `#ticket-shelf`, `#offer-module`, `#clarify-box`, `#donation-shelf`, `ytd-metadata-row-container-renderer > #always-shown`, `ytd-info-panel-container-renderer`; player `.ytp-suggested-action`, `.ytp-drawer`; new `#below > #shopping-timely-shelf`; `#masthead-ad`; `ytd-mealbar-promo-renderer`, `ytd-primetime-promo-renderer` | UH, CPFYT, TAD | containers exist but were empty on the test videos; `#shopping-timely-shelf` is the first child of `#below`; 5 `.ytp-suggested-action` |
| Video info and description | `ytd-watch-metadata` (whole); `#title.ytd-watch-metadata`; `#top-row` (owner plus actions); channel `#owner.ytd-watch-metadata` (`#subscribe-button`, `#sponsor-button` Join, `#owner-sub-count`); buttons `#actions.ytd-watch-metadata` and `#actions-inner` (`segmented-like-dislike-button-view-model`, `ytd-download-button-renderer`); `#bottom-row` (`#description`, `#bottom-actions`, `#comment-teaser`, `#teaser-carousel`); `#description.ytd-watch-metadata`, `#description-inner`, `ytd-text-inline-expander`; expanded `ytd-structured-description-content-renderer #items` children `ytd-horizontal-card-list-renderer[modern-chapters]`, `ytd-video-description-transcript-section-renderer`, `ytd-video-description-infocards-section-renderer`, `ytd-video-description-header-renderer`, `ytd-expandable-video-description-body-renderer`, remix shelf `ytd-reel-shelf-renderer`; AI summary `#video-summary`, `[has-video-summary]`; legacy `ytd-video-secondary-info-renderer` gone | UH, CPFYT, RYS, TAD | all ids present; `#middle-row` empty; `ytd-video-primary-info-renderer` still present but unused |
| Top header and masthead | `#masthead-container`, `ytd-masthead`; spacing `#page-manager.ytd-app { margin-top: 0 }`, `ytd-mini-guide-renderer.ytd-app { top: 0 }`; logo `ytd-topbar-logo-renderer a#logo`; `yt-searchbox`; voice `#voice-search-button`, `.ytSearchboxComponentVoiceSearchWrapper`; Create `#masthead #end #buttons > ytd-button-renderer` | UH, CPFYT, TAD | present |
| Notifications | `ytd-notification-topbar-button-renderer`, `ytd-notification-topbar-button-shape-renderer`; Shorts in the dropdown `ytd-notification-renderer:has(> a[href^="/shorts/"])`; title count stripped via JS on `<title>` (`(N) ` prefix) | UH, RYS | needs a login, not verified |
| Explore and Trending | the pages `/feed/trending` and `/feed/explore` now client-redirect to Home; guide section `ytd-guide-section-renderer:has(a[href^="/feed/storefront"])` (Explore: Music, Movies `/feed/storefront?bp=...`, Hype `/feed/hype`); RYS positional `ytd-guide-section-renderer.style-scope:nth-of-type(4)`; CPFYT tags sections itself (`[cpfyt-section="explore"]`) by matching header text in JS | UH, RYS, CPFYT, TAD | Explore section 1; `a[href^="/feed/trending"]` and `a[href^="/feed/explore"]` 0 |
| More from YouTube | `ytd-guide-section-renderer:has(a[href="/premium"])` (derived, robust); UH `#sections > ytd-guide-section-renderer:nth-last-child(2)`; RYS `:nth-of-type(5)`; premium entry `#endpoint.ytd-guide-entry-renderer[href="/premium"]`; footer `#footer.ytd-guide-renderer`; report history `a[href="/reporthistory"]` | UH, RYS, CPFYT | entries: Try Premium `/premium`, music.youtube.com, youtubekids.com; logged-out section order is primary, `ytd-guide-signin-promo-renderer`, Explore, More from YouTube, Report history (logged in adds the Subscriptions channel list and You) |
| Subscriptions | link `a[href^="/feed/subscriptions"]` (`.yt-simple-endpoint`); page `ytd-browse[page-subtype="subscriptions"]`; channel-list section `ytd-guide-section-renderer:has(a[href^="/feed/subscriptions"])` (logged out this matches the primary section because the Subscriptions entry lives there); "Latest" bar `ytd-browse[page-subtype="subscriptions"] ytd-rich-grid-renderer > #contents > ytd-rich-section-renderer:first-child`; subs Shorts as in the Home row | UH, CPFYT, RYS | link 1 |
| Autoplay toggle | `button[data-tooltip-target-id="ytp-autonav-toggle-button"]` wrapping `.ytp-autonav-toggle-button-container > .ytp-autonav-toggle-button[aria-checked]` (click to switch); `.ytp-next-button`, `.ytp-prev-button`; the player root now carries `.ytp-delhi-modern` | UH, CPFYT | 1; `aria-checked="true"` by default |
| Shorts player page | root `ytd-shorts` (children `#header`, `#offline-container`, `#shorts-container`, `#shorts-panel-container`); `#shorts-inner-container`; `ytd-reel-video-renderer#reel-video-renderer[extract-action-bar][migrate-shorts-player-controls-to-cow]`; `#shorts-player`; `ytd-shorts-player-controls-cow`; overlay `ytd-reel-player-overlay-renderer > yt-reel-player-overlay-view-model` with `yt-reel-metapanel-view-model` (`.ytReelMetapanelViewModelMetapanelItem`), `yt-reel-channel-bar-view-model`, `yt-shorts-video-title-view-model`; action bar `reel-action-bar-view-model` (like-button-view-model, comments, share and remix `button-view-model`, `pivot-button-view-model`); navigation `.navigation-container` with `#navigation-button-up` and `#navigation-button-down` | CPFYT, YSB, RYSh, UH | legacy `#actions.ytd-reel-player-overlay-renderer`, `#comments-button`, `#share-button`, `#remix-button`, `#pivot-button`, `.reel-video-in-sequence`, `#player-shorts-container` all 0 |

### New element names and their internals (all seen live)

- `yt-lockup-view-model`: wrapper class `ytLockupViewModelWrapper`, inner `.ytLockupViewModelHost.content-id-<VIDEOID>` with `.ytLockupViewModelCompact` in the sidebar; children `yt-thumbnail-view-model`, `yt-thumbnail-bottom-overlay-view-model`, `yt-thumbnail-badge-view-model`, `yt-lockup-metadata-view-model`, `yt-content-metadata-view-model`, `yt-decorated-avatar-view-model`. It now carries the watch sidebar and channel shelves.
- `ytm-shorts-lockup-view-model-v2 > ytm-shorts-lockup-view-model`: class `shortsLockupViewModelHost`, link `a.shortsLockupViewModelHostEndpoint.reel-item-endpoint[href^="/shorts/"]`, badge `yt-badge-view-model.shortsLockupViewModelHostBadge`. This is the Shorts tile everywhere, including desktop.
- `grid-shelf-view-model`: `.ytGridShelfViewModelHost`, rows `.ytGridShelfViewModelGridShelfRow`, items `.ytGridShelfViewModelGridShelfItem`, `.ytGridShelfViewModelGridShelfBottomButtonContainer`; header `yt-section-header-view-model > yt-shelf-header-layout`. This is the Shorts shelf on search.
- `ytd-rich-item-renderer` carries `[rendered-from-rich-grid]`, `[is-slim-media]`, `[is-shorts-grid]` and `[lockup=true]`.
- `badge-shape.ytBadgeShapeHost[aria-label]` replaced the old badge renderers; `chip-shape` replaced chip internals; `yt-tab-shape[tab-title]` replaced paper tabs; `yt-page-header-view-model` replaced `ytd-c4-tabbed-header-renderer`; `ytd-comment-view-model` replaced `ytd-comment-renderer`.
- Search results are still `ytd-video-renderer`, now with `[lockup=true][is-search]`.
- `ytd-reel-shelf-renderer`, `ytd-reel-item-renderer`, `ytd-compact-video-renderer`, `ytd-radio-renderer` and `ytd-grid-video-renderer` (outside channel shelves) were 0 on every page tested and should be kept only as legacy fallbacks.
- `ytd-rich-grid-group` (class `ytdRichGridGroupHost`) is the new three-Shorts group on Home, per CPFYT and YSB issue #57.

### SPA navigation and redirect approaches

- Desktop fires `yt-navigate-start` and `yt-navigate-finish` on `document`, and `yt-page-data-updated` on `window`. Mobile fires `state-navigatestart` and `state-navigateend` on `window`.
- None of the three extensions use declarativeNetRequest. A rule such as `regexFilter: ^https://www\.youtube\.com/shorts/([A-Za-z0-9_-]{11})`, `regexSubstitution: https://www.youtube.com/watch?v=\1`, `resourceTypes: ["main_frame"]` only catches full page loads, because SPA navigation is a `pushState` plus an `/youtubei/v1/...` XHR.
- Control Panel for YouTube handles the SPA case without a reload: a capture-phase `click` listener finds `a[href^="/shorts/"]` and rewrites the element's `_data.commandMetadata.webCommandMetadata.url` to `/watch?v=ID` and `webPageType` to `WEB_PAGE_TYPE_WATCH` before YouTube's router runs; for direct loads it redirects when `location.pathname` starts with `/shorts/`. Youtube-shorts block's `yt-navigate-start` plus `history.back()` plus `location.href` approach always reloads the page.
- Mobile selectors on which CPFYT and GIJS agree: `ytm-pivot-bar-item-renderer:has(> div.pivot-shorts)`, `.tab-content[tab-identifier="FEwhat_to_watch"] ytm-rich-section-renderer:has(ytm-shorts-lockup-view-model)`, `ytm-search grid-shelf-view-model`, `ytm-video-with-context-renderer:has(a[href^="/shorts"])`, `ytm-thumbnail-overlay-time-status-renderer[data-style="SHORTS"]`, and `ytm-item-section-renderer[section-identifier="related-items"]` for the mobile sidebar.

## Sources

- Unhook store listing: https://chromewebstore.google.com/detail/unhook-remove-youtube-rec/khncfooichmfjbepaaaebmommgaepoid
- Unhook Firefox version history: https://addons.mozilla.org/en-US/firefox/addon/youtube-recommended-videos/versions/
- Unhook site: https://unhook.app/
- Remove YouTube Shorts store listing: https://chromewebstore.google.com/detail/remove-youtube-shorts/mgngbgbhliflggkamjnpdmegbkidiapm
- Youtube-shorts block store listing: https://chromewebstore.google.com/detail/youtube-shorts-block/jiaopdjbehhjgokpphdfgmapkobbnmjp
- Youtube-shorts block repository and issues: https://github.com/doma-itachi/Youtube-shorts-block
- Control Panel for YouTube: https://github.com/insin/control-panel-for-youtube
- uBlock Origin Shorts list: https://github.com/gijsdev/ublock-hide-yt-shorts
- tadwohlrapp uBlock Origin YouTube filters: https://gist.github.com/tadwohlrapp/722bbe97cb20bb34da8df73675415cae
- Remove YouTube Suggestions: https://github.com/lawrencehook/remove-youtube-suggestions
- ImprovedTube: https://github.com/code-charity/youtube
- uAssets: https://github.com/uBlockOrigin/uAssets
- UnTrap for YouTube: https://untrap.app/
