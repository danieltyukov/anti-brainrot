# Anti-distraction tools: feature landscape

Date of research: 2026-09-20. Method: web search and page fetches only (store
listings, vendor sites, help centres, GitHub READMEs, reviews). No packages
were unpacked and no browser automation was used, so option names are as the
vendor or a reviewer writes them, not as read from shipped code. Where a store
page was behind a consent wall, the same description was read from the Firefox
listing or a mirror, and that is the URL cited. The companion document
`existing-extensions.md` covers Unhook, Remove YouTube Shorts and
Youtube-shorts block at code level and is not repeated here.

The Anti-Brainrot baseline used for the "have / add" marks is the working tree
on this date: `README.md`, `extension/lib/features.js`,
`extension/lib/distractions.js`, `extension/lib/settings.js`,
`extension/content/distractions.js`, `extension/blocked/blocked.js` and
`extension/background.js`.

Sections:

1. Tool by tool: what each offers and how it is enforced
2. Feature catalogue with have / partial / add / skip marks
3. URL pattern table for sixteen platforms
4. Sources

## 1. Tool by tool

### Unhook (YouTube)

Chrome, Firefox, Edge. Closed source. Every option is a popup toggle that
writes an attribute on `<html>`; hiding is pure CSS, plus a small main-world
script for autoplay, annotations, the notification count in the title and the
logo redirect. Explore, Trending and Subscriptions redirects are tab
navigations issued from a `webRequest` listener. Full option list, storage keys
and selectors are in `existing-extensions.md`.

Options (popup labels): Hide Home Feed, Redirect to Subscriptions, Hide Video
Sidebar, Hide Recommended, Hide Live Chat, Hide Playlist, Hide Fundraiser, Hide
End Screen Feed, Hide End Screen Cards, Hide Shorts, Hide Comments, Hide
Profile Photos, Hide Mixes, Hide Merch, Tickets, Offers, Hide Video Info, Hide
Buttons Bar, Hide Channel, Hide Description, Hide Top Header, Hide
Notifications, Hide Inapt Search Results, Hide Explore, Trending, Hide More
from YouTube, Hide Subscriptions, Disable Autoplay, Disable Annotations, and a
global on/off. No timers, budgets, schedules or locks of any kind.

Sources: https://addons.mozilla.org/en-US/firefox/addon/youtube-recommended-videos/
and https://unhook.app/

### UnTrap for YouTube

