# End to end checklist

Manual checks against the live www.youtube.com, driven through Chrome DevTools.
Add a dated line whenever you verify or fix something. Logged-out YouTube
unless noted.

## 2026-09-20, Chrome 153, versions 0.4.0 to 1.0.0

YouTube markup observed: Shorts shelves are `grid-shelf-view-model` with
`ytm-shorts-lockup-view-model-v2` items; sidebar recommendations are
`yt-lockup-view-model` inside `ytd-item-section-renderer`; the guide's Shorts
entry has `a[title="Shorts"]` with no href, the mini guide entry has
`a[href="/shorts/"]`; Explore entries link to `/feed/storefront`, `/feed/hype`
and a Music channel; More from YouTube links to `/premium`, music and kids.

| Check | Result |
| --- | --- |
| Full load of `/shorts/90ab3E3Ek-M` | Redirected to `/watch?v=90ab3E3Ek-M` by the declarative rule |
| Shorts shelf in search (`grid-shelf-view-model`) | Hidden |
| Shorts guide entry and mini guide entry | Hidden |
| Home page with redirect on | Lands on `/feed/subscriptions` |
| Sidebar recommendations (`#related`) | Hidden; visible again when the filter is off |
| Mixes in sidebar (`yt-lockup-view-model` with `list=RD`) | Hidden |
| Comments | Hidden |
| Autoplay toggle | Switched off by the script on load and after navigation |
| Search shelves (`ytd-shelf-renderer`, ads, `ytd-secondary-search-container-renderer`) | Hidden, 10 of 10 real results visible |
| Explore and More from YouTube guide sections | Hidden |
| Toggle in popup while a YouTube tab is open | Applied without reload |
| Countdown 30 s to completion | Filter turned off, badge shows OFF, all attributes removed |
| Countdown cancelled by closing the popup | Filter stayed on |
| Delay select disabled while on | Yes; shorter delay refused by options page, longer accepted |
| Educational mode, Education video (aircAruvnKk) | Plays, no overlay |
| Educational mode, Music video (eYuUAGXN0KM) | Overlay, video paused, play() re-paused |
| Educational mode after in-page search navigation | Overlay on the new video, category read from `ytd-page-manager` |
| Educational mode switched off while blocked | Overlay removed, video plays |
| Options: add category while on | Refused with message; removing a category saved |
| Options: reset while on | Refused (would loosen) |
| Adult blocker, keyword host (xvideos.com) | Block page with hostname |
| Adult blocker, custom domain (example.com) | Block page |
| Adult blocker, allow list entry for example.com | Site loads; keyword hosts still blocked |
| Adult blocker, filter off | Ruleset disabled, dynamic rules removed |
| Hide Top Header | Masthead hidden, page margin 0, player at the top |
| Hide Video Info | Description and bottom row hidden, title and channel visible |
| Hide Video Sidebar at 900 px (one-column layout) | Recommendations that YouTube moves below the video are hidden too (fixed in 0.4.x) |
| Educational mode, allowed video after reload | Plays without a manual resume |
| Search "Shorts" filter chip | Tagged by content.js and hidden |
| Logo click with redirect on | Goes straight to `/feed/subscriptions`, no home flash |
| Educational mode, blocked video to allowed video by in-page search | Overlay gone, Education video playing within 4 s (waiting guard plus resume) |
| Three popup toggles clicked within a few ms | All three persisted (serialised updates) |
| Block screen after the text-node rewrite | Title and category shown correctly, video paused |

## 2026-09-20, Chrome 153, version 1.1.0 (full feature matrix)

