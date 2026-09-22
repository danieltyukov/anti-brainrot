# Architecture

Manifest V3, plain JavaScript, no build step. Everything the popup, options
page, service worker and content scripts share lives in `extension/lib/` as
scripts that attach one object each to `globalThis.AntiBrainrot`. Node tests
`require` those same files.

## Pieces

| Path | Runs where | Job |
| --- | --- | --- |
| `lib/features.js` | everywhere | The feature registry: id, label, default, CSS attribute, parent and mode. Display order of the popup. |
| `lib/settings.js` | everywhere | Defaults, validation, load and save in `chrome.storage.sync`, `isActive`, `activeAttributes`, the loosening rule. |
| `lib/shorts.js` | content, tests | `/shorts/ID` to `/watch?v=ID`. |
| `lib/education.js` | content, options, tests | Category and channel policy. |
| `lib/countdown.js` | popup, tests | Countdown state machine and formatting. |
| `lib/blocker.js` | worker, options, block page, tests | Domain parsing and dynamic rule builders. |
| `lib/distractions.js` | worker, options, watcher, tests | Presets, the pattern grammar, matching, RE2 rule builders, pass and budget helpers. |
| `content/distractions.js` | enabled distracting hosts, isolated world | Redirects in-app navigation, grayscale, pass warning, expiry. Registered with `chrome.scripting`. |
| `content/distractions-main.js` | enabled distracting hosts, main world | Wraps `history.pushState` and `replaceState` and raises `abr:navigate`. |
| `content/hide.css` | youtube.com | Every hiding rule, keyed by `data-abr-*` attributes on `<html>`, Shorts included. |
| `content/content.js` | youtube.com, isolated world | Sets attributes from settings, handles in-page navigation, home redirect, autoplay, and the educational overlay. |
| `content/page-bridge.js` | youtube.com, main world | Reads the player response and emits `abr:video` with a JSON string. |
| `background.js` | service worker | Normalises settings on start, badge text, Shorts and adult rulesets, custom rules, distracting site rules and scripts, passes, cooldowns, budget, stats, locked hours, the extensions page guard. |
| `rules/shorts.json` | declarativeNetRequest | Redirects full loads of `/shorts/ID`. Enabled while Hide Shorts is active. |
| `rules/adult.json` | declarativeNetRequest | Domain list plus keyword rules redirecting navigations to `blocked/blocked.html`, with block twins for the media those hosts serve to other pages. Disabled until the feature is on. |
| `rules/safesearch.json` | declarativeNetRequest | Query transform redirects adding each search engine's strict parameter. Enabled while Force safe search is active. |
| `rules/youtube-restrict.json` | declarativeNetRequest | Sets `YouTube-Restrict: Strict` on YouTube requests. Enabled while Restrict YouTube is active. |
| `lib/keywords.js` | shared | Blocked keyword normalisation, URL matching, and the dynamic rule builder. |
| `popup/` | action popup | Toggle tree, filter switch, delay picker, countdown. |
| `options/` | options page | Lists, delay, theme, import, export, reset. |
| `blocked/` | extension page | Themed block page with five views: adult, block, pause, keyword, guard. The Subscriptions link only shows for a YouTube URL. |

## Data flow

1. `content.js` runs at `document_start`, before YouTube renders. It asks
   `settings.load()` and sets `data-abr-<attr>` for every active feature.
   `hide.css` was injected by the manifest even earlier, so hidden elements
   never flash.
2. Changing anything in the popup or options calls `settings.update()`,
   which writes one `settings` object. `chrome.storage.onChanged` fans that
   out to every open YouTube tab, the popup, the options page and the
   worker. Each re-renders from the new object.
3. YouTube navigates in place. `content.js` listens to `yt-navigate-start`
   and `yt-navigate-finish` and re-runs URL rules (Shorts, home redirect),
   autoplay, and the educational decision.
4. For educational mode the isolated world cannot read page objects, so
   `page-bridge.js` in the main world resolves the player response (event
   detail, `ytd-page-manager.getCurrentData()`, `ytInitialPlayerResponse`, or
   a fetch of the watch page as last resort) and dispatches `abr:video`.
   `content.js` calls `education.decide()` and either removes the overlay or
   inserts it and keeps the video paused.
5. The worker reacts to settings changes by enabling or disabling the
   `shorts` and `adult` rulesets and replacing the dynamic rules built from
   the user's own lists. Allow rules carry a higher priority than block
   rules.
6. Distracting sites use dynamic redirect rules (ids 3000 to 3499),
   exception allow rules (3500 to 3999) and session pass rules (4000 and up,
   priority 4). The worker registers the two watcher scripts for the enabled
   hosts only. A pass is granted through a serialised queue, stored in
   `chrome.storage.local` with its expiry, mirrored as a session rule, and
   revoked by an alarm, after which a cooldown entry replaces it.
7. Locked hours and ad hoc locks: `settings.isLockedNow` is checked by the
   popup, by `settings.update` (refuses turning the filter off) and by the
   worker, which forces the switch on through `settings.patch` on every
   settings change and once a minute.
8. Prevent removal: with the optional `tabs` permission the worker watches
   `tabs.onCreated` and `tabs.onUpdated` and sends any tab at
   `<scheme>://extensions` to `blocked.html?kind=guard` while the feature is
   active, and sweeps open tabs on every sync. It also sets the uninstall
   URL to the website's install section. The real lock is outside the
   extension: a browser policy (`ExtensionInstallForcelist`) pointing at
   `site/updates.xml`, which names the CRX attached to the release. The
   options page reads `management.getSelf().installType` to show whether
   the running copy is policy-managed.

## Rule id ranges

| Ruleset | Ids | Priority | Purpose |
| --- | --- | --- | --- |
| static `shorts` | 1, 2 | 1 | Shorts to watch redirect |
| static `adult` | 1 to 6, 7 to 12, 13 | 1, 1, 2 | domain list and keyword redirects, their subresource block twins, benign keyword exceptions |
| static `safesearch` | 1 to 7 | 1 | strict parameter per search engine |
| static `youtube-restrict` | 1 | 1 | Restricted Mode header |
| dynamic | 1000, 1001, 2000 | 2, 2, 3 | user's blocked adult domains (navigations, then media), allowed domains |
| dynamic | 5000 and up | 5 | blocked keywords, a redirect and a block per five words |
| dynamic | 3000 to 3499 | 2 | distracting site redirects |
| dynamic | 3500 to 3999 | 3 | distracting site exceptions |
| session | 4000 and up | 4 | active passes |

## The loosening rule

`settings.isLoosening(current, next)` is the single definition of "allows
more". `settings.update()` refuses such a change with `LockedError` while
the filter is on and stays on. The popup disables active toggles in that
state; the options page shows the refusal in its status line. Turning the
filter off is only done by the popup after its countdown.

## Pinned id and the CRX

`manifest.json` includes a public `key`, so the extension id is the same for
every install: `ibcicobbbpfmonjbhpmllnjgdkedneop`. The adult ruleset needs an
absolute `chrome-extension://` URL for its redirect target, which is only
possible with a stable id, and the policy install names the id too.
`scripts/check.mjs` verifies the rules, the key and `site/updates.xml`
agree.

`scripts/pack-crx.mjs` signs the release zip into a CRX3 file with the
private half of that key (outside the repository, `~/.config/anti-brainrot/
key.pem` locally and the `CRX_KEY_PEM` secret in the release workflow).
The CRX3 header is a small protobuf message written by hand so the build
stays dependency free.