Chrome, Firefox, Safari (iOS and macOS). Claims 250 or more options. The
distraction set is the same family as Unhook (hide Shorts, comments, related
and suggested videos, notifications, buttons, sidebars, trending tabs, ad
banners) plus thumbnail treatments ("Grayscale, blur, or hide thumbnails"),
content filtering ("Block videos, channels, or comments using keywords,
channel IDs, or regex"), and navigation automation ("Auto-redirect the YouTube
homepage to a custom page", auto theater or full screen).

The focus tools are what set it apart from Unhook, quoted from the listing:

- "Password-Protected Settings: Keep your preferences secure"
- "Temporary Focus Mode: Block distractions with a single click"
- "Scheduled Focus Sessions: Automate distraction-free time blocks"
- "Opening Delays: Add a pause before accessing YouTube or launching the
  extension"
- "Custom Hotkeys: Instantly trigger features with keyboard shortcuts"

Enforcement is in-page (content script and CSS); the opening delay is an
overlay countdown before the page is usable. Free tier has all of the above;
UnTrap Plus adds unlimited AI summaries and cloud sync.

Sources: https://addons.mozilla.org/en-US/firefox/addon/untrap-for-youtube/
and https://gigazine.net/gsc_news/en/20250204-untrap-for-youtube/

### Control Panel for YouTube (insin)

Chrome, Firefox, Edge, Safari. Open source, MIT. Over 70 options, applied by
CSS plus a page script that watches YouTube's SPA events. Options by section
as listed on the vendor page (desktop unless noted):

- Ads: Block ads, Hide Sponsored videos and promos, Hide Merch, Offers etc.
- Video Lists: Hide suggested sections in Home, Subscriptions and Search, Hide
  Live videos, Hide Streamed videos, Hide Mixes, Hide Playlists, Hide Movies
  and TV, Hide Upcoming videos, Hide Members only videos, Hide Collaborations,
  Hide low view videos (Related), Hide Auto-dubbed videos, Hide watched videos
  (with a watched percentage threshold), Hide hidden videos, Hide channels,
  Disable Home feed, smaller search thumbnails, grid items per row, Hide Posts
  in Home (mobile).
- Video Pages: Disable Autoplay, Disable Stable Volume, Disable Ambient mode,
  Hide Related videos, Hide Next button, Hide channel watermark, Hide video
  metadata, Hide Ask button, Hide Comments, Always use theater mode, Hide
  video end cards, Hide video endscreen content, Hide Chat, Hide watch page
  sidebar when empty, Hide Premium Jump ahead button.
- Shorts: Hide Shorts, Redirect Shorts to normal player, Hide suggested
  actions, Hide link to related Short by channel, Always show progress bar,
  Stop Shorts looping, Hide Shorts metadata until you hover, Hide Remix
  button.
- Annoyances: Hide AI Summaries, Hide Premium upsells, Hide information
  panels, Disable video previews, Automatically pause channel trailers.
- UI Tweaks: Show full video titles, Hide categories in Home, Tidy Guide
  sidebar, Link YouTube logo to Subscriptions, Hide "Latest" bar in
  Subscriptions, Hide channel banner images.

No timers, budgets or locks.

Source: https://soitis.dev/control-panel-for-youtube

### ImprovedTube

Chrome, Firefox, Edge, Opera. Open source. About 175 options, mostly player and
layout, with a distraction subset: hide Shorts, hide comments, hide sidebar
and related, hide home feed, hide thumbnails, blur thumbnails, hide view
counts, hide details, disable autoplay, Zen Mode layout, lecture and
presentation modes, auto-pause when leaving the tab, permanent playback speed
and quality locks, and day and week watch-time tracking. Ad blocking with a
per-channel whitelist. All in-page.

Source: https://improvedtube.com/

### DF Tube (Distraction Free for YouTube)

Chrome and Firefox; a maintained MV3 fork exists. By default it "disables
autoplay, hides the video recommendations sidebar as well as the related
videos that appear at the end of videos, and removes the grid of recommended
videos that appear on your homepage"; optional "hide comments" and "disable
playlists sitewide". The fork lists feed, sidebar, comments, trending tab,
notification bell, related videos, live chat, playlists and merch as
toggles, plus a quick global on/off. Pure in-page hiding via a content
script. No timers or locks.

Sources: https://addons.mozilla.org/en-US/firefox/addon/df-youtube/ ,
https://extpose.com/ext/mjdepdfccjgcndkmemponafgioodelna and
https://github.com/adrianbartnik/distraction-free-youtube-chrome-extension

### News Feed Eradicator

Chrome and Firefox. Open source. Replaces the feed element on a site with a
quote card and leaves the rest of the site intact. Supported sites from the
version notes: Facebook (and web.facebook.com), Twitter/X (including trending
and news removal), Reddit (with a "Subreddit eradication" toggle), Hacker
News, YouTube (home feed and related, plus notification and subscription
hiding), LinkedIn, Instagram (feed and Explore), GitHub dashboard, Threads and
Bluesky. Options: "Customise each site" with per-site feature toggles, custom
quotes, bulk quote add and CSV import/export, disable built-in quotes
individually, "Disable the blurred background" and transparent background
styles, per-site dark or light mode. Friction features: a temporary disable
timer (since 2.0.0) and a hold-to-snooze control (since 3.0.0) so the feed
cannot be shown with a single click. Removal is a mix of CSS selectors and
DOM replacement keyed off a declarative site registry.

Sources: https://github.com/jordwest/news-feed-eradicator ,
https://addons.mozilla.org/en-US/firefox/addon/news-feed-eradicator/versions/
and https://deepwiki.com/jordwest/news-feed-eradicator

### StayFocusd

Chrome only. Settings as named by reviews of the options page:

- Blocked Sites with a shared daily Max Time Allowed for the group, or Allowed
  Sites mode that blocks everything except the list.
- Active Days and Active Hours restrict when the limit applies; Daily Reset
  Time sets when the counter resets.
- "Block this entire site" or only specific pages, subdomains or paths, and
  in-page content limiting (images, videos, forms, games).
- Require Challenge: to change settings you must type a paragraph of text
  without a mistake (backspace is disabled).
- The Nuclear Option: block for the number of hours you specify on the days
  you choose, either all sites, only the blocked list, or everything except
  the allowed list; it "will work independent of the active hours" and cannot
  be cancelled once armed. Reviews claim it survives uninstall and restart.
- A Dashboard with time per site per day; a Focus Timer; a social media
  feature blocker for YouTube Shorts, TikTok and Instagram components;
  cross-device sync with Android.

Enforcement: a time counter per active tab and a block page; the Nuclear
Option is a stored deadline the options page refuses to edit.

Sources: https://www.cisdem.com/resource/stayfocusd-review.html ,
https://tooltivity.com/extensions/stayfocusd-website-blocker-and-focus-timer ,
https://mindfultechwork.com/stayfocusd-nuclear-option/ and
https://www.techjunkie.com/stay-focused-chrome-extension-review/

### LeechBlock NG

Chrome, Firefox, Edge. Open source. The most complete option set in this
category, all enforced by a background timer over tab URLs plus a block or
delay page. Up to 30 block sets, each with:

- What to block: domains one per line, paths, `*` wildcards, `+` exceptions,
  `~` keywords, `>` referrer rules, comments.
- When to block: time periods in HHMM ranges, a time limit in minutes per
  period, "OR" or "AND" of the two, day selection, rollover of unused time,
  count only the active tab, count only tabs playing audio.
- How to block: redirect to the built-in or a custom page, or apply a CSS
  filter instead (blur, grayscale) and mute the tab, close the tab, block
  immediately or only after the first access, show the matched keyword, "delay"
  mode with a countdown in seconds that auto-loads the page when it ends and
  can be cancelled when the tab loses focus, limit access after the
  countdown to N minutes, add blocked pages to history or not.
- Lockdown: block the set immediately for a chosen duration, during which the
  set's own options are locked; the options page and (on Chrome) the
  extensions and settings pages can be blocked during active periods.

General options: password or random access code to open the options page,
access code length, prevent options access during time periods, countdown
timer overlay and badge, warning message N seconds before a block, temporary
override with a duration and a maximum frequency per hour, day or week,
optional separate override password, dynamic site list from a URL, custom
regex for block and allow, sync storage, statistics page, export and import.

Source: https://www.proginosko.com/leechblock/documentation/

### BlockSite

Chrome, Firefox, Edge, iOS, Android. Freemium, account based. Features by
name: Block List, Focus Mode (a Pomodoro-style timer that blocks the list for
the interval), Schedule (days and hours), Password Protection (needed to
pause blocking, end a focus session early or remove a site), Site Redirect (to
a URL of your choosing), Custom Block Page, Insights (time per site and
category), Block by Category, Block by Keyword (also on search queries),
Whitelist Mode, Bulk Adult Block, Uninstall Prevention, Sync on Multiple
Platforms, Slack Integration, and media counters that "track YouTube Shorts,
YouTube videos, and Instagram Reels watched each day, with the ability to set
separate daily limits for each type of media". Enforcement is a block page
redirect and in-page counters; password and uninstall prevention rely on the
account.

Sources: https://blocksite.co/ ,
https://addons.mozilla.org/en-US/firefox/addon/blocksite/ and
https://chromewebstore.google.com/detail/blocksite/bggpghhpmdhmgpcaoncnjakckiflgecm

### Cold Turkey Blocker

Windows and macOS desktop app with a companion extension; blocks at OS level
(proxy or driver), so it survives browser changes. Feature names from the
vendor:

- Block lists with wildcards, exceptions, application blocking, YouTube
  channel targeting.
- Scheduled blocks drawn on a weekly calendar; autostart at a time, at sign-in
  or at the next scheduled block.
- Locks: Timer (no changes until a time), Restart (must reboot), Password
  (Pro), Random text (type 1 to 5000 random characters or custom text), Time
  range (no changes during chosen hours on chosen days), Schedule (no changes
  while a scheduled block is active or about to start).
- Breaks: Pomodoro (alternate on and off), Allowance (a usage budget that
  refills per block, day, week or month or on a rolling window; only counts
  while the tab is foreground and the mouse moved in the last 3 minutes),
  Reward (breaks earned after working N minutes), Delay breaks (a countdown
  before a break starts).
- Frozen Turkey: lock, log off or shut down the computer for a period.
- Block page with a motivational message, statistics of top distractions,
  "Pause for a Cause" (donate to WWF to buy a 10 minute break), blocking of
  task managers and the clock settings so the timer cannot be cheated.

Sources: https://getcoldturkey.com/features/ and
https://getcoldturkey.com/support/user-guide/

### Freedom

Mac, Windows, iOS, Android, Chromebook, plus a Chrome extension. Subscription.
Blocklists (preset lists or custom), Block Websites, Block Apps, Block The
Internet, sessions started now or scheduled, Recurring Schedules, Allow-only
sessions (desktop), Locked Mode, Focus Sounds, cross-device sync. Locked Mode
prevents removing items from an active blocklist, deleting devices or editing
the timezone, and limits "End Session" to once in any 7 day period; it cannot
be turned off while any session is active. Freedom also publishes Pause, a
free standalone extension that shows a calming green screen for five seconds
(adjustable) before a site from its list of 50 distracting sites opens, with
"continue", "stay paused" and "close tab" choices.

Sources: https://freedom.to/features ,
https://support.freedom.to/en/articles/1802927-locked-mode ,
https://support.freedom.to/en/articles/3149199-how-to-use-pause and
https://freedom.to/blog/introducing-pause-a-chrome-extension-for-intentional-browsing/

### Intention (getintention.com)

Chrome and Firefox. When you open a listed site a dark full-page screen asks
how long you want to unlock distracting sites: "Unlock for 1m" or "Custom"
minutes. The toolbar icon shows the remaining time. When it runs out the tab
is locked with "Close Tab" or another "Custom" request. Options: a Sites list
(YouTube, Facebook, Twitter, Instagram and Reddit are suggested), "Set
scheduled hours" (for example weekdays 9 to 5), a daily limit across all
distracting sites, Stats of visit counts, and a streak for each day you stay
under the limit. Enforcement is a content script overlay plus a background
timer. Last updated 2021.

Sources: https://www.getintention.com/ ,
https://addons.mozilla.org/en-US/firefox/addon/intention/ and
https://gigazine.net/gsc_news/en/20200423-intention-addon-chrome-firefox/

### one sec

iOS, Android, macOS, plus Chrome, Firefox and Safari extensions. Instead of
blocking, an intervention runs before the app or site opens, then you choose
"don't open" or "continue". Intervention types: Breathing Exercise
(default), Minimal breathing, 4-7-8 breathing, Follow the Dot (10 seconds),
Black Screen (10 seconds), Rotate Phone (three rotations), Mirror (front
camera, look at yourself), Conversational reflection (why are you opening
this), Type random text, and task integrations (Lengo language practice).
Options: intervention duration, custom phrase, re-intervention after N
minutes inside the app, "intentional tab switching" that pauses interventions
for x minutes on desktop, doomscroll detection, time limits and open limits,
scheduled focus, distraction-free mode (hard block), attempt statistics
("you reached for Instagram 23 times today"), and healthy alternative
suggestions. The browser extension adds an adult content filter.

Sources: https://one-sec.app/ and
https://tutorials.one-sec.app/en/articles/3310978

### reflect (getreflect.app)

Chrome and Firefox. Open source, free. On a blocked site it asks "what is
your intention?". A small on-device ML classifier trained on survey data
judges the answer; "the algorithm prefers longer and more specific answers
oriented around some kind of work or just taking a break", and there are no
magic phrases. A valid answer whitelists the site for a set period; blocked
exact URLs and subdomains are treated separately since 1.4.0; keyboard
shortcut Ctrl+Shift+O toggles blocking. Everything is local.

Sources: https://getreflect.app/ and https://github.com/getreflect/reflect-chrome

### Opal

iOS, macOS, Android, with website blocking in Safari. Subscription. Focus
sessions (manual or Smart Schedules) with three difficulty levels: Normal
(cancel any time), Timeout ("increasing delays before you can take another
break"), Deep Focus (cannot be ended early; deleting the app does not lift
the block; Pro only). App Limits (daily time caps) and open limits (how often
you may open an app), Whitelist mode (Pro), Opal Score, Gems, streaks,
milestones, leaderboards and co-working sessions, weekly reports of pickups
and productive versus distracting time.

Sources: https://makeheadway.com/blog/opal-app-review/ and
https://www.blok.so/resources/opal-app-review-is-it-worth-100-year-for-screen-time-management

### ScreenZen

iOS, Android, macOS, Windows. Free, donation supported. Per app or site: a
wait time before opening (a few seconds to minutes, optionally increasing
each open), a number of opens allowed per day, a session length after which
the app locks, a cooldown before the next open, a customisable prompt such as
"what are you seeking?", breathing or an activity checklist during the wait,
daily limits with automatic blocking at the limit, schedules by day of week
and time, streaks and statistics, a passcode and a lock mode that stop you
from raising limits mid-session, website and adult content blocking, and an
optional black and white colour filter.

Sources: https://screenzen.co/ and https://nibble-app.com/blog/screenzen

### Control Panel for Twitter (insin)

Chrome, Firefox, Edge, Safari, mobile browsers. Open source. Applied in-page
with a script that watches X's SPA navigation. Options by section:

- Home timeline: default to "Following" and switch back whenever X moves you
  to "For you"; hide the "For you" tab; move Retweets and Quote Tweets to
  separate tabs or hide them; hide Retweets in pinned Lists; hide tweets
  quoting blocked or muted accounts; hide the floating "See new Tweets"
  button; hide "Who to follow" and topic suggestions; full-width timeline.
- Remove algorithmic content: hide "What's happening" and "Topics to follow",
  hide Explore page contents, hide "Discover more" tweets.
- Reduce "engagement": hide metrics, reduced interaction mode (hides reply,
  repost and like actions), disable home timeline access.
- Reduce Premium: replace or hide checkmarks, hide Premium replies and
  upsells, hide Grok, hide Subscriptions.
- UI: hide Views, restore headlines under links, default to "Latest" in
  search, fast blocking, hide bookmark and share buttons, hide analytics
  links, hide navigation items, hide the Messages drawer, hide "Open app"
  nags on mobile, font and density tweaks.

Sources: https://soitis.dev/control-panel-for-twitter and
https://addons.mozilla.org/en-US/firefox/addon/control-panel-for-twitter/

### Minimal Theme for Twitter / X (Typefully)

Chrome, Firefox, Safari. Open source. CSS-first declutter: remove Promoted
posts and suggestions, remove "Who to Follow", remove the trends sidebar,
remove the "For you / Following" timeline tabs, default to "Following", hide
the sticky timeline header, hide view counts and vanity counts under tweets,
hide the Search Bar, hide the Tweet button, hide Grok, customise the left
navigation, timeline width and borders, and Writer Mode, which hides the
whole interface except the composer. No timers.

Sources: https://addons.mozilla.org/en-US/firefox/addon/minimaltwitter/ and
https://github.com/typefully/minimal-twitter

### Grayscale extensions (Monochromate, Grayscale Screen, Grayscale Website Filter, GrayDay, Grey-Session)

All apply `filter: grayscale()` to the document (some via CSS injection, some
via the page's root style). Options seen across them: intensity from 0 to
100 percent (Monochromate), a scheduler that turns grayscale on during chosen
hours (Monochromate "Smart Scheduler", GrayDay, Grayscale Screen), blacklist
mode (grayscale only the listed sites) or whitelist mode (colour only the
listed sites) (Grayscale Website Filter, Monochromate "Site Exceptions"),
fullscreen support, hotkey toggle, and settings export. Grey-Session and
Grayscale Website Filter ship with YouTube, Facebook, Instagram, Twitter and
TikTok pre-listed. The rationale they all cite is that colour drives the
reward response and grayscale makes feeds feel flat.

Sources: https://monochromate.lirena.in/ ,
https://chromewebstore.google.com/detail/grayscale-website-filter/mhmhckgplokapaholpkmadihdidaiohe ,
https://chromewebstore.google.com/detail/grayscale-screen/mccgphkdghdpebkjkokhbodbjobpihal and
https://community.humanetech.com/t/chrome-extension-to-turn-distracting-websites-grayscale/5289

### DeArrow (ajayyy)

Chrome, Firefox, Safari, Android. Open source, crowdsourced. Replaces clickbait
titles and thumbnails on YouTube with user-submitted and voted alternatives.
When nothing has been submitted it reformats the original title to Title
Case or Sentence case and shows a screenshot from a random timestamp
(avoiding SponsorBlock segments) instead of the uploaded thumbnail; both
fallbacks can be turned off. A "show original" button peeks at the real
title and thumbnail. Thumbnails come from a cached generation service or are
rendered locally. Enforcement is in-page DOM replacement; it needs a network
call per video to the DeArrow API.

Sources: https://github.com/ajayyy/DeArrow and https://dearrow.ajay.app/

### Others worth knowing

- Hacker News `noprocrast`: a profile setting on the site itself with
  `maxvisit` (minutes you may stay, default 20) and `minaway` (minutes you
  must stay away, default 180). It is the oldest example of the "session
  then cooldown" model. https://news.ycombinator.com/item?id=814695
- Pause by Freedom: covered under Freedom above. A five second green screen
  with a "stay paused" option is the simplest possible intervention.
- sanjeed5/intentional: a small open source extension that pauses before a
  distracting site and asks why you are going there, close to reflect without
  the ML. https://github.com/sanjeed5/intentional

## 2. Feature catalogue

Marks: Have (already in Anti-Brainrot), Partial (exists in a narrower form),
Add (worth doing next, with the reason), Skip (out of scope or conflicts with
the design). The Anti-Brainrot column names the setting or file where the
feature lives.

### (a) YouTube specific hiding

| Feature | Seen in | Anti-Brainrot | Mark |
|---|---|---|---|
| Hide home feed, redirect logo to Subscriptions | Unhook, DF Tube, Control Panel, NFE | `homeFeed`, `redirectHome` | Have |
| Hide sidebar, recommended, live chat, playlist, fundraiser | Unhook, DF Tube, Control Panel | `sidebar` and children | Have |
| Hide end screen feed and cards | Unhook, DF Tube, Control Panel | `endScreenFeed`, `endScreenCards` | Have |
| Hide Shorts everywhere and redirect `/shorts/ID` to `/watch` | Control Panel, UnTrap, StayFocusd | locked `shorts` plus DNR rule | Have, and unconditional |
| Hide comments, avatars | all YouTube tools | `comments`, `commentAvatars` | Have |
| Hide mixes, merch, video info, buttons, channel, description | Unhook, Control Panel | `mixes`, `merch`, `videoInfo` and children | Have |
| Hide header, notifications, explore, More from YouTube, Subscriptions | Unhook | `topHeader`, `notifications`, `explore`, `moreFromYouTube`, `subscriptions` | Have |
| Hide inapt search shelves, filter chips, posts, news and games shelves | Unhook, Control Panel | `inaptSearch`, `chips`, `richSections` | Have |
| Disable autoplay and annotations | Unhook, DF Tube, Control Panel | `autoplay`, `annotations` | Have |
| Hide thumbnails | UnTrap, ImprovedTube, DF Tube | `thumbnails` | Have |
| Blur or grayscale thumbnails only | UnTrap, ImprovedTube | whole-site `grayscale` only | Add: a blur variant keeps titles readable while removing the clickbait pull, one CSS rule |
| Hide view counts, likes, durations | Control Panel "Hide metrics", ImprovedTube | `metrics` | Have |
| Hide search suggestions | UnTrap | `searchSuggestions` | Have |
| Category allow list (educational only) | none of the surveyed tools | `educational` | Have, unique |
| Channel allow list | Cold Turkey channel targeting, Control Panel Hide channels | `educational.allowedChannels` | Have (allow direction only) |
| Channel or keyword block list, regex | UnTrap content filter, Control Panel Hide channels, LeechBlock keywords | none | Add: the mirror of the allow list; hides listed channels or title keywords in feeds and blocks their watch pages, reusing the watch-page gate |
| Hide watched videos above N percent | Control Panel | none | Skip: convenience, not friction |
| Hide live, streamed, upcoming, members-only, low view videos | Control Panel | none | Skip: tidy-up options, not distraction |
| Hide AI summaries, Premium upsells, info panels | Control Panel | none | Skip |
| Stop Shorts looping, Shorts metadata on hover | Control Panel | not applicable, Shorts never render | Skip |
| Opening delay before YouTube itself | UnTrap Opening Delays, one sec, Pause | none; YouTube is not a distraction host | Add: an optional pause page for `youtube.com` watch pages outside educational mode, with the same pass and budget as other hosts |
| Watch-time tracking per day and week | ImprovedTube, StayFocusd dashboard | none | Add (local only): a per-day minutes counter shown in the popup gives the friction timer a reason; no network, no history |
| Crowdsourced de-clickbait titles and thumbnails | DeArrow | none | Skip: needs a third-party API per video, conflicts with "no network calls of its own" |

### (b) Cross-site distraction controls

| Feature | Seen in | Anti-Brainrot | Mark |
|---|---|---|---|
| Block list of hosts with paths and wildcards | LeechBlock, BlockSite, Cold Turkey, Freedom | `distractions.custom` with host, host/, host/path grammar | Have (no wildcards) |
| Preset lists per platform, feed surfaces separate from utilities | Freedom presets, NFE per-site, Pause list | `PRESETS` (feed-only and whole-site variants) | Have |
| Exceptions inside a blocked prefix | LeechBlock `+`, Cold Turkey exceptions, StayFocusd allowed pages | adult blocker has `allowedDomains`; distractions have none | Add: `netflix.com/browse` needs `netflix.com/browse/my-list` allowed and `reddit.com/r/x` needs `/comments/` allowed; one `+pattern` line and a priority 3 allow rule |
| In-page feed removal that leaves the rest of the page | NFE, Control Panel for Twitter, Minimal Twitter | URL-level only | Partial. Add later for X (`/home` tabs are in-page state) and Facebook groups feed, which URL patterns cannot split; otherwise URL blocking is cheaper and cannot be out-selected |
| Pause or intention screen with countdown | Pause, one sec, Intention, reflect, LeechBlock delay, ScreenZen | `mode: pause`, `pauseSeconds`, `intention` | Have |
| Timed pass after the pause | Intention, reflect, LeechBlock "limit access after countdown", ScreenZen session | `passMinutes` with alarm and return to pause page | Have |
| Daily budget of pass minutes | Intention daily limit, StayFocusd Max Time Allowed, Cold Turkey Allowance, LeechBlock time limit | `dailyBudgetMinutes`, pass-based accounting | Have (charged per pass, not per minute on site) |
| Count only the active or audible tab | LeechBlock, Cold Turkey allowance | not applicable while passes are charged up front | Skip unless accounting moves to time on site |
| Schedules or locked hours | StayFocusd Active Hours, LeechBlock periods, BlockSite Schedule, Freedom Recurring, Intention schedule, ScreenZen | `schedule` (days, start, end) forces the filter on | Have (one global window) |
| Multiple schedules or per-list schedules | LeechBlock block sets, Cold Turkey per block | one window | Skip for now: one window covers the study day; revisit if users ask |
| Nuclear option or lockdown for N hours, cannot be cancelled | StayFocusd Nuclear, LeechBlock Lockdown, Cold Turkey Timer lock, Freedom Locked Mode, Opal Deep Focus, UnTrap Temporary Focus Mode | only the recurring schedule | Add: a "Lock for N hours" button in the popup that sets a deadline the switch refuses until; reuses `isLockedNow` and the LockedError path |
| Allow-only or whitelist mode | StayFocusd Nuclear (allowed only), BlockSite Whitelist, Freedom allow-only, Cold Turkey allow-only | none | Add (behind locked hours): "during locked hours only these sites open" turns the block list inside out for exam weeks; the DNR rule set is small |
| Grayscale the site | grayscale extensions, LeechBlock filter, ScreenZen colour filter, UnTrap | YouTube `grayscale`; distraction hosts only during a pass | Add: an "always grayscale distraction hosts" checkbox, since the CSS is already applied by the watcher |
| Grayscale on a schedule | Monochromate, GrayDay | none | Skip: locked hours plus always-grayscale covers it |
| Quotes on the block page | NFE, Cold Turkey block message | none | Add: a user-written line ("why I turned this on", shown on the pause and block pages) is the cheapest commitment device and needs no quote corpus |
| Custom block page or redirect to a URL | BlockSite, LeechBlock, Cold Turkey | fixed block page | Skip: a redirect to "a productive site" is a new tab away from being ignored |
| Usage stats and insights | BlockSite Insights, LeechBlock statistics, StayFocusd dashboard, one sec attempts, Intention stats | none | Add (local only): blocks per day, passes used, minutes of pass time, shown in options; keep no history beyond seven days |
| Streaks | Intention, Opal, ScreenZen | none | Add: days under budget, computed from the same counters; one number in the popup |
| Password to change settings | LeechBlock, BlockSite, Cold Turkey, UnTrap, ScreenZen | none; the unlock delay replaces it | Skip: the design chose waiting over secrets; a password a friend holds is covered by "Add" under (c) if wanted |
| Prevent uninstall or block the extensions page | BlockSite, StayFocusd Nuclear, LeechBlock, Cold Turkey | none | Partial. MV3 cannot prevent uninstall. Blocking `chrome://extensions` during locked hours needs the `tabs` permission and a tabs.onUpdated redirect; low value for the permission cost |
| Sync, export and import of settings | LeechBlock, UnTrap, BlockSite | `chrome.storage.sync` | Partial. Add export and import as JSON in options; cheap and helps bug reports |
| Adult content filter | one sec extension, BlockSite Bulk Adult Block, ScreenZen | `adultSites` with bundled 15,000 domain list | Have |
| Pomodoro or focus timer | BlockSite Focus Mode, Cold Turkey Pomodoro, StayFocusd Focus Timer | none | Skip: a timer app, not a blocker |
| Slack integration, focus sounds, co-working | BlockSite, Freedom, Opal | none | Skip |

### (c) Friction and commitment mechanics

| Mechanic | Seen in | Anti-Brainrot | Mark |
|---|---|---|---|
| Delay before loosening (countdown you must sit through) | LeechBlock delay page, UnTrap Opening Delays, Pause | unlock delay 30 s to 1 h that only runs while the popup is open; closing the popup cancels | Have, and stricter than any surveyed tool |
| Countdown cancels on focus loss | LeechBlock "cancel countdown when tab loses focus" | popup countdown: yes; pause page countdown: no | Add: cancel the pause page countdown on `visibilitychange`, otherwise a background tab counts down for free |
| Tighten any time, loosen only while off | none of the surveyed tools state it this way; Freedom Locked Mode and LeechBlock lockdown approximate it | `isLoosening` in settings.js | Have, unique |
| Type a paragraph or random text to change settings | StayFocusd Require Challenge, Cold Turkey Random text (1 to 5000 chars), LeechBlock random access code | none | Add as an alternative to the delay: "type this paragraph" is friction that works even when you have an hour to wait; keep the delay as the default |
| Password held by another person | LeechBlock password, BlockSite, Cold Turkey Password lock | none | Add as an optional third mode alongside delay and paragraph; store a hash, never the secret, and make the choice itself a loosening so it cannot be flipped while on |
| Limited number of overrides per day or week | LeechBlock temporary override limit, Freedom one End Session per 7 days | budget limits passes; the filter switch has no count limit | Add: "at most N unlocks per week" on the switch; the delay slows one unlock, the count stops the tenth |
| Session then cooldown | HN noprocrast (maxvisit, minaway), ScreenZen cooldown, one sec re-intervention | pass expires and returns to the pause page; the next pass is available immediately | Add: a cooldown after a pass (for example 30 min) before the pause page offers another; a one-line check in `canPass` |
| Escalating delays | Opal Timeout, ScreenZen increasing wait | fixed `pauseSeconds` | Add: double the pause for each pass taken today, reset at midnight; makes the tenth pass cost more than the first with no new UI |
| Open limits per day (count, not minutes) | ScreenZen, Opal open limits, one sec | minutes budget only | Skip: the budget already limits passes since each pass costs `passMinutes`; add a count only if pass length becomes variable |
| Hold-to-snooze instead of a click | NFE 3.0 | none | Skip: the countdown already prevents single-click loosening |
| Typed intention before entry | Intention, reflect, one sec conversational, ScreenZen prompt | `intention` (3 characters minimum) | Have. Consider raising the minimum to a sentence and echoing it on the pass badge |
| Machine judged intention | reflect | none | Skip: an on-device model for a sentence is heavy, and "no magic phrases" fails in practice |
| Breathing, mirror, rotate phone, black screen | one sec | none | Skip for the browser: the countdown fills the same role; a black screen is the countdown with the lights off |
| Warning before a pass ends | LeechBlock warning message, Intention badge timer | none | Add: a toast in the watcher script 30 s before expiry and a badge with the minutes left; avoids losing a half-written comment |
| Uncancellable lock that survives restart | StayFocusd Nuclear, Cold Turkey Timer and Restart locks, Opal Deep Focus | schedule survives restart; no ad hoc lock | Add: same item as "Lock for N hours" in (b) |
| Donate to buy a break | Cold Turkey Pause for a Cause | none | Skip: needs payments |

### (d) Other clever things

- LeechBlock's "count only tabs playing audio" is the right way to meter
  Twitch, Kick and YouTube if time-on-site accounting is ever added. Mark:
  note for later.
- LeechBlock's "delay then auto-load" is exactly the pause mode here; its
  "block only on first access" toggle (do not re-pause on every in-page
  navigation) explains why the watcher grants a pass per pattern rather than
  per URL. Mark: Have.
- Control Panel for Twitter's "switch back to Following whenever X moves you
  to For you" is a pattern for any SPA that resets state: react to the
  platform's own navigation event rather than polling. The watcher here polls
  `location.href` every 500 ms; listening to `history.pushState` via a
  main-world patch would be faster. Mark: consider.
- News Feed Eradicator's per-site "customise each site" sub-toggles (hide
  Stories but keep the feed, or the reverse) match the preset split here
  (Reels and Explore versus all of Instagram). Mark: Have.
- BlockSite's media counters (Shorts and Reels watched per day with separate
  limits) treat short video as its own budget. Shorts are already gone; a
  Reels counter would need in-page counting on Instagram. Mark: Skip.
