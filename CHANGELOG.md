# Changelog

All notable changes to Anti-Brainrot are listed here. The format follows
Keep a Changelog and the project uses semantic versioning.

## [Unreleased]

## [1.8.3] - 2026-09-23

### Fixed
- Android: typing in a browser's address bar no longer brings up the block
  screen for a site you are not going to. Chrome fills in the rest of an
  address from history as you type, so "li" read as linkedin.com and a
  blocked LinkedIn locked you out mid-word. The address bar now counts
  only once it shows a page: while it has focus nothing is judged, and the
  site you open is checked as soon as the bar lets go, including when you
  press Go faster than the scan interval.

## [1.8.2] - 2026-09-22

### Fixed
- The popup and the block page no longer leave an unchecked
  runtime.lastError ("The message port closed before a response was
  received") on the extension's errors page when the worker gives no
  answer, which happens when the files were updated under a running
  worker or while the worker restarts. Every message to the worker now
  treats no answer as an empty reply.

## [1.8.1] - 2026-09-22

### Changed
- Prevent removal no longer keeps you from managing your other extensions.
  A Manage extensions button sits under the switch in the popup: it runs
  the same countdown as turning the filter off, then opens the extensions
  page and leaves it alone for five minutes, after which the guard
  returns. Closing the popup cancels the countdown, as always.
- When Chrome reports the copy as installed by policy, the guard stands
  down altogether: the extensions page cannot remove or switch off a
  managed extension, so there is nothing to guard against and the page
  stays open.
- The guard's block page and the options page explain both.

## [1.8.0] - 2026-09-22

### Added
- Block adult sites now keeps the media off other pages too. Images, video,
  frames, scripts and fetches served by any of the 15,000 listed domains or
  a keyword host are blocked wherever they are embedded, so a forum or a
  search results page cannot show them. Navigations still land on the block
  page. Your own blocked sites get the same treatment.
- Force safe search, under Block adult sites, on by default. Every search on
  Google, Bing, DuckDuckGo, Yahoo, Yandex and Brave, images and videos
  included, carries the engine's strict setting in its address, whatever
  the account says. It is the same parameter Chrome's own
  ForceGoogleSafeSearch policy appends.
- Restrict YouTube, under Block adult sites, on by default. Every YouTube
  request carries the Restricted Mode header, which hides mature videos and
  comments.
- Block keywords, in the Everywhere group. A list of words in the options;
  any web address whose path or query contains one of them, as a whole
  word, goes to the block page: a search on any engine, a subreddit, a tag
  page. Frames and fetches with one are blocked. In-page searches on YouTube
  and on the distracting sites are caught too. Adding a word works any time,
  taking one off waits until the filter is off.
- The policy file the options page prints for Prevent removal also carries
  Chrome's ForceGoogleSafeSearch and ForceYouTubeRestrict settings while
  the matching switches are on.
- Android: Force safe search and Restrict YouTube, under Block adult sites.
  The DNS filter answers lookups for Google search, Bing, DuckDuckGo and
  YouTube with the addresses of their forced safe search hosts, the way
  those companies document it for networks. The YouTube app follows too.
- Android: Blocked keywords on the Sites tab. A page whose address carries
  one of the words gets the block screen in the supported browsers.

### Changed
- The block page only offers the way to Subscriptions when the blocked page
  was on YouTube. An adult site, a feed elsewhere or the extensions page
  guard now shows Keep it blocked alone.
- The name is written Anti Brainrot everywhere it is shown: the extension,
  its pages, the website and its wordmark, the README, the phone. The
  repository, package and file names keep the slug anti-brainrot.
- The leaf on the website glides.

### Known limits
- No filter here judges a picture by its content. A page that is not on
  the list, not a search engine and has no blocked word in its address
  still shows what it shows.

## [1.7.0] - 2026-09-21

### Changed
- Hide Shorts is a setting. It is on by default and sits under the filter
  like every other toggle: it goes off with the filter, and switching it off
  while the filter is on waits for the countdown. Nothing in the extension
  is locked on any more. Existing installs keep Shorts hidden.
- The descriptions on the extension, the website, the README and GitHub
  describe the whole tool instead of leading with YouTube.

### Added
- Prevent removal, in the Everywhere group. While it and the filter are on,
  the browser's extensions page, where the Remove button and the on/off
  switch live, is sent to a block page as soon as it opens, the way strict
  mode leaves App info on the phone. It needs the tabs permission, asked for
  on the first switch-on; turning it off waits until the filter is off. The
  toolbar menu can still remove the extension.
- A browser policy install for the real lock. The build also produces a
  signed CRX, every release attaches it, and the website serves an update
  manifest. Listing the extension in Chrome's ExtensionInstallForcelist (one
  file on Linux; the options page shows the command) makes Chrome install
  its own copy, keep it updated, and refuse to remove or turn it off until
  the policy file is deleted. The options page shows whether Chrome reports
  the copy as policy-managed. Windows and macOS honour this for an extension
  outside the Web Store only on a managed machine.
