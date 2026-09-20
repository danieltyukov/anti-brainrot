# Changelog

All notable changes to Anti-Brainrot are listed here. The format follows
Keep a Changelog and the project uses semantic versioning.

## [Unreleased]

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