| Check | Result |
| --- | --- |
| Search page baseline (defaults) | 10 results visible, Shorts shelves, ads, shelves hidden, search filter chips kept |
| Watch page baseline | related and comments hidden, autoplay off, cards hidden, metrics visible |
| Hide Thumbnails, Hide Metrics, Hide Search Suggestions, Grayscale | thumbnails, metadata lines and duration badges hidden with titles and channel names intact; suggestions container hidden while typing; `filter: grayscale(1)` on `ytd-app`; like count, view count and subscriber count hidden on watch |
| Blur Thumbnails | `blur(14px) saturate(0.6)` on thumbnail images |
| Live toggling while a video plays | playback unaffected |
| Popup with 36 rows, YouTube and Everywhere groups | renders, active toggles locked while on, all editable while off |
| Distracting sites on | 26 dynamic redirect rules for the default presets, two watcher scripts registered |
| reddit.com in pause mode | pause page with host, countdown, intention, budget line, Continue disabled until both are done |
| Continue | pass granted, session allow rule, budget debited, expiry alarm; reddit loads in grayscale |
| Pass expiry (1 minute) | warning toast 30 seconds before, tab returns to the pause page, cooldown message shown, session rule removed |
| Budget 0 | exhausted view, no Continue |
| Block mode | example.com (custom `example.com/`) blocked, example.com/other loads |
| In-app navigation | `history.pushState('/r/pics/')` from an allowed subreddit lands on the pause page through the main-world hook |
| Exceptions | `reddit.com/r/programming` loads under a whole-site reddit preset, grayscale always applied |
| Locked hours | filter forced on immediately and by alarm, power disabled, update refused with the end time |
| Lock for 1 hour | power disabled, note shows the end time, turning off refused |
| Options gates while locked | remove preset, block to pause, remove day, later start, bigger budget, intention off, remove pattern all refused; add preset, shorter pass, longer pause, add pattern saved |
| Reason line and stats | shown on block pages and in the popup ("blocked 3 times, 1 pass used") |
| Adult blocker | xvideos.com lands on the adult view of the block page |
| Popup countdown | starts at the chosen delay, cancel works |
| Console | no errors or warnings on popup, options, block page |

Bug found and fixed during this run: content-script navigation to the
block page produced `chrome-extension://invalid/` until the page was listed
under `web_accessible_resources`.

Not verifiable without a signed-in account in the test browser: notification
bell hiding, live chat hiding, subscriptions channel list hiding, playlist
panel, fundraiser shelf, merch shelf. Their selectors come from current
YouTube markup and Unhook's public behaviour; please report if any reappear.

The permission prompt for Block adult sites is native Chrome UI and cannot
be clicked from DevTools. The blocker mechanics were verified with the
permission pre-granted in a test manifest; the shipped manifest requests it
optionally from the popup.

## 2026-09-20, Android 15 emulator (Pixel AVD, API 35), version 1.2.0

Run with `adb`, `android/scripts/emu.sh` and the debug build. The real
YouTube app on the image refuses to run without an update it cannot get, so
the Shorts detector was exercised with the stand-in app under
`android/fakeyoutube`, which uses the same view ids and a fork's package name
(`app.revanced.android.youtube`). The accessibility service must be turned
off and on again (or the device rebooted) after every reinstall; Android does
not rebind it otherwise, and the Setup and Home screens now say so.

| Check | Result |
| --- | --- |
| Setup screen | Lists the accessibility service, overlay, notification access and battery rows with the right states; restricted settings hint on Android 13 or later |
| Blocked app (Chrome) with the filter on | Block screen over Chrome within a second, blocks counter incremented |
| Pause mode | Countdown counts only while the block screen is in front; Continue disabled until zero; pass of 5 minutes granted, budget debited to 25; Chrome reopens without a block during the pass |
| Keep it blocked | Home screen, Chrome gone |
| Shorts stand-in | ShortsActivity closed by Back within a second (view ids `reel_player_page_container` and friends); WatchActivity with `watch_player` left alone (negative signal); feeds closed counter incremented |
| Stand-in HomeActivity | Never closed (no fullscreen feed ids) |
| Adult site filter | VPN consent dialog, then `tun0` up; `nslookup xvideos.com` returns no such name, `nslookup example.com` resolves |
| Turn off with a 5 minute delay | State stayed on through the countdown and turned off at about 290 s; leaving the screen at 15 s cancelled it |
| Turn off with a 30 s delay | Off after the countdown; `unlockDelaySec` kept at 30 |
| Delay picker | Disabled while on; set at turn-on |
| Locked hours with today enabled | Filter forced on within 16 s by the service ticker; Home reads "Locked until 17:00", Turn off replaced by a disabled Locked button |
| Disabling the schedule while locked | Refused: "The filter is on. Loosening it needs the filter off first." |
| Day chips | Wrap onto two rows; Sat and Sun reachable at 1080 px width |
| Lock for 8 hours | `lockUntil` set to now plus 8 h, Home reads "Locked until 22:24" |
| Strict mode, filter on | App info page and the Accessibility settings page left within a second (6 guard hits); Wi-Fi settings page stays |
| Strict mode, filter off | App info page stays |
| Reinstall regression | Chrome block and Shorts stand-in still work after the rebuild |