- After a removal while Prevent removal was on, Chrome opens the install
  page of the website.

### Fixed
- Android: the Setup screen and the Home screen no longer promise Shorts and
  Reels closing, which went away in 1.4.0.

## [1.6.0] - 2026-09-21

### Changed
- Android: timers are plain daily limits. A timed app or site opens on its
  own and can be used for its minutes a day, counted while it is in front;
  when they are gone it is blocked until midnight, with a warning a minute
  before. Sessions, session length and cooldowns are gone. The pause before
  opening is one optional switch, off by default, with its length and the
  intention line underneath; when on, it shows again after a minute away.
  The Apps tab's settings card reads "How rules work" and is half the size.

## [1.5.0] - 2026-09-20

### Added
- Android: Block new app installs. While the filter is on, the Play Store,
  other app stores and the package installer get the block screen, and any
  app that gets installed anyway starts out with a Block rule. Turning it
  off waits until the filter is off, like every loosening.
- Android: rules outlive their app. An uninstalled app keeps its rule and is
  blocked or timed again the moment it is reinstalled; the Apps tab lists
  such rules under "Not installed right now" so they can be changed.
- Android: Prevent uninstall. The app becomes a device admin with no
  policies, which Android refuses to uninstall until the admin is turned off
  in Settings; strict mode leaves that page, and the uninstall dialog, as
  soon as they open. Turning it off waits until the filter is off.
- Android: while a timed app or site is in front, a quiet notification shows
  the time left and a heads-up warning comes one minute before the block
  screen returns.

### Fixed
- Android: the Apps list is reloaded whenever the screen comes back, so an
  app installed or removed meanwhile shows up right away.

## [1.4.1] - 2026-09-20

### Fixed
- Signed-in YouTube: the "You" section of the guide, with History,
  Playlists, Watch later and Liked videos, disappeared with the filter on.
  Its "Your videos" entry links to YouTube Studio, which the Hide More from
  YouTube rule matched. The rule now leaves any section that holds History,
  Playlists or You alone.

### Added
- Hide History toggle, off by default: hides the History entry in the guide
  and the History shelf on the You page.

## [1.4.0] - 2026-09-20

### Changed
- Android: rules are per app and per site. Each app on the list, and each
  site, is either blocked outright or on a daily timer of its own, chosen in
  a sheet from the Apps or Sites tab. Sites are read from the browser's
  address bar (Chrome, Firefox, Samsung Internet, Brave, Edge, Opera,
  Vivaldi, DuckDuckGo); blocked sites are also answered at the DNS level. Timed apps open after the pause for one session at a time, and only
  the time they are in front counts against the limit; when it is used up the
  app stays blocked until midnight. The single mode, the shared daily budget
  and the distracting sites switch with its presets are gone; existing lists
  migrate to one rule per app and per site.
- Android: Progress and Home show time in timed apps, per day and per app.

### Removed
- Android: the Shorts, Reels and Spotlight detection inside other apps and
  the stand-in test app. The app blocks or times whole apps only.

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