- Freedom's Pause offers "stay paused" as a first-class button: keep the
  green screen up and the site blocked without closing the tab. Mark: Add a
  "Keep it blocked" button on the pause page that just goes back, mirroring
  "Keep it on" in the popup.
- one sec's attempt counter ("you reached for X 23 times today") is the
  single most persuasive statistic in this category and is a counter of
  block page loads. Mark: Add under stats.
- Cold Turkey's Allowance only counts time while the mouse moved in the last
  3 minutes, so a forgotten tab does not eat the budget. Mark: note for
  later.
- DeArrow's "show original" button is a good pattern for any replacement:
  never hide information you cannot get back with one click. Mark: note.
- ImprovedTube's Zen Mode and Minimal Twitter's Writer Mode are "hide
  everything but the thing I came for" presets. The equivalent here would be a
  "watch page only" preset that turns on sidebar, comments, video info and
  header hiding together. Mark: Add as a one-click preset in the popup.

### Suggested order

1. Lock for N hours (b) and cooldown after a pass (c): both are small changes
   to existing state and close the two biggest gaps against StayFocusd and
   LeechBlock.
2. Exceptions inside a blocked prefix (b): needed before Netflix and Reddit
   presets are truly usable.
3. Pause page countdown cancels on focus loss, warning before expiry, "Keep it
   blocked" button (c, d): polish of what exists.
