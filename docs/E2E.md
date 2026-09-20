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
