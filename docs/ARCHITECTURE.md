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
| `content/hide.css` | youtube.com | Every hiding rule, keyed by `data-abr-*` attributes on `<html>`. Shorts rules have no attribute. |
| `content/content.js` | youtube.com, isolated world | Sets attributes from settings, handles in-page navigation, home redirect, autoplay, and the educational overlay. |
| `content/page-bridge.js` | youtube.com, main world | Reads the player response and emits `abr:video` with a JSON string. |
| `background.js` | service worker | Seeds settings, badge text, enables the adult ruleset and dynamic rules. |
| `rules/shorts.json` | declarativeNetRequest | Redirects full loads of `/shorts/ID`. |
| `rules/adult.json` | declarativeNetRequest | Domain list plus keyword rules redirecting to `blocked/blocked.html`. Disabled until the feature is on. |
| `popup/` | action popup | Toggle tree, filter switch, delay picker, countdown. |
| `options/` | options page | Lists, delay, theme, import, export, reset. |
| `blocked/` | extension page | Themed block page. |

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
   `adult` ruleset and replacing the dynamic rules built from the user's own
   lists. Allow rules carry a higher priority than block rules.

## The loosening rule

`settings.isLoosening(current, next)` is the single definition of "allows
more". `settings.update()` refuses such a change with `LockedError` while
the filter is on and stays on. The popup disables active toggles in that
state; the options page shows the refusal in its status line. Turning the
filter off is only done by the popup after its countdown.

## Pinned id

`manifest.json` includes a public `key`, so the extension id is the same for
every install: `ibcicobbbpfmonjbhpmllnjgdkedneop`. The adult ruleset needs an
absolute `chrome-extension://` URL for its redirect target, which is only
possible with a stable id. `scripts/check.mjs` verifies the rules and the key
agree.
