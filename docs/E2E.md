# End to end checklist

Manual checks against the live www.youtube.com, driven through Chrome DevTools.
Add a dated line whenever you verify or fix something. Logged-out YouTube
unless noted.

## 2026-09-20, Chrome 153, version 0.4.0

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

Not verifiable without a signed-in account in the test browser: notification
bell hiding, live chat hiding, subscriptions channel list hiding, playlist
panel, fundraiser shelf, merch shelf. Their selectors come from current
YouTube markup and Unhook's public behaviour; please report if any reappear.

The permission prompt for Block adult sites is native Chrome UI and cannot
be clicked from DevTools. The blocker mechanics were verified with the
permission pre-granted in a test manifest; the shipped manifest requests it
optionally from the popup.