Found and fixed while testing: `findAccessibilityNodeInfosByText` returns
nothing inside Compose screens, and the Android 15 App info page is one, so
the strict mode guard now walks the node tree itself. The schedule's day chips
overflowed the card; they wrap now. The More tab could read "1 passes used".

Not verifiable on this host: the real YouTube, Instagram, Facebook and
Snapchat apps (no Play sign-in on the AVD); notification hiding from a real
blocked app; Private DNS interaction on a physical network.

## 2026-09-20, Android 15 emulator (Pixel AVD, API 35), version 1.3.0

Same setup as the 1.2.0 run. The uiautomator dump used by `emu.sh tap`
waits for an idle screen, so during the countdown (the ring animates every
second) taps went by coordinates instead.

| Check | Result |
| --- | --- |
| Name and header | Launcher label, header wordmark, block screen and Setup read AntiBrainrot; the status pill reads On, Off or Locked with the lock icon |
| Home, filter on | Blue hero card with the power ring, Lock for row, Turn off; Locked state shows the lock in the ring and a disabled Locked button |
| Home, filter off | Surface card, empty ring, delay picker, Turn on |
| Turn off with a 5 minute delay | Ring counts down with the seconds inside it and 4:57 below; off after 300 s |
| Keep it on | Countdown cancelled, filter still on 34 s later (30 s delay) |
| Turn off with a 30 s delay | Off after the countdown; Turn on afterwards leaves the delay at 30 s |
| Today strip | Reads 1m filter on, 2 blocks, 1 feed closed after one block screen and one closed Short; opens Progress |
| Progress, empty | Nothing yet card, zero tiles, empty bars with weekday labels |
| Progress, seeded 60 days (debug button) | 7 day tiles 24h 19m, 24, 31, 6 passes 30 min; 30 day tiles 166h 7m, 154, 155, 41 passes 205 min; bars grow in on range change; date labels every 7 days at 30 and every 30 at 90; stacked blocks and feeds chart with legend; streak 2 with best 28 and seven dots; most blocked apps with icons and bars |
| History on disk | Today's record holds blocks, feedsClosed, focusSeconds (60 after one minute of the filter on) and byApp counts |
| Chrome block | Card slides in, icon pops, ring fills from 9 to 0, Continue enabled at Ready |
| Shorts stand-in | Closed within a second, feeds closed counter incremented |
| Strict mode | App info page left within a second |
| Setup | Green check marks for granted rows; Notifications row reads the real permission state (was always Open on a fresh install) |
| Apps tab | App list loads after the settings cards with a progress bar, rows reorder with animation when one is checked |
| Locked hours card on Home | Chips and time buttons expand when the switch is on; Sun chip on the second row |
| Unit tests | 24 pass, including history recording, pruning, streaks, top apps and the Stats migration |
| Dark theme (1.3.1) | Header title, the Apps list and every tab readable; the 1.3.0 build drew the title and list black. Status bar icons turn light with the Dark theme choice and dark again on System |

Not verifiable on this host: the animations' smoothness at 60 Hz (the
emulator renders with SwiftShader), dark theme on a real display.

## 2026-09-20, Android 15 emulator (Pixel AVD, API 35), version 1.4.0

Per-app and per-site rules. Chrome on the emulator freezes the guest under
SwiftShader's Vulkan path, so `/data/local/tmp/chrome-command-line` got
`--disable-gpu`; unrelated to the app.