4. Local counters, streak, attempt count (b, d): the popup gets a reason to be
   opened for something other than turning the filter off.
5. Escalating pause and weekly unlock limit (c).
6. Channel and keyword block list on YouTube (a).
7. Paragraph and friend-password modes (c), export and import (b), always
   grayscale (b), blur thumbnails (a), watch-page-only preset (d).

## 3. URL pattern table

Paths are for the desktop web app. "SPA" means the site navigates with
`history.pushState`, so network-level rules only catch the first load and the
in-page watcher must catch the rest. The current presets in
`extension/lib/distractions.js` are noted where they differ from the table.
Paths marked "mixed" are useful and distracting at once and belong in a
whole-site variant, not the feed-only preset.

| Platform | Brain rot surfaces | Utility parts to leave open | SPA | Notes |
|---|---|---|---|---|
| TikTok `tiktok.com` | `/` and `/foryou` (For You feed), `/following`, `/explore`, `/friends`, `/live`, `/search`, `/tag/x`, `/@user/video/ID` (scrolls on to the next video) | `/messages` and `/inbox`, `/upload` and `/tiktokstudio`, `/@user` profile grid (mixed), `/setting` | Yes | Preset blocks the whole host, which is right: every path scrolls into the feed |
| Instagram `instagram.com` | `/` (home feed), `/reels/`, `/reel/ID/`, `/explore/`, `/explore/tags/x/`, `/stories/user/ID/`, `/p/ID/` (mixed) | `/direct/inbox/` and `/direct/t/ID/`, `/username/` profiles (mixed), `/accounts/`, `/your_activity/`, `/create/` | Yes | Preset covers reels, reel, explore. Add `instagram.com/` (root only) and `instagram.com/stories` to the feed preset; DMs stay open |
| X `x.com`, `twitter.com` (redirects to x.com) | `/home` (For you and Following are in-page tabs on one path), `/explore` and `/explore/tabs/*`, `/i/trending/ID`, `/search`, `/i/grok`, `/i/communities`, `/i/timeline` | `/messages`, `/notifications`, `/i/bookmarks`, `/i/lists`, `/compose/post`, `/settings`, `/username` (mixed), `/username/status/ID` | Yes | Preset covers root, home, explore. Add `/search` and `/i/trending`. Root `/` redirects to `/home` when signed in |
| Reddit `reddit.com`, `old.reddit.com`, `new.reddit.com` | `/`, `/?feed=home`, `/r/popular`, `/r/all`, `/best`, `/hot`, `/new`, `/top`, `/rising`, `/r/sub/` listings and `/r/sub/(hot,new,top)` (mixed) | `/r/sub/comments/ID/slug` threads (where search lands), `/message/inbox`, `/chat`, `/user/name`, `/submit`, `/settings`, `/search` (mixed) | New Reddit yes; old.reddit.com is server rendered | Preset covers front, popular, all and sort paths. Host suffix matching already covers old and new subdomains. Subreddit listings stay open on purpose; an exception for `/comments/` would be needed if `reddit.com/r` were ever blocked |
| Facebook `facebook.com`, `web.facebook.com`, `m.facebook.com` | `/`, `/home.php`, `/?sk=h_chr`, `/watch`, `/reel/ID`, `/reels`, `/stories`, `/gaming`, `/videos`, `/groups/feed`, `/marketplace` (mixed) | `/messages/t/ID`, `/events`, `/groups/ID` (mixed), `/marketplace/item/ID`, `/notifications`, `/profile.php`, `/username`, `/settings`, `/friends` | Yes | Preset covers root, watch, reel, reels, stories. Add `/home.php`, `/gaming`, `/videos`, `/groups/feed`. Subdomains covered by suffix match |
| Threads `threads.com` (threads.net redirects) | `/` (For you), `/following`, `/search`, `/@user/post/ID` (mixed) | `/activity`, `/@user` profile (mixed), `/settings` | Yes | Preset blocks the whole host; a feed-only variant would be `threads.com/`, `threads.com/following`, `threads.com/search` |
| LinkedIn `linkedin.com` | `/feed/`, `/feed/update/urn:li:activity:ID` (single post with feed below), `/news/`, `/games/`, `/video/` | `/messaging/`, `/mynetwork/` (mixed), `/jobs/`, `/in/name/`, `/company/x/`, `/notifications/`, `/learning/`, `/school/` | Partly (Ember router; many links do a full load) | Preset covers root and `/feed`. Add `/news` and `/games`. Root `/` redirects to `/feed/` when signed in |
| Pinterest `pinterest.com`, country domains `pinterest.co.uk`, `pinterest.de`, `nl.pinterest.com` and so on | `/` (home feed), `/today/`, `/ideas/`, `/videos/`, `/search/pins/`, `/pin/ID/` (related pins scroll forever) | `/username/`, `/username/boardname/`, `/settings/`, `/pin-builder/`, `/business/` | Yes | Preset blocks the whole host. Country TLDs are separate hosts and need their own lines; `nl.pinterest.com` is covered by suffix match, `pinterest.de` is not |
| Twitch `twitch.tv`, `m.twitch.tv`, `clips.twitch.tv` | `/` (front page with autoplaying carousel), `/directory`, `/directory/all`, `/directory/category/x`, `/directory/following` (mixed), `clips.twitch.tv/*`, `/username/clip/ID` | `/username` (one channel, mixed), `/videos/ID`, `/username/videos`, `/messages`, `/settings`, `/subscriptions`, `/drops/inventory`, `dashboard.twitch.tv` | Yes | Preset blocks the whole host. A feed-only variant: `twitch.tv/`, `twitch.tv/directory`, `clips.twitch.tv` |
| Kick `kick.com` | `/` (front page), `/browse`, `/browse/categories`, `/browse/categories/x`, `/username/clips`, `/clips` | `/username` (one channel, mixed), `/video/ID`, `/username/videos`, `/dashboard`, `/settings` | Yes (Nuxt) | Preset blocks the whole host. Feed-only variant: `kick.com/`, `kick.com/browse` |
| 9GAG `9gag.com` | `/`, `/hot`, `/trending`, `/fresh`, `/top`, `/shuffle`, `/interest/x`, `/tag/x`, `/gag/ID` (comments plus infinite next posts) | `/u/username`, `/notifications`, `/settings` | Yes | Preset blocks the whole host; there is no utility side worth keeping |
| Imgur `imgur.com`, `i.imgur.com` | `/` (viral front page), `/hot`, `/top`, `/user/viral`, `/t/tag`, `/r/sub`, `/search`, `/gallery/ID` (mixed: a post plus the next-post rail) | `/a/ID` albums and `/ID` single images linked from elsewhere, `i.imgur.com/*` direct files (embedded across the web, must stay open), `/upload`, `/user/name`, `/account/settings` | Yes | Preset blocks the root page only, which is the right split. Never add bare `imgur.com` to a preset because `i.imgur.com` is a suffix match |
| Snapchat web `snapchat.com`, `web.snapchat.com`, `my.snapchat.com`, `accounts.snapchat.com` | `snapchat.com/spotlight` and `/spotlight/ID`, `/discover`, `/stories`, `/@user/spotlight/*`, `/p/*` public profiles and stories | `web.snapchat.com` (chat, camera), `snapchat.com/add/user`, `my.snapchat.com` (Memories), `accounts.snapchat.com`, `snapchat.com/download` | Yes | Preset blocks the whole host, which also kills chat on web.snapchat.com. Split into `snapchat.com/spotlight`, `snapchat.com/discover`, `snapchat.com/stories`, `snapchat.com/` |
| Bluesky `bsky.app` | `/` (Following and Discover tabs in-page), `/feeds`, `/profile/bsky.app/feed/whats-hot` (Discover), `/profile/x/feed/y` custom feeds, `/search`, `/hashtag/x`, `/explore` | `/notifications`, `/messages`, `/profile/handle` (mixed), `/profile/handle/post/ID`, `/settings`, `/lists`, `/starter-pack/*` | Yes | Preset blocks root only. Add `/feeds`, `/search`, `/hashtag`, and the `/profile/*/feed/*` shape, which needs a wildcard or a path segment rule the grammar does not have yet |
| Tumblr `tumblr.com`, `www.tumblr.com`, blog subdomains `name.tumblr.com` | `/`, `/dashboard`, `/dashboard/following`, `/dashboard/stuff_for_you`, `/dashboard/trending`, `/dashboard/your_tags`, `/explore`, `/explore/trending`, `/explore/today`, `/tagged/x`, `/search/x`, `/communities` | `/inbox`, `/messaging`, `/blog/view/name` and `/blog/name`, `/name` blog view, `/likes`, `/following`, `/settings`, `/new/*` post editor, `/edit/*`, `name.tumblr.com` themed blogs (mixed) | www.tumblr.com yes; blog subdomains are server rendered themes | Preset covers root and `/dashboard` (which also covers `/dashboard/*`). Add `/explore`, `/tagged`, `/search`, `/communities`. Blog subdomains are caught by suffix match for `tumblr.com/` root only, which is what you want |
| Netflix `netflix.com` | `/browse` (autoplaying billboard and rows), `/browse/genre/ID`, `/latest` (New and Popular), `/Kids`, `/browse/games`, `/search`, `/title/ID` (preview autoplay, mixed) | `/watch/ID` (the thing you chose), `/browse/my-list` (under the blocked prefix), `/YourAccount`, `/ManageProfiles`, `/viewingactivity`, `/browse/audio` | Yes for browse; `/watch` is a separate page load | Preset covers `/browse`, which also blocks My List. Add `/latest`, `/Kids`, `/search`, and an exception for `/browse/my-list` once exceptions exist |

