# Privacy

Anti-Brainrot is a local tool. This page lists everything it touches.

## What is stored

Your settings (which toggles are on, the unlock delay, theme, educational
categories and channels, custom blocked and allowed sites) are stored in
`chrome.storage.sync`. Chrome may sync that object between your own browsers
when you are signed in to Chrome. Nothing else is stored.

## What is sent

Nothing is sent to the author or to any third party. There is no analytics,
no telemetry, no update check beyond what Chrome itself does for
extensions.

The extension makes exactly one kind of network request of its own: when
educational mode is on and YouTube's in-page navigation has not exposed the
current video's category, the content script fetches that same watch page
from www.youtube.com (same origin, with your normal cookies) and reads the
category from it. That request goes to YouTube only.

## Permissions

- `storage`: settings.
- `declarativeNetRequest`: the Shorts redirect rule and the adult site
  ruleset. Rules are declarative; the extension never sees request contents.
- Host access to youtube.com: the content scripts that hide elements and
  gate videos.
- Optional host access to all sites: requested only when you switch on
  Block adult sites, because a redirect rule can only act on sites the
  extension has access to. You can revoke it at any time in
  `chrome://extensions`.

## What the content scripts read

On www.youtube.com the extension reads the page's own data about the current
video (id, title, category, channel) to decide whether educational mode
should block it. It does not read your account, history, comments or
anything you type.