| Check | Result |
| --- | --- |
| Apps tab | Settings card (pause, session, cooldown, intention, notifications), search, one row per app with the rule under the name and a pill: Block (dark), 5 min (light), Add (outlined); ruled apps sort first |
| Rule sheet, app | Segmented No rule / Block / Timer, minute chips 5 min to 3 h, help text per mode; Chrome set to Timer 5 min and Calendar to Block while the filter was on |
| Stored settings | `apps.rules` holds `com.android.chrome: timer 5` and `com.google.android.calendar: block`; `sites.rules` holds `example.com: block` and `wikipedia.org: timer 5` |
| Loosening a fresh rule | Switching a just-added Block to Timer while on was refused; Add now opens the sheet without a rule so the first choice is free |
| Sites tab | Rule rows with a globe, host and rule, an add field and suggestion chips for the usual feeds; adult switch and allow list below |
| Calendar (block) | Block screen "Not now, Calendar is blocked", only Keep it blocked |
| Chrome (timer 5 min) | "Take a breath, Chrome has a limit of 5 minutes a day", ring to Ready, intention, "4m left today", Continue for 4m opens Chrome |
| Session end | Chrome blocked again after the session with the cooldown message |
| example.com (block) in Chrome | Block screen "Not now, example.com is blocked" as soon as the address bar shows it, also on reopening Chrome with that tab; DNS answers NXDOMAIN too (`tun0` up) |
| wikipedia.org (timer 5 min) in Chrome | Block screen with the Chrome icon, "wikipedia.org has a limit of 5 minutes a day", Continue for 5m; the page loads; 43 s of use metered after 45 s in front, keyed `site:wikipedia.org`; Chrome itself, without a rule, is not metered |
| wikipedia.org limit used up | After 5 minutes of the page in front the block screen came up on its own: "Time's up. You have used your 5 minutes in wikipedia.org for today. It resets at midnight." |
| Filter off, rule removal | Chrome's rule removed while off, refused while on |
| Home and Progress | Home strip reads filter on, blocks and timed use; Progress lists wikipedia.org and Chrome under most used timed apps and sites, and wikipedia.org, Calendar and Chrome under most blocked |
| Unit tests | 29 pass: rules per app and site, loosening per rule, sessions capped by the minutes left, usage, streaks, the 1.3 list migration and the distracting sites migration |

## 2026-09-20, Chrome 153, version 1.4.1

Signed-in guide regression, reproduced in the logged-out test browser with a
detached document built like the signed-in "You" section (History,
Playlists, Your videos linking to Studio, Watch later) next to a real "More
from YouTube" section.

| Check | Result |
| --- | --- |
| Old More from YouTube rule | Matched both sections, so the "You" section with History went with it |
| New rule | Matches only the More from YouTube section; a section holding History, Playlists or You is left alone |
| Hide History toggle | Listed in the popup after Hide Subscriptions; with its attribute set, the guide's History entry is `display: none`, and visible again without it |
| Real guide, logged out | History visible with the filter on and the toggle off |

## 2026-09-20, Android 15 emulator (Pixel AVD, API 35), version 1.5.0

| Check | Result |
| --- | --- |
| Block new app installs switch | Under the session settings on the Apps tab; on while the filter was on (tightening) |
| New package while installs are blocked | `adb install` of the release variant gave it a Block rule within seconds; opening it showed "Not now, AntiBrainrot is blocked" |
| Rule survives uninstall | After `adb uninstall` the rule stayed in settings and the Apps tab listed the package under "Not installed right now"; after reinstalling, opening it showed the block screen again |
| Apps list refresh | The list reloads when the screen comes back, so the removed app moved to the not-installed section without restarting |
| Prevent uninstall | The switch opens Android's device admin confirmation ("Activate this device admin app"); after activating, `adb uninstall` fails with DELETE_FAILED_DEVICE_POLICY_MANAGER |
| Strict mode and the admin page | The device admin page for the app (where it could be deactivated) is left within a second, one guard hit |
| Session notifications | With a 1 minute session on Clock (timer 5 min): an ongoing "Clock" notification with the time left and a heads-up "Clock: one minute left. Wrap up. The block screen comes back when the minute is over." Both listed by dumpsys (ids 20 and 21) |
| Session end | The block screen returned on its own after the session, reading "Cooling down. The next session opens at 18:37." |
| Unit tests | 30 pass, including the installer rule and the loosening rules for installs and uninstall protection |