Two grammar gaps the table exposes: a wildcard in the middle of a path
(Bluesky `/profile/*/feed/*`) and an exception line (Netflix
`+netflix.com/browse/my-list`). Both fit the LeechBlock syntax, which is the
most widely understood one in this category.

## 4. Sources

YouTube tools

- https://addons.mozilla.org/en-US/firefox/addon/youtube-recommended-videos/
- https://unhook.app/
- https://addons.mozilla.org/en-US/firefox/addon/untrap-for-youtube/
- https://gigazine.net/gsc_news/en/20250204-untrap-for-youtube/
- https://soitis.dev/control-panel-for-youtube
- https://improvedtube.com/
- https://addons.mozilla.org/en-US/firefox/addon/df-youtube/
- https://extpose.com/ext/mjdepdfccjgcndkmemponafgioodelna
- https://github.com/adrianbartnik/distraction-free-youtube-chrome-extension
- https://github.com/ajayyy/DeArrow
- https://dearrow.ajay.app/

Cross-site blockers and feed removers

- https://github.com/jordwest/news-feed-eradicator
- https://addons.mozilla.org/en-US/firefox/addon/news-feed-eradicator/versions/
- https://deepwiki.com/jordwest/news-feed-eradicator
- https://www.cisdem.com/resource/stayfocusd-review.html
- https://tooltivity.com/extensions/stayfocusd-website-blocker-and-focus-timer
- https://mindfultechwork.com/stayfocusd-nuclear-option/
- https://www.techjunkie.com/stay-focused-chrome-extension-review/
- https://www.proginosko.com/leechblock/documentation/
- https://blocksite.co/
- https://addons.mozilla.org/en-US/firefox/addon/blocksite/
- https://chromewebstore.google.com/detail/blocksite/bggpghhpmdhmgpcaoncnjakckiflgecm
- https://getcoldturkey.com/features/
- https://getcoldturkey.com/support/user-guide/
- https://freedom.to/features
- https://support.freedom.to/en/articles/1802927-locked-mode
- https://support.freedom.to/en/articles/3149199-how-to-use-pause
- https://freedom.to/blog/introducing-pause-a-chrome-extension-for-intentional-browsing/

