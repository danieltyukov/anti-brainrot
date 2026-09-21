# Privacy

Anti-Brainrot is a local tool. This page lists everything it touches.

## What is stored

Your settings (which toggles are on, the unlock delay, theme, educational
categories and channels, custom blocked and allowed sites, distracting site
lists and pass settings, the schedule, your reason line) are stored in
`chrome.storage.sync`. Chrome may sync that object between your own browsers
when you are signed in to Chrome.

Passes, cooldowns, today's pass budget and today's block and pass counts are
stored in `chrome.storage.local` on this machine only. They hold a pattern
name and a timestamp, never a URL or a page title, and nothing older than
today is kept.

## What is sent

Nothing is sent to the author or to any third party. There is no analytics,
no telemetry, no update check beyond what Chrome itself does for
extensions. A copy installed through the browser policy is updated by
Chrome from `updates.xml` on the website and the CRX on the GitHub
release, which are plain static files.

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
- `scripting`: registers the distracting sites watcher, and only for the
  hosts you enabled. Nothing runs on other sites.
- `alarms`: the once-a-minute locked hours check and pass expiry timers.
- Optional host access to all sites: requested only when you switch on
  Block adult sites or Block distracting sites, because a redirect rule can
  only act on sites the extension has access to. You can revoke it at any
  time in `chrome://extensions`.
- Optional `tabs`: requested only when you switch on Prevent removal. It
  lets the worker see tab URLs, which it needs to notice the extensions
  page; it looks at nothing else, stores no URL, and Chrome labels the
  permission "read your browsing history" because that is the broadest
  thing it could do. Revoking it switches Prevent removal off.
- `management.getSelf()` (no permission needed): the options page reads
  how this copy was installed to say whether a browser policy protects it.

## What the content scripts read

On www.youtube.com the extension reads the page's own data about the current
video (id, title, category, channel) to decide whether educational mode
should block it. It does not read your account, history, comments or
anything you type.

## The Android app

Settings and the ninety day counters are stored on the phone only, in the
app's private storage. The accessibility service reads which app is in front
and, for apps with a daily timer, how long it stays there; it does not read
text you type or messages. The notification listener sees notifications from blocked apps
only to dismiss them. The DNS filter handles name lookups on the device and
forwards them to the network's own resolver; no traffic leaves the phone
through the app and nothing is logged. There are no accounts, no analytics
and no network calls of the app's own.