Not verifiable on this host: the Play Store (the Google APIs image has a
stub without a launcher activity) and the package installer and uninstall
dialogs, which cannot be started from the shell on this image; both are
plain app windows handled by the same block and guard code as App info.

## 2026-09-21, Android 15 emulator (Pixel AVD, API 35), version 1.6.0

Timers as plain limits, the pause as an option.

| Check | Result |
| --- | --- |
| Apps tab card | "How rules work" with the pause switch (off by default), its length and the intention line only while on, notifications and the install block |
| Timed app, pause off | Clock (timer 5 min) opened directly, no block screen; the ongoing notification "Clock, 4m left today" was posted while it was in front |
| Pause on | Switching it on while the filter was on was accepted (tightening); opening Clock showed the countdown, the intention line and Continue |
| Pause memory | Back after 10 s away: no pause. Back after 75 s away: the pause again |
| Limit used up | After 5 minutes of Clock in front the block screen came up on its own: "Time's up. You have used your 5 minutes in Clock for today." A heads-up "Clock: one minute left" had fired before. Clock stayed blocked after a reinstall of the app |
| Shade | Opening the notification shade no longer counts as leaving the app: the ongoing time-left notification for a timed Camera was still there after expanding and collapsing it (the first run had cleared it) |
| Unit tests | 30 pass: limits, kept-alive pauses, pause loosening rules, migrations |

## 2026-09-21, Chrome 153, version 1.7.0 (Shorts as a setting, Prevent removal)

Driven through the chrome-devtools-ext MCP browser, logged-out YouTube.

| Check | Result |
| --- | --- |
| Popup with the filter on | Hide Shorts is an ordinary row (checked, disabled while on, no "always on" tag); Prevent removal listed under Everywhere after Locked hours |
| Search page, defaults | `data-abr-shorts` on `<html>`; 15 Shorts shelves (`grid-shelf-view-model`) present, 0 visible; guide entry hidden; `shorts` ruleset enabled |
| Filter off | Attributes removed, all 15 shelves visible, `shorts` ruleset disabled, badge OFF; a full load of `/shorts/90ab3E3Ek-M` stays on the Shorts player |
| Hide Shorts off, filter on | 17 attributes set, none for Shorts; the Shorts player stays visible and the URL is not rewritten; ruleset stays disabled |
| Hide Shorts on while the filter is on | Accepted as tightening; the open Shorts page was rewritten to `/watch?v=90ab3E3Ek-M` by the content script within a second; ruleset enabled again; a fresh full load of the Shorts URL redirected by the declarative rule |
| Options page | Prevent removal card shows "Chrome reports this copy as removable" (`installType` development) and the one-line policy command with the pinned id and the update URL |
| Prevent removal, extensions page | With the tabs permission granted (test copy with `tabs` required, since the native prompt cannot be clicked from CDP) and the feature on: `chrome://extensions/` and `chrome://extensions/?id=<id>` both landed on `blocked.html?kind=guard` within 1.5 s; `chrome://version/` was left alone |
| Prevent removal, filter off | `chrome://extensions/` opened normally; turning the filter back on re-armed the guard |
| Guard view | Heading "Not while the filter is on", two lines of explanation, Leave button; no console messages |
| Stale worker | After editing `background.js` the MCP browser kept a cached service worker without the new functions; `reload_extension` fixed it. Not a product issue, noted for the next session |
| CRX | `scripts/pack-crx.mjs` output parsed back: CRX3 magic, same public key and 16-byte id as `google-chrome --pack-extension` with the same key, both signatures verify, id `ibcicobbbpfmonjbhpmllnjgdkedneop` |
| Unit tests and check | 75 pass; `npm run check` ok with 34 CSS attributes and the update manifest at 1.7.0 |

Not verified on this host: the policy install itself. Writing to
`/etc/opt/chrome/policies/managed` would force-install the extension into
every Chrome on this machine, including the main browser, and an isolated
mount namespace was not permitted. The CRX, the update manifest and the
policy text follow Chrome's documented format; the first real check is a
Linux machine with the policy file in place, looking for "installed by your
administrator" on the extensions page and "installed by policy" in the
options.