Intention and friction apps

- https://www.getintention.com/
- https://addons.mozilla.org/en-US/firefox/addon/intention/
- https://gigazine.net/gsc_news/en/20200423-intention-addon-chrome-firefox/
- https://one-sec.app/
- https://tutorials.one-sec.app/en/articles/3310978
- https://getreflect.app/
- https://github.com/getreflect/reflect-chrome
- https://github.com/sanjeed5/intentional
- https://makeheadway.com/blog/opal-app-review/
- https://www.blok.so/resources/opal-app-review-is-it-worth-100-year-for-screen-time-management
- https://screenzen.co/
- https://nibble-app.com/blog/screenzen
- https://news.ycombinator.com/item?id=814695

X and grayscale

- https://soitis.dev/control-panel-for-twitter
- https://addons.mozilla.org/en-US/firefox/addon/control-panel-for-twitter/
- https://addons.mozilla.org/en-US/firefox/addon/minimaltwitter/
- https://github.com/typefully/minimal-twitter
- https://monochromate.lirena.in/
- https://chromewebstore.google.com/detail/grayscale-website-filter/mhmhckgplokapaholpkmadihdidaiohe
- https://chromewebstore.google.com/detail/grayscale-screen/mccgphkdghdpebkjkokhbodbjobpihal
- https://community.humanetech.com/t/chrome-extension-to-turn-distracting-websites-grayscale/5289

URL structures (spot checks; the rest of the table is from public URL
conventions of each site as of this date)

- https://techcrunch.com/2025/04/24/threads-officially-moves-to-threads-com-and-updates-its-web-app/
- https://bsky.app/profile/bsky.app/feed/whats-hot
- https://www.tumblr.com/alunclewe/624937994033479680/tumblr-dashboard-page-url-trick
- https://help.tumblr.com/knowledge-base/dashboard-tabs/
- https://www.snapchat.com/spotlight and https://www.snapchat.com/discover
- https://kick.com/browse/categories
- https://www.netflix.com/tudum/articles/netflix-secret-codes-guide
