# Changelog

All notable changes to Anti-Brainrot are listed here. The format follows
Keep a Changelog and the project uses semantic versioning.

## [Unreleased]

## [1.3.1] - 2026-09-20

### Fixed
- Android: text outside cards (the header title, the app list) was drawn
  black in the dark theme because the transparent scaffold set no content
  colour. Status and navigation bar icons now follow the app's own theme
  choice instead of the system's.

## [1.3.0] - 2026-09-20

### Added
- Android: a Progress tab. The app keeps ninety days of daily counters (time
  the filter was on, block screens, feeds closed, passes and their minutes,
  block screens per app) and shows them as totals, bar charts over 7, 30 or
  90 days, a streak of days with the filter on for at least an hour, and the
  most blocked apps. Home shows today's three numbers and opens Progress.

### Changed
- Android: the app is called AntiBrainrot. New header with the leaf mark and
  a status pill, a hero card for the filter that cross-fades between its off
  and on looks, a countdown ring for the friction timer and on the block
  screen, staggered card entrances, animated tab changes and counters,
  refined typography and corner radii. Locked hours moved from their own tab
  to a card on Home. The app list loads off the main thread.

## [1.2.0] - 2026-09-20

### Added
- Android app (`android/`), attached to releases as an APK: Shorts, Reels
  and Spotlight closed inside their apps, blocked apps with pause, intention,
  timed passes, daily budget and cooldown, notification hiding, a DNS-level
  filter for adult and distracting sites, the friction timer, locked hours,
  Lock for N hours, strict mode, daily counters. Setup screen for the
  permissions Android needs.
- Emulator test helper (`android/scripts/emu.sh`) and a stand-in YouTube app
  for exercising the detector.

## [1.1.3] - 2026-09-20

### Fixed
- The "Lock for" row showed while the filter was off, and the pause page's
  intention field could show when it should not: display rules overrode the
  hidden attribute. A blanket rule now makes hidden win everywhere.

## [1.1.2] - 2026-09-20

### Changed
- Descriptions in the manifest, README, site and GitHub About now mention
  distracting sites and locked hours.

## [1.1.1] - 2026-09-20

### Changed
- Preset corrections from the research pass: Snapchat now covers Spotlight,
  Discover and Stories and leaves chat open; X adds search and trends;
  Facebook adds home.php, Videos, Gaming and the groups feed; Instagram adds
  the feed root and Stories; Bluesky and Tumblr add feeds, explore, tags
  and search; Pinterest covers the country domains.

## [1.1.0] - 2026-09-20

### Added
- Distracting sites: presets for the feed and short-video surfaces of 16
  platforms, custom patterns with a host, front-page and path grammar,
  exceptions that stay open, pause mode with a countdown, an intention line,
  timed passes drawn from a daily budget and a cooldown, block mode,
  in-app navigation caught by a watcher registered only on enabled hosts,
  grayscale during a pass or always, a warning before a pass ends.
- Locked hours (weekly schedule) and Lock for N hours from the popup. The
  filter turns itself on and the power button is disabled until the lock
  ends.
- YouTube: hide or blur thumbnails, hide view counts, likes and durations,
  hide filter chips, hide posts, news and games shelves, hide search
  suggestions, grayscale.
- A reason line shown on block pages and in the popup, and a daily count of
  blocks and passes.
- Consistency check that the shipped manifest never grants all-sites access
  at install.

### Changed
- All toggles are editable while the filter is off.
- The block page has three views and a "Keep it blocked" button.
- Worker writes that enforce locks go through the same serialised queue as
  user changes; granting passes is serialised too.

### Fixed
- Content-script navigation to the block page failed with an invalid URL
  because the page was not web accessible.

## [1.0.1] - 2026-09-20

### Changed
- Described as the anti brain rot extension for Chrome rather than a YouTube
  tool, in the manifest, README, site and GitHub description.
- README mark switches to the cream version on dark GitHub themes.

## [1.0.0] - 2026-09-20

First full release: everything from 0.1.0 to 0.4.0 plus the items below, the
install website at https://danieltyukov.github.io/anti-brainrot/, and a full
end to end pass recorded in docs/E2E.md.

### Added
- Sub-options from Unhook: Hide Profile Photos under comments; Hide Buttons
  Bar, Hide Channel and Hide Description under video info.
- Search "Shorts" filter chip hidden, up next countdown hidden with autoplay
  off, unread count stripped from the tab title with notifications hidden,
  logo click goes straight to Subscriptions.
- Popup completes the site blocker switch after the permission prompt even
  when the prompt closes the popup.

### Fixed
- Hide Video Sidebar now also hides the parts YouTube moves below the video
  in the one-column layout.
- An allowed video no longer stays paused after the educational check.
- The educational block screen is built from text nodes; the video title
  never reaches innerHTML.
- Educational mode stays closed while a video's category is still unknown,
  and shows the block screen if the category never arrives.
- Settings updates are serialised, so two quick toggles cannot overwrite
  each other.
- A custom blocked site now wins over the bundled allow list for keyword
  false positives.

## [0.4.0] - 2026-09-20

### Added
- Adult site blocker: a bundled list of 15,000 domains plus hostname keyword
  rules, redirecting to a block page. Off by default, needs a one-time
  permission grant, and sits under the filter switch and its timer.
- Options page: educational categories and channel allow list, custom blocked
  and allowed sites, unlock delay, theme, export, import and reset.
- Rule: tighten any time, loosen only while the filter is off. Applies to
  every toggle, list and the delay.

## [0.3.0] - 2026-09-20

### Added
- Educational videos only mode. Watch pages outside the allowed categories are
  paused behind a block screen. Channel allow list.

## [0.2.0] - 2026-09-20

### Added
- Popup with the toggle tree, the filter switch and the friction timer.

## [0.1.0] - 2026-09-20

### Added
- Shorts blocking everywhere, with /shorts/ID rewritten to /watch?v=ID.
- Hiding rules for the home feed, sidebar, end screens, comments, mixes,
  merch, video info, header, notifications, search shelves, guide sections.
- Redirect home to subscriptions, disable autoplay, hide annotations.