## 2026-09-22, Chrome 153, version 1.8.0

Run in the chrome-devtools-ext MCP browser with a test copy of the
extension that has `<all_urls>` in `host_permissions` (the permission
prompt is native UI), Block adult sites on with both children, then Block
keywords on with the list `feet`, `foot fetish`, `nudes`.

| Check | Result |
| --- | --- |
| Rulesets after the switch | `shorts`, `adult`, `safesearch`, `youtube-restrict` enabled together |
| Page embedding favicons from xvideos.com and pornhub.com, plus an xvideos.com iframe | All three `net::ERR_BLOCKED_BY_CLIENT`; a Wikipedia favicon on the same page loaded |
| xvideos.com | Adult view of the block page, `Keep it blocked` the only button |
| Block page for a YouTube URL (`kind=block`) | `Keep it blocked` and `Subscriptions` |
| Google `search?q=feet&udm=2` | Redirected once to `...&safe=active&ssui=on`, no loop; Google's abuse check appeared first for the automation browser, then the page loaded |
| Bing `images/search?q=feet` | `adlt=strict` added, header reads "SafeSearch: Strict" |
| DuckDuckGo `?q=feet&ia=images` | `kp=1` added, the page's own `i.js` calls carry `p=1` |
| Yahoo images | `vm=r` added, page reads "SafeSearch on" |
| Yandex images, Brave images | `family=yes` and `safesearch=strict` added, pages loaded |
| YouTube `results?search_query=feet` | Document request carries `youtube-restrict: Strict` |
| Keyword rules | Two dynamic rules (5000 redirect, 5001 block, priority 5) for the three words |
| Google `search?q=feet`, Reddit `r/feet/`, Bing `images/search?q=foot+fetish` | Keyword view of the block page; the Bing one reads `The address on bing.com contains "foot fetish".` |
| Bing `search?q=football` | Loads (whole words only) |
| YouTube in-page navigation to `results?search_query=feet+pics` | Content script sends the tab to the keyword view |
| Popup | `Force safe search` and `Restrict YouTube` nested under `Block adult sites`, `Block keywords` a row of its own |
| Options | Both checkboxes on; the policy command carries `ForceGoogleSafeSearch` and `ForceYouTubeRestrict: 0` |
| Console | No errors or warnings on the block, popup and options pages |

## 2026-09-22, Android 15 emulator (Pixel AVD, API 35), version 1.8.0

Debug build installed with `android/scripts/emu.sh install` on the `m2a_pixel`
AVD, which still had `example.com` blocked and `wikipedia.org` timed from
the 1.4.0 run, so the DNS filter was already running.

| Check | Result |
| --- | --- |
| Sites tab | `Force safe search` and `Restrict YouTube` under `Block adult sites`, greyed until it is on; a `Blocked keywords` card with a field and Save |
| Block adult sites on | Both children on by default |
| `ping www.google.com`, `google.nl` | 216.239.38.120, the address of `forcesafesearch.google.com` |
| `ping www.bing.com` | 150.171.30.16, one of `strict.bing.com` |
| `ping duckduckgo.com` | 52.142.126.100, `safe.duckduckgo.com` |
| `ping www.youtube.com`, `m.youtube.com` | 216.239.38.120, `restrict.youtube.com` |
| `ping mail.google.com` | Its own address, untouched |
| `ping example.com` | Unknown host, the site rule's NXDOMAIN as before |
| Keywords | `feet` saved from the field; Save greys out once the list matches |
| Filter off | Countdown ran out at 30 s, `tun0` gone |
| Unit tests | 38 pass, including the DNS answer builder, the safe search targets, the keyword matcher and the new loosening cases |

Not verified: the address bar keyword block in a browser. Chrome on this
image dies with SIGTRAP inside libmonochrome on every launch, with the
filter on or off and with the tunnel up or down (its own log reads "Enter
Safe Mode for CachedFlags, crash streak is 2"), and no other browser is
installed. The keyword check sits on the same address bar path as the site
rules verified in the 1.3.0 and 1.4.0 runs, and the matcher itself is unit
tested.
