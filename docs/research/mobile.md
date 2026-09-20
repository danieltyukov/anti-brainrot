# Android research: AppBlock and blocking techniques

Research for an Android version of Anti-Brainrot. Written 2026-09-20 from
public web sources only (no device testing). Where a claim is inferred rather
than stated by the source, it says so. URLs are given inline and collected at
the end.

Scope: Part 1 is a feature and enforcement audit of AppBlock. Part 2 covers
the implementation techniques on Android 8 to 15, with the open-source apps
that use them. Part 3 maps the Chrome extension's features to an Android app.

## Part 1: AppBlock (MobileSoft)

### Identity

- Store title: "AppBlock - Block Apps & Sites", package `cz.mobilesoft.appblock`,
  developer MobileSoft s.r.o. (Hradec Kralove, Czech Republic, founded 2012).
  Latest version seen: 7.20.0, updated 17 September 2026, requires Android 9.0
  or newer.
- Reach: over 10 million installs and about 700,000 monthly active users by
  2025, around 20 percent of them paying (Wikipedia). A third-party write-up
  quotes a 4.7 star average over roughly 238,000 Play reviews (Habit Doom).
- Platforms: Android, iOS and iPadOS, watchOS, plus Chrome, Edge and Brave
  extensions.

Sources: https://apkpure.com/appblock-block-apps-sites/cz.mobilesoft.appblock,
https://en.wikipedia.org/wiki/AppBlock,
https://habitdoom.com/blog/android-app-blocker-that-cant-be-bypassed

### Permissions it asks for (Android)

The setup guide lists, with its stated reasons:

- Usage access: "so AppBlock can detect which apps you're using".
- Display over other apps: "so the block screen can appear on top".
- Accessibility: "needed for website blocking in browsers and Strict Mode".
- Location: "only if you want to use location or Wi-Fi schedules".
- Battery optimization off: "so it can run reliably in the background".
- Autostart: "so AppBlock starts automatically after a reboot" (some OEMs).

Not in the setup guide but documented elsewhere: Device admin (the uninstall
article says AppBlock "registers as a device administrator, which prevents
standard uninstallation"). Notification blocking is not tied to a named
permission on the help site; on Android it can only be done with Notification
access (a NotificationListenerService), so that is an inference.

Sources: https://appblock.app/how-to-set-up-appblock-on-android/,
https://mobilesoft.freshdesk.com/support/solutions/articles/48001268949-i-am-not-able-to-uninstall-appblock

### Feature list with tier and enforcement

Tier comes from the Android column of the pricing page
(https://appblock.app/premium/) unless noted. Where the help site contradicts
itself the discrepancy is stated.

App blocking

- "Block apps" via Quick Block or Schedules. Free: up to 5 blocked items
  (apps plus websites or keywords) per schedule and 3 schedules; Premium:
  unlimited. Enforced by watching the foreground app through Usage access and
  drawing a block screen with the Display over other apps permission (the two
  permissions the setup guide names for exactly this).
- "Allowlist Blocking mode" (Premium): block everything except selected apps;
  websites are excluded from allowlists.
- "Add newly installed apps" (Premium): apps installed after the toggle is set
  are added to that schedule's blocklist automatically.
- "Block reinstalled applications": a Strict Mode option that keeps a block on
  an app the user uninstalls and reinstalls.
- "In-app purchase blocking" (listed as free on the pricing page).

Website blocking

- "Block websites" and "Keywords blocking" (match "In domain" or "Anywhere in
  URL"). Free: 3 websites or keywords in Quick Block, 5 per schedule.
- Enforcement: the Accessibility service reads the URL bar of a supported
  browser and shows the block screen. The limitations page is explicit: "If a
  browser isn't on our supported list, AppBlock can't read its URLs",
  "Browser updates can occasionally break detection", "In-app browsers
  (inside social media or messaging apps) may not always be blocked", "A page
  may flash briefly before the block screen appears. This is normal
  Accessibility-based behavior", and "Incognito or private browsing in
  unsupported browsers may bypass blocking".
- Supported browsers: Chrome, Chrome Beta, Firefox, Firefox Beta, Microsoft
  Edge, Opera, Opera Beta, Opera Mini, Opera Mini Beta, Opera Touch, Huawei
  Browser, UC Browser, UC Mini, UC Turbo, CM Browser, Dolphin, Ecosia, Puffin,
  Maxthon Browser, Mint Browser, Via, Kiwi Browser, Nox, DuckDuckGo, Bromite,
  Spin Browser. Incognito statistics only for Chrome, Microsoft Edge and
  Brave.
- "Block unsupported browsers" (Premium): any browser not on the list is
  blocked as an app. The alternative is to block the browser app yourself.

Sources: https://appblock.app/which-browsers-are-supported-by-appblock-2/,
https://appblock.app/what-are-the-limitations-of-website-blocking-on-android/,
https://appblock.app/how-to-block-websites-on-android/

Adult content

- "Porn sites blocking" (the how-to article calls it Premium; the pricing page
  lists "Adult content blocking" under the free tier, so the tier has
  probably changed over time). Works "in all browsers (based on our database
  of porn sites)", meaning all supported browsers, by the same URL reading
  mechanism. Combined with keyword blocking for betting or gambling terms.

Source: https://appblock.app/how-to-block-adult-betting-or-distracting-websites-on-android/

Content blocking inside apps

- "Content blocking" (from version 7.5.0): Instagram Reels and Stories,
  YouTube Shorts, Snapchat Stories and Spotlight, WhatsApp Channels and
  Statuses. "Content blocking inside apps requires the Accessibility
  permission." The rest of the app stays usable. Applied through Quick Block
  or Schedules. Tier not stated.

Source: https://appblock.app/how-to-block-instagram-reels-youtube-shorts-or-snapchat-stories-on-android/

Schedules and conditions

- "Schedules" with five condition types that can be combined in one schedule:
  "Time" (days and hours), "Location" (radius around a point, with an inverse
  option that blocks outside the area, plus a setting to keep blocking when
  location is unavailable), "Wi-Fi" (block while connected to a chosen
  network), "Usage limit" (daily total, for example 30 minutes per day, or
  hourly, for example 10 minutes per hour; one limit per app), "Launch count"
  (opens per day or hour regardless of duration).
- Free limits: 3 schedules, 2 intervals per schedule, launch count max 15,
  usage limit up to 30 minutes per day. Premium: unlimited, plus "Schedule:
  Pause" (15 minutes, 1 hour, until end of day, or custom) and schedule
  duplication.
- Schedule locks: "Time lock" and "Charger lock".
- Per schedule toggles "Launch" and "Notifications", so a schedule can block
  only notifications.

Sources: https://appblock.app/how-schedules-work-in-appblock-2/,
https://appblock.app/how-usage-limits-work-on-android/,
https://appblock.app/how-to-create-a-location-schedule/,
https://appblock.app/how-can-i-block-only-notifications-from-selected-apps/

Quick Block, timer, Pomodoro

- "Quick Block": apps, websites and keywords, started with one tap. Free: 3
  apps plus 3 websites or keywords. "Quick Block Timer" presets 25 minutes,
  1 hour, 24 hours or custom (Premium on the how-to page; the pricing page
  lists "Quick Block Timer" as free and "Quick Block Timer & Pomodoro" as
  Premium). "Pomodoro" alternates blocking and breaks for a set number of
  sessions (Premium). Can be stopped early unless Strict Mode is active. A
  Quick Settings tile starts or stops it from the shade.

Source: https://appblock.app/how-to-use-quick-block-2/

Strict Mode

- "Strict Mode": free up to 24 hours, Premium unlimited with all access
  conditions and "Strict Mode Timer extension".
- Access methods (what you must do to turn it off): "PIN", "Charger" (stays
  on until the phone is plugged in), "Timer", "Cooldown" (an unlock request
  takes effect only after a waiting period), "Follow schedules" (active only
  while schedules run), "Approval" (a trusted person approves by email), and
  the combinations Timer and PIN, Timer and Charger, Follow schedules and
  PIN, Follow schedules and Charger.
- Options: "Lock blockings in app" (always on: no editing, pausing or
  deleting blocks), "Disable AppBlock uninstalling", "Block device settings"
  ("prevents access to device Settings"), "Block recent apps" (Pixel, Samsung
  and Xiaomi only, against the pop-up view and floating window bypass),
  "Block split screen", "Block reinstalled applications".
- Enforcement: uninstall protection is Device admin (the user must first
  deactivate the admin, which the app blocks by blocking Settings). Blocking
  Settings, recents and split screen is done by the Accessibility service
  (the setup guide says Accessibility is needed for Strict Mode). Support
  notes that if Block device settings is on and the access method is lost,
  the user "may need professional support".

Sources: https://appblock.app/how-to-use-strict-mode-2/,
https://mobilesoft.freshdesk.com/support/solutions/articles/48001268949-i-am-not-able-to-uninstall-appblock

PIN

- "Launch PIN": a 4 to 8 digit code required to open AppBlock at all, set
  under Profile, Customize, General, PIN code. Separate from the Strict Mode
  PIN. Recovery by a one-time code.

Source: https://appblock.app/can-i-set-a-pin-code-to-protect-appblocks-launch-2/

Statistics

- "Insights": daily and weekly overview, most used apps and top 5 websites
  per browser, time split into Distractive, Neutral and Productive, longest
  time away from the phone, longest time in one app, pickups and unlocks,
  "Peak Time", "Balance". Premium: weekly trends, long-term progress,
  exclude system apps. Needs Usage access; website and incognito tracking use
  the Accessibility service, with separate toggles.

Source: https://appblock.app/a-guide-to-appblock-insights-2/

Widgets

- No home screen widget is documented on the help site. The only shortcut
  found is the Quick Settings tile for Quick Block.

Other

- Backup, "Academy" lectures (3 free), multi-device account, "Alternative App
  Icon" (iOS list), Chrome, Edge and Brave extensions.

### Common complaints

- The service gets killed. The help site has a whole page per OEM: Samsung
  App Power Monitor, Xiaomi MIUI battery saver and Autostart, Huawei
  Protected apps, OnePlus resetting battery optimization, Oppo, Vivo, Realme,
  Sony Stamina mode. A dedicated page "My Accessibility keeps turning off"
  says devices from Xiaomi, Huawei and Oppo "are aggressive about killing
  Accessibility services, especially after a restart or when battery saver
  kicks in". The fix is always the same: unrestricted battery, Autostart, pin
  in recents.
- Strict Mode bypasses. A Play review from August 2026 describes getting past
  Strict Mode by typing the long password the app itself displays, and the
  developer replied acknowledging the gap. Another review reports Strict Mode
  working for a day or two after each reinstall and then stopping.
- Website blocking is porous: page flash before the block, unsupported and
  in-app browsers, incognito.
- Lockout: with Block device settings and a forgotten PIN the user cannot
  even reach Settings to remove the device admin, and support has to help.
- The structural point made by Habit Doom: every Android blocker runs on an
  Accessibility service the phone's owner can switch off, so Strict Mode is
  friction, not prevention.

Sources: https://appblock.app/specific-phone-brands/,
https://appblock.app/my-accessibility-keeps-turning-off-what-can-i-do-2/,
https://habitdoom.com/blog/android-app-blocker-that-cant-be-bypassed

## Part 2: Implementation techniques on Android 8 to 15

### (a) Detecting the foreground app

Two options, and the mature apps use both.

AccessibilityService. Declare a service with `canRetrieveWindowContent` and
subscribe to `TYPE_WINDOW_STATE_CHANGED` (and `TYPE_WINDOW_CONTENT_CHANGED`
for in-app surfaces). Every event carries `packageName` and `className`.
Delivery is immediate. Pitfalls:

- Keyboard and system windows also raise `TYPE_WINDOW_STATE_CHANGED`. The
  input method (for example the Gboard package), System UI (notification
  shade, recents, volume panel) and system dialogs (package `android`) all
  produce events. Naive code sees the keyboard opening inside a blocked app
  as a switch to another app and drops the block. Fix: either restrict the
  service with `packageNames` (only the packages you care about, which is
  what Scroll Guard does: "Only packages enabled by the user are included in
  the service's event filter"), or resolve the foreground app from
  `getWindows()` instead of from the event. `AccessibilityWindowInfo` types
  distinguish `TYPE_APPLICATION`, `TYPE_INPUT_METHOD`, `TYPE_SYSTEM`,
  `TYPE_ACCESSIBILITY_OVERLAY` and `TYPE_SPLIT_SCREEN_DIVIDER`, so filtering
  to application windows removes the keyboard and shade.
- Split screen and freeform put two `TYPE_APPLICATION` windows on screen. Use
  `isFocused` first and `isActive` as fallback. Scrolless documents why:
  "Focus is checked first because Android can lag behind during Home and
  Recents gestures" and "During a Home gesture, Android may still call the
  old touched window 'active'." Blocking both halves is the safe rule, and
  AppBlock offers "Block split screen" precisely because a blocked app in one
  half can be driven from the other.
- Overlays. Your own block window is a `TYPE_APPLICATION_OVERLAY` or
  `TYPE_ACCESSIBILITY_OVERLAY` window; exclude it before deciding what is in
  front.
- Lifecycle. The service dies when the user force stops the app, when the
  process crashes, or when an OEM battery manager kills it; the system does
  not restart it and the user must re-enable it in Settings.
- Sideloaded APKs on Android 13 and newer cannot enable the service until
  the user goes through "Allow restricted settings" (see (b)).

UsageStatsManager. Needs the `PACKAGE_USAGE_STATS` special permission (no
runtime dialog; the user toggles "Usage access" in Settings). Poll
`queryEvents(lastQuery, now)` on a timer and track `ACTIVITY_RESUMED`
(API 29 and newer; `MOVE_TO_FOREGROUND` before that). A published
no-accessibility blocker polls every 900 ms from a foreground service and
keeps the last known package in memory because "queryEvents returns events,
not state": a quiet window returns nothing. Latency is about one second
versus near instant for the accessibility route, and it cannot see inside
an app (no view tree), so it detects apps, not Shorts. It does not trigger
the restricted settings gate and is not affected by the accessibility
service being switched off. TimeLimit and the no-accessibility blocker both
guard against strobing (an app fires several foreground events while
opening) and against the brief return of the previous app during a back
gesture.

Recommendation: use the accessibility service as the primary sensor (it is
required for Shorts and Reels anyway) and keep a UsageStats poller as the
fallback that still blocks whole apps when the accessibility service is off.

Sources: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService,
https://developer.android.com/reference/android/view/accessibility/AccessibilityWindowInfo,
https://developer.android.com/reference/android/app/usage/UsageEvents.Event,
https://dev.to/rexa/how-to-block-apps-on-android-without-an-accessibilityservice-5gog,
https://github.com/ngdathd/ForegroundActivity,
https://raw.githubusercontent.com/duartebarbosadev/Scrolless/HEAD/app/src/main/java/com/scrolless/app/accessibility/ForegroundAppWindow.kt,
https://raw.githubusercontent.com/undergroundairlines/sroll/HEAD/app/src/main/kotlin/app/scrollguard/services/ShortFormContentBlockerService.kt

### (b) Blocking an app

Three mechanisms, usually layered.

1. Global actions from the accessibility service.
   `performGlobalAction(GLOBAL_ACTION_BACK)` or `GLOBAL_ACTION_HOME`. Every
   open-source Shorts blocker uses BACK for an in-app surface (it returns to
   the feed) and HOME for a whole app (Scroll Guard for TikTok, Blockfy for
   TikTok and X). Cheap, no extra permission, but it is a nudge: the user can
   reopen instantly, so every app adds a cooldown (1 to 1.5 s) to stop the
   BACK loop from fighting the app's own navigation.

2. An overlay window. With `SYSTEM_ALERT_WINDOW` ("Display over other apps",
   granted through `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`) a service can
   add a full-screen `TYPE_APPLICATION_OVERLAY` window. TimeLimit does this:
   `TYPE_APPLICATION_OVERLAY` on Android 8 and newer, `FLAG_NOT_FOCUSABLE`,
   translucent format, and it checks `Settings.canDrawOverlays()` plus the
   AppOps mode before showing. An accessibility service can also add
   `TYPE_ACCESSIBILITY_OVERLAY` windows without that permission, and Android
   14 added `attachAccessibilityOverlayToWindow()` so the cover follows the
   app window; Scrolless uses this to cover only the video region ("attaches
   covers directly to app windows (Android 14+) or uses screen overlays as
   fallback"). Overlay caveats: Android 12 blocks touches that pass through
   an untrusted overlay with opacity above 0.8, but accessibility overlays
   are trusted; and Settings screens such as the device admin deactivation
   page call `setFilterTouchesWhenObscured()`, so an overlay cannot be used to
   confuse those.

3. A full-screen Activity. Start a translucent, `singleInstance` activity
   with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP |
   FLAG_ACTIVITY_SINGLE_TOP` (the no-accessibility blocker) or a normal
   activity that finishes to the launcher (Blockfy's InterruptActivity).
   Starting an activity from a service counts as a background activity
   launch. The documented exemptions include: the app has a visible window,
   the app "has the SYSTEM_ALERT_WINDOW permission granted by the user", the
   launch comes from a system PendingIntent (a notification tap), or the app
   holds `START_ACTIVITIES_FROM_BACKGROUND`. So holding the overlay
   permission is what makes the activity route work; without it the start
   is silently dropped. Android 14 made PendingIntent senders opt in, and
   Android 15 made PendingIntent creators opt in, which matters only if you
   trampoline through a notification. The alternative for the no-permission
   case is a notification with a full-screen intent, but on Android 14
   `USE_FULL_SCREEN_INTENT` is reserved for calling and alarm apps on Play;
   a sideloaded app keeps it, and the user can toggle it under Settings.

TimeLimit's sequence is the robust one: the accessibility service presses
Home ("to press the home button before showing the lock screen. This fixes
blocking in some cases"), an overlay covers the blocked app immediately, and
the lock activity starts on top of the launcher. Where device owner mode is
available (only via adb or provisioning) TimeLimit instead suspends the
package with `DevicePolicyManager.setPackagesSuspended`, and without device
owner "app suspension simply does not function".

Foreground service. The monitor must be a foreground service (persistent
notification). Android 14 requires a `foregroundServiceType`. The only
honest type for an app blocker is `specialUse` with the
`PROPERTY_SPECIAL_USE_FGS_SUBTYPE` property; Play reviews it, sideload does
not. `systemExempted` is allowed for device admin apps and for the app that
is the configured VPN, which is useful for the DNS service but not for the
monitor. Android 15 adds: a `BOOT_COMPLETED` receiver may not start
`dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection` or
`microphone` services (specialUse is fine), and the SYSTEM_ALERT_WINDOW
exemption for starting a foreground service from the background now also
requires a currently visible overlay window.

Package visibility. To offer an app picker on Android 11 and newer you need
`QUERY_ALL_PACKAGES` or a `<queries>` block; Play gates the former, sideload
does not.

Restricted settings for sideloaded APKs. Since Android 13, an app installed
by a non-session installer (a browser, mail or chat client handing the APK
to the system installer) cannot have its accessibility service or
notification listener enabled: the toggle is greyed out with "For your
security, this setting is currently unavailable". The user must (1) tap the
service once to see that dialog, (2) open Settings, Apps, the app, tap the
three-dot menu, choose "Allow restricted settings", (3) confirm with PIN or
biometrics, (4) go back and enable the service. The Esper write-up notes
"the system does not tell the user they can do this". Android 14 and 15 kept
the App info escape hatch; Android 15 added Enhanced Confirmation Mode,
which extends the gate to more special permissions for apps from untrusted
installers (Android Authority lists Accessibility, Notification listener,
Device admin, Display over other apps and Usage access). Installing through
a session-based installer (F-Droid, Obtainium, SAI, or your own updater) is
treated as a store install and skips the gate. The documentation for the
Android app must include these four steps with screenshots.

Sources: https://developer.android.com/guide/components/activities/background-starts,
https://developer.android.com/about/versions/14/changes/fgs-types-required,
https://developer.android.com/about/versions/14/behavior-changes-14,
https://developer.android.com/about/versions/15/behavior-changes-15,
https://developer.android.com/about/versions/12/behavior-changes-all#untrusted-touch-events,
https://developer.android.com/training/package-visibility,
https://codeberg.org/timelimit/opentimelimit-android (OverlayUtil.kt, AccessibilityService.kt, SuspendAppsLogic.kt),
https://f-droid.org/packages/io.timelimit.android.open/,
https://raw.githubusercontent.com/buenotty/Blockfy/HEAD/app/src/main/java/com/buenotty/blockfy/feature_monitor/InterruptActivity.kt,
https://www.esper.io/blog/android-13-sideloading-restriction-harder-malware-abuse-accessibility-apis,
https://www.androidauthority.com/android-15-restricted-settings-sideloading-3481098/,
https://dev.moe/en/3030,
https://www.xda-developers.com/android-13-restricted-setting-notification-listener/

### (c) Blocking Shorts, Reels and TikTok inside the apps

All of the open-source blockers work the same way: an accessibility service
filtered to the target packages receives `TYPE_WINDOW_STATE_CHANGED`,
`TYPE_WINDOW_CONTENT_CHANGED` and sometimes `TYPE_VIEW_SCROLLED`, walks the
node tree of the active window (breadth first, capped at 100 to 200 nodes),
looks for identifying `viewIdResourceName` values (the service needs
`FLAG_REPORT_VIEW_IDS`), content descriptions or activity class names, and
then presses BACK, clicks the Home tab, or covers the player with an overlay.

Identifiers seen in shipping open-source apps. "Corroborated" means two or
more independent projects use it; the rest are single-project candidates,
and one project (block-instagram-reels) explicitly warns that its own
candidates were "written from documentation and memory. Not one of them has
been read off a device."

YouTube (`com.google.android.youtube`; forks `app.revanced.android.youtube`,
kids `com.google.android.apps.youtube.kids`)

- `reel_player_page_container`: the Shorts page container. Corroborated
  (ShortsBlocker, Scrolless). The single strongest signal.
- `reel_watch_fragment_root` and the bottom bar `pivot_bar` (Blockfy).
- `reel_progress_bar` (Shorts-Blocker, Scroll Guard). Shorts-Blocker issue 74
  reports it missing on some devices and ROMs.
- `reel_recycler` (FocusBlock, AntiScroll), `reel_watch_player` (AntiScroll),
  `reel_watch_fragment_container`, `reel_video_player`, `shorts_container`
  (FocusBlock), `reel_player_page`, `shorts_shelf`, `shorts_player_controls`,
  `shorts_video_list` (AntiScroll), `shorts_player`, `reel_watch`,
  `reel_player` (Scroll Guard).
- Activity class names: `ReelWatchActivity`, `ReelPlayerActivity`
  (FocusBlock).
- Negative signals that mean a normal video: `watch_player`, `movie_player`,
  `time_bar`, `search_results_editor`, labels "enter full screen" and
  "chapters".
- Shorts tab in the bottom bar: the bar is `pivot_bar`; the tab is the child
  whose text or content description equals "Shorts" and whose `isSelected`
  is true (FocusBlock). Scroll Guard also tries `shorts_tab` and
  `pivot_shorts`. Text matching is locale dependent; match against the
  localized string for the device locale or against the id where present. To
  hide or neutralise the tab, click the Home tab node
  (`ACTION_CLICK` on the sibling with text "Home") rather than BACK.

Instagram (`com.instagram.android`)

- `clips_viewer_view_pager`: the Reels viewer pager. Corroborated by four
  projects (Scrolless, AntiScroll, FocusBlock, AwayDoomscrollin).
- `clips_tab` with `isSelected` true for the Reels tab; absence of `feed_tab`
  as a fullscreen viewer signal (Shorts-Blocker); `clips_swipe_refresh_container`
  and `feed_tab` (Blockfy); `clips_video_container`, `clips_viewer_root`,
  `clips_ufi_container`, `clips_viewer_fragment` (FocusBlock); `clips_player`,
  `clips_timeline`, `viewer_media_view_pager` (AwayDoomscrollin).
- Activity: `ClipsViewerActivity` is Reels; `ReelViewerActivity` is Stories.
  In Instagram's own naming "reel" means Stories and "clips" means Reels, so
  `reel_viewer_root`, `reel_viewer_layout`, `story_viewer_fragment_container`
  identify Stories, not Reels.
- Reels tab: node with content description "Reels" or containing "Reels tab"
  and `isSelected` true.
- Exemptions everyone needs: Reels shared inside a DM thread. Scrolless
  requires `sender_username_or_fullname`, `sender_timestamp` and
  `reply_bar_edittext` to be present and `suggested_title` absent before it
  treats the viewer as a DM; FocusBlock whitelists `direct_thread_feed`,
  `direct_inbox`.

Facebook (`com.facebook.katana`, `com.facebook.lite`)

- `fb_shorts_container` (Blockfy, FocusBlock), `reels_viewer` (Blockfy),
  `fb_shorts_viewer_fragment`, `reels_tab` (FocusBlock). Facebook ids are
  sparse, so Scrolless matches content descriptions
  `FbShortsComposerAttachmentComponentSpec_STICKER` and `_GIF`, a "Reels,"
  content description prefix on the selected tab, and a nested RecyclerView,
  Button, SurfaceView structure. Facebook Lite: `video_view`.

TikTok (`com.zhiliaoapp.musically`, `com.ss.android.ugc.trill`,
`com.ss.android.ugc.aweme`; Lite `com.zhiliaoapp.musically.go`)

- `player_view` for the feed player, `simplayer_api_player_view` for Lite
  (Scrolless, which covers the player region rather than pressing BACK).
- AwayDoomscrollin classifies TikTok screens by `bottom_tab` ids with
  `home`, `friends`, `for_you`, `foryou` prefixes plus localized labels (it
  ships English and Turkish strings), and counts action rail buttons (like,
  comment, share, favorite) to recognise the feed.
- Scroll Guard gives up on layout and blocks TikTok by package: "TikTok is
  blocked by package name and does not depend on its interface layout." For
  an app whose entire purpose is the feed, that is the right call.

Snapchat (`com.snapchat.android`): `spotlight_container` (Scrolless),
`spotlight_carousel`, `spotlight_fullscreen`, `spotlight_player`,
`spotlight_fragment` (FocusBlock).

How the projects react when ids change

- Silent failure is the default. Shorts-Blocker issue 74: the detector
  "relies entirely on one exact substring match with no fallback", so an app
  update that renames the id, or an OEM ROM where `viewIdResourceName`
  returns null, turns the blocker off with no log line.
- Scroll Guard scores several independent signals (ids, selected tab label,
  control labels such as "remix" and "use this sound", a vertical scrollable
  pager, at least three Shorts controls, minus normal player ids) against a
  threshold (6 for YouTube, 7 for Instagram) and ships a diagnostics view
  that prints the highest score, the threshold, the reasons and the safe ids
  so users can report a miss.
- Blokr (closed source) keeps the rules as JSON in a remote database so a
  rule change needs no release; its author says Instagram "changes their
  View IDs every week", which is an exaggeration for ids but true for
  layouts.
- block-instagram-reels refuses to ship guessed ids and documents an adb
  workflow instead: `adb shell uiautomator dump`, pull the XML from each
  surface (Shorts tab, fullscreen player, home feed with and without a
  shelf, a normal video) and diff them.

Practical rule: ship a rules file keyed by package and app version range,
prefer structural and label signals as a second vote, log a counter when
events arrive for a target package but nothing matches for N minutes, and
give users a one-tap "send hierarchy dump" that strips text.

Sources: https://raw.githubusercontent.com/yunussorkac/ShortsBlocker/HEAD/app/src/main/java/com/yeslab/shortsblocker/ShortsBlockService.kt,
https://raw.githubusercontent.com/duartebarbosadev/Scrolless/HEAD/core/domain/src/main/java/com/scrolless/app/core/model/BlockableApp.kt,
https://raw.githubusercontent.com/buenotty/Blockfy/HEAD/app/src/main/java/com/buenotty/blockfy/feature_accessibility/ReelsBlockAccessibilityService.kt,
https://raw.githubusercontent.com/shubh07o/FocusBlock/HEAD/app/src/main/java/com/focusblock/app/detector/ShortsReelsDetector.kt,
https://raw.githubusercontent.com/yadavnikhil03/AntiScroll/HEAD/app/src/main/java/com/antiscroll/app/AntiScrollAccessibilityService.java,
https://raw.githubusercontent.com/undergroundairlines/sroll/HEAD/app/src/main/kotlin/app/scrollguard/services/detectors/YouTubeShortsDetector.kt,
https://raw.githubusercontent.com/undergroundairlines/sroll/HEAD/app/src/main/kotlin/app/scrollguard/services/detectors/InstagramReelsDetector.kt,
https://raw.githubusercontent.com/atick-faisal/Shorts-Blocker/HEAD/app/src/main/kotlin/dev/atick/shorts/services/detectors/YouTubeShortsDetector.kt,
https://raw.githubusercontent.com/atick-faisal/Shorts-Blocker/HEAD/app/src/main/kotlin/dev/atick/shorts/services/detectors/InstagramReelsDetector.kt,
https://github.com/atick-faisal/Shorts-Blocker/issues/74,
https://raw.githubusercontent.com/ResolveCommunity/AwayDoomscrollin/HEAD/app/src/main/java/com/resolvecommunity/awaydoomscrollin/InstagramProtectionEngine.kt,
https://raw.githubusercontent.com/ResolveCommunity/AwayDoomscrollin/HEAD/app/src/main/java/com/resolvecommunity/awaydoomscrollin/TikTokSurfacePolicy.kt,
https://github.com/Moritz-Staat/block-instagram-reels/issues/13,
https://github.com/Moritz-Staat/block-instagram-reels/issues/40,
https://dev.to/vishal_pathak_209/i-reverse-engineered-youtube-to-delete-just-the-shorts-here-is-the-code-3b84

### (d) DNS-based site blocking with VpnService

The DNS66 pattern, still the reference:

- Call `VpnService.prepare()`; the system shows the VPN consent dialog once.
- `Builder.addAddress` with a documentation prefix that no real network
  uses. DNS66 tries `192.0.2.1/24`, then `198.51.100.1/24`, then
  `203.0.113.1/24`, and for IPv6 `2001:db8::/120` when an upstream has an
  IPv6 address. Blockfy uses `10.7.0.2/32` with the DNS server at `10.7.0.1`.
- For each upstream resolver, allocate a fake address in that subnet
  (`192.0.2.2`, `192.0.2.3`, ...), call `addDnsServer(fake)` and
  `addRoute(fake, 32)`. Nothing else is routed, so ordinary traffic never
  enters the tunnel and there is no throughput or battery cost beyond DNS.
- Read packets from the tun file descriptor, parse IPv4 or IPv6 plus UDP
  port 53, and answer locally. DNS66 answers a blocked name with rcode
  NOERROR, no answer records and an SOA in the authority section with a 5
  second negative TTL (a NODATA answer). NetGuard "ignores the IP addresses
  in the hosts file, because it does not route blocked domains to
  localhost". NXDOMAIN or an A record of 0.0.0.0 are the other two choices;
  NXDOMAIN gives the clearest browser error page, 0.0.0.0 makes some apps
  retry. Treat A and AAAA the same.
- Forward everything else on a `DatagramSocket` that you pass to
  `VpnService.protect()` so it leaves through the real network, to the real
  upstream mapped from the fake destination address (DNS66 indexes by the
  last byte). Blockfy hardcodes 1.1.1.1 with a 1.5 s timeout; better is to
  read the underlying network's resolvers from `LinkProperties`.
- `addDisallowedApplication` for your own package and for a list of banking
  apps that refuse to run under a VPN (Blockfy ships such a list), or
  `addAllowedApplication` to scope the filter to browsers only.

What bypasses it

- Private DNS (Settings, Network, Private DNS). AdGuard's compatibility
  notes: "before version Q, Private DNS didn't break AdGuard DNS filtering
  logic, but starting from version Q, the presence of Private DNS forces
  apps to redirect traffic through the system resolver". In the hostname
  (strict) mode the resolver talks DNS over TLS on port 853 to that host and
  never sends plain DNS to the VPN's server. NetGuard: "It is therefore not
  sufficient to disable Private DNS within Android, but you must also check
  the settings for DoT and DoH (especially for browsers)." The app should
  read `LinkProperties.getPrivateDnsServerName()` / `isPrivateDnsActive()`
  and tell the user to set Private DNS to Off (Automatic falls back to plain
  DNS when port 853 to the fake server does not answer). AdGuard's fallback
  for OEMs that hide the toggle is to blackhole the upstream addresses the
  system would use for DoT.
- Chrome's Secure DNS. By default it is "turned on in automatic mode", which
  only upgrades to DoH when the current resolver is a known provider; a fake
  192.0.2.x resolver is not, so automatic mode stays on plain DNS through the
  VPN. If the user picks a provider explicitly, Chrome sends DoH over HTTPS
  and the filter is blind. Chrome disables the feature entirely when "your
  device is managed or parental controls are turned on". Tell the user to
  keep "Use secure DNS" on automatic or off. Firefox has the same knob.
- Apps with their own resolver or hardcoded IPs. Only DNS is intercepted, so
  anything that connects to an address it already knows goes through. That
  is acceptable for site blocking; it is not a firewall.
- IPv6. If you add only an IPv4 fake resolver, the system still uses it for
  all apps (the VPN's DNS list replaces the network's). Add the IPv6 fake
  address as well so AAAA queries over an IPv6-only network do not fail, and
  answer AAAA for blocked names identically.

Keeping it alive

- It must be a foreground service (Android 8 and newer put a VPN app on a
  temporary allowlist and shut it down if it does not call
  `startForeground()` promptly). On Android 14 declare
  `foregroundServiceType="systemExempted"`, which is explicitly allowed for
  "VPN apps (configured via Settings > Network & Internet > VPN)", or
  `specialUse`.
- Declare `android.net.VpnService.SUPPORTS_ALWAYS_ON` metadata so the user
  can switch on Always-on VPN in Settings; the system then starts the service
  at boot and restarts it if it dies, and shows a non-dismissable
  notification while it is down. Do not recommend "Block connections without
  VPN" (lockdown) for a DNS-only tunnel: NetGuard warns "This will block
  resolving domain names too", because lockdown drops every packet that does
  not enter the tunnel and a DNS-only VPN routes almost nothing. RethinkDNS
  can support lockdown only because it routes all traffic.
- Handle `onRevoke()` (another VPN app was chosen) by stopping cleanly and
  telling the user. Reconnect with exponential backoff on network change
  (DNS66 doubles the retry timeout up to a cap).
- The same OEM battery managers that kill accessibility services kill VPN
  services; NetGuard adds a watchdog alarm every 10 to 15 minutes on Huawei
  and Xiaomi.

Sources: https://raw.githubusercontent.com/julian-klode/dns66/master/app/src/main/java/org/jak_linux/dns66/vpn/AdVpnThread.java,
https://raw.githubusercontent.com/julian-klode/dns66/master/app/src/main/java/org/jak_linux/dns66/vpn/DnsPacketProxy.java,
https://raw.githubusercontent.com/buenotty/Blockfy/HEAD/app/src/main/java/com/buenotty/blockfy/feature_vpn/AdultBlockVpnService.kt,
https://developer.android.com/develop/connectivity/vpn,
https://developer.android.com/about/versions/14/changes/fgs-types-required,
https://github.com/M66B/NetGuard/blob/master/FAQ.md,
https://github.com/M66B/NetGuard/blob/master/ADBLOCKING.md,
https://adguard.com/kb/adguard-for-android/solving-problems/compatibility-issues/,
https://support.google.com/chrome/answer/10468685?hl=en&co=GENIE.Platform%3DAndroid,
https://www.chromium.org/developers/dns-over-https/,
https://rethinkdns.com/faq,
https://github.com/IngoZenz/personaldnsfilter

### (e) Notification blocking

Declare a `NotificationListenerService` with permission
`BIND_NOTIFICATION_LISTENER_SERVICE` and the
`android.service.notification.NotificationListenerService` intent action;
send the user to `ACTION_NOTIFICATION_LISTENER_SETTINGS` to enable it. In
`onNotificationPosted` check `sbn.packageName` against the blocked set and
call `cancelNotification(sbn.key)`; `snoozeNotification(key, ms)` hides it
until the block ends instead of deleting it, which is the better semantic
for a pass system. Limits: ongoing notifications from foreground services
cannot be cancelled; the listener is subject to restricted settings on
sideloaded APKs exactly like the accessibility service (the Android 13 gate
covers "Accessibility settings and Notification Listener"); heads-up
notifications may flash before the cancel lands. TimeLimit additionally uses
notification access to detect and stop background media playback from
blocked apps. AppBlock's "Notifications" toggle per schedule is this
mechanism.

Sources: https://developer.android.com/reference/android/service/notification/NotificationListenerService,
https://www.xda-developers.com/android-13-restricted-setting-notification-listener/,
https://f-droid.org/packages/io.timelimit.android.open/

### (f) Strict mode techniques

What is still possible on Android 13 to 15, in order of strength:

1. Device admin. Register a `DeviceAdminReceiver` (`BIND_DEVICE_ADMIN`,
   `DEVICE_ADMIN_ENABLED` filter, policies XML) and activate it with
   `ACTION_ADD_DEVICE_ADMIN`. An active admin cannot be uninstalled until the
   user deactivates it in Settings; `onDisableRequested()` returns a warning
   string that Settings shows first, and `onDisabled()` fires afterwards so
   the app can log the attempt (TimeLimit logs `TriedDisablingDeviceAdminAction`).
   The password, camera and keyguard policies were deprecated in Android 9
   and throw in Android 10; `USES_POLICY_FORCE_LOCK` and
   `USES_POLICY_WIPE_DATA` remain, and you need neither. Restricted settings
   on Android 15 reportedly extends to device admin for untrusted installs,
   so document the App info step for it too.
2. Blocking the Settings pages that undo you. From the accessibility service,
   watch `TYPE_WINDOW_STATE_CHANGED` for package `com.android.settings` and
   press HOME (or show the block screen) when the class name is one of:
   `com.android.settings.applications.InstalledAppDetailsTop` (App info, the
   page with Uninstall and Allow restricted settings) when the window title
   or a node text equals your app label;
   `com.android.settings.Settings$AccessibilitySettingsActivity` and
   `com.android.settings.SubSettings` when the title is your service label
   (the per-service toggle page);
   `com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminAdd`
   (the deactivate page; Settings shows "Remove and uninstall device admin"
   there) and `Settings$DeviceAdminSettingsActivity`;
   `Settings$ManageApplicationsActivity` only if you want to be as blunt as
   AppBlock's "Block device settings". OEM Settings apps (Samsung, Xiaomi)
   use other class names, so match on window title as the second key and
   let users report misses. This is the same trick as AppBlock's Block device
   settings, and the same lockout risk: ship a recovery path (Cooldown, or a
   timer that expires) so a forgotten PIN never needs support.
3. Block recents and split screen: HOME on `com.android.systemui` recents
   windows (Pixel, and the Samsung and Xiaomi launchers, which is why
   AppBlock lists exactly those three).
4. Force stop, Safe mode and "Clear storage" cannot be prevented without
   device owner. Safe mode disables all third-party apps; the countermeasure
   is a boot receiver that notices it was not running and reports it. Device
   owner (`adb shell dpm set-device-owner`) lets TimeLimit suspend packages
   and block Settings for real, but it requires a factory-fresh or
   adb-provisioned device and is out of scope for a consumer sideload.

Sources: https://developer.android.com/guide/topics/admin/device-admin,
https://developers.google.com/android/work/device-admin-deprecation,
https://codeberg.org/timelimit/opentimelimit-android (AdminReceiver.kt),
https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/AndroidManifest.xml,
https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/src/com/android/settings/Settings.java,
https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/src/com/android/settings/applications/specialaccess/deviceadmin/DeviceAdminAdd.java,
https://appblock.app/how-to-use-strict-mode-2/

### (g) Alarms and schedules

- Exact alarms. `SCHEDULE_EXACT_ALARM` (Android 12) is denied by default for
  newly installed apps targeting API 33 or higher on Android 14; check
  `canScheduleExactAlarms()` and send the user to
  `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` ("Alarms & reminders" in Settings).
  Apps on the battery optimization allowlist are exempt. `USE_EXACT_ALARM`
  is a normal permission granted at install; only Play policy restricts it
  to alarm and calendar apps, so a sideloaded build can declare it and never
  ask. `setAlarmClock()` also needs the permission but is the one kind of
  alarm that wakes the device from Doze on time.
- Doze. Standard `setExact` and `setWindow` alarms are deferred to
  maintenance windows; `setAndAllowWhileIdle` and
  `setExactAndAllowWhileIdle` fire in Doze but "cannot fire more than once
  per 9 minutes per app"; `setWindow` needs a window of at least 10 minutes.
  Locked hours need a start and an end alarm, and a re-check at boot and on
  `TIME_SET` / `TIMEZONE_CHANGED`, since a clock change is a classic bypass.
- WorkManager: periodic work has a 15 minute minimum interval and a 5 minute
  minimum flex, and does not run in Doze. Fine for daily budget resets and
  rule updates, useless for a lock that must engage at 22:00.
- The reliable design used by TimeLimit and AppBlock is to not depend on
  alarms for enforcement at all: the foreground service evaluates the
  schedule on every foreground change and once a minute, and alarms only
  wake the process when the screen is off. Request
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (the docs list task automation apps
  as an acceptable use) and pin the OEM settings from AppBlock's brand page.

Sources: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms,
https://developer.android.com/develop/background-work/services/alarms,
https://developer.android.com/training/monitoring-device-state/doze-standby,
https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work

### (h) Sideloading an APK from GitHub releases

- Signing. Sign with `apksigner` using v1, v2 and v3 (v2 is mandatory for
  apps targeting API 30 or higher; v3 adds key rotation on Android 9 and
  newer; v4 only matters for `adb install --incremental`). Updates must be
  signed with the same key or the install fails, so the keystore is a
  permanent asset: back it up, keep it out of the repository, and give the
  certificate a validity past 2033. Publish the SHA-256 of the signing
  certificate in the README so users and Obtainium can verify it.
- Target SDK. Android 14 refuses to install anything targeting below API 23
  (`INSTALL_FAILED_DEPRECATED_SDK_VERSION`), which is irrelevant for a new
  app, but target the current API so the behaviour changes above apply
  predictably.
- The Chrome flow. Attach `anti-brainrot-<version>.apk` to a GitHub release.
  On the phone: tap the asset in Chrome, Chrome downloads it and shows an
  "Open" notification; the system package installer opens; the first time,
  Android 8 and newer shows "For your security, your phone is not allowed to
  install unknown apps from this source" with a Settings button that opens
  the per-app "Install unknown apps" toggle for Chrome (the global "Unknown
  sources" switch was removed in Android 8); back in the installer, Google
  Play Protect may show "App scan recommended" for an APK it has never seen
  and run a code-level scan before allowing the install; then Install. After
  that, first launch, then the restricted settings dance from (b) for the
  accessibility service and the notification listener, then Usage access,
  Display over other apps, VPN consent, Alarms and reminders, battery
  optimization. This is eight permission screens; the onboarding must walk
  them in order with a live checklist.
- Regional risk: since 2024 Play Protect in Singapore (a pilot Google may
  widen) blocks installs from browsers, messaging apps and file managers of
  apps that request RECEIVE_SMS, READ_SMS, the accessibility service or the
  notification listener. An app installed through a session installer is not
  affected.
- Updates. GitHub releases have no update channel; recommend Obtainium
  (tracks GitHub releases, installs through the session installer, which
  also sidesteps the restricted settings gate on the next install), or build
  an in-app updater with `REQUEST_INSTALL_PACKAGES` and the session API.
- F-Droid style reproducibility (optional). Pin `buildToolsVersion`, disable
  PNG crunching, avoid R8 features that are not deterministic, keep the NDK
  out unless pinned, and check that `apksigcopier` can transplant your
  release signature onto a clean build; F-Droid then publishes your own
  signed APK under `AllowedAPKSigningKeys`. Obtainium itself does this.

Sources: https://source.android.com/docs/security/features/apksigning,
https://developer.android.com/studio/publish/app-signing,
https://developer.android.com/about/versions/oreo/android-8.0-changes,
https://developer.android.com/about/versions/14/behavior-changes-all,
https://9to5google.com/2023/10/18/google-play-protect-scan/,
https://thehackernews.com/2024/02/google-starts-blocking-sideloading-of.html,
https://github.com/ImranR98/Obtainium,
https://f-droid.org/docs/Reproducible_Builds/

## Part 3: Feature mapping from the extension to an Android app

| Extension feature (README) | Android mechanism | Fidelity |
|---|---|---|
| Shorts hidden always, `/shorts/ID` opens as `/watch` | Accessibility service on the YouTube app: BACK or click Home tab on `reel_player_page_container`, hide or neutralise the Shorts tab in `pivot_bar`; in browsers, DNS cannot do it, so use the URL bar reading trick from AppBlock or leave the extension in charge of the browser | Direct for the app; the redirect to a normal watch page is not possible (no navigation API), BACK is the closest |
| Shorts in feeds, search, notifications hidden | Accessibility can cover shelf nodes (`shorts_shelf`) with an overlay region like Scrolless does for TikTok, but cannot remove them; notifications: NotificationListenerService cancel | Partial |
| Reels and short video surfaces of Instagram, Facebook, TikTok, Snapchat (distracting sites presets) | Accessibility service with the ids in (c); TikTok blocked by package | Direct, with the id maintenance cost |
| Distracting sites by domain, pause mode with countdown, intention, timed pass, daily budget, cooldown, grayscale | App level: foreground detection plus block Activity that runs the countdown and then grants a pass tracked in the app; site level in browsers: DNS VPN blocks the domain, and a pass means temporarily answering the name normally (the tun answers change instantly, no restart) | Direct for apps; direct for whole domains; grayscale is not possible outside your own window |
| Domain patterns `site.com/` and `site.com/path`, exceptions like `reddit.com/r/programming` | DNS sees only hostnames. Path rules and path exceptions cannot be done. Only AppBlock's approach (accessibility reads the URL in supported browsers) sees paths, at the cost of browser-specific selectors and a page flash | Cannot be done under DNS; partial under accessibility URL reading |
| Adult site blocker (15,000 domains plus hostname keyword rules) | DNS VPN with the same list; keyword rules on the queried name work the same way | Direct, subject to the Private DNS and Secure DNS caveats |
| Friction timer: turning the filter off starts a countdown that only runs while the popup is open | Same logic in the app: a countdown Activity that cancels on `onPause`; enforce in the app's own settings screen | Direct |
| Tighten any time, loosen only while off | Pure app logic; also mirror it in the strict layer (Settings block only relaxes while the filter is off) | Direct |
| Locked hours (weekly schedule, cannot be turned off) | Foreground service evaluates the schedule every minute plus exact alarms at boundaries; strict layer keeps Settings and uninstall blocked during the window | Direct, with the alarm and OEM battery caveats |
| Lock for N hours | Same, ad hoc window; Cooldown style unlock as the recovery path | Direct |
| Educational videos only (category gate on the watch page) | The YouTube app exposes no category in its view tree, and the app cannot fetch the watch page on the app's behalf. Could be approximated only by reading the channel name from the player node and matching an allow list, or by blocking the app and steering to the mobile site where the extension logic could run in a WebView | Cannot be done inside the YouTube app |
| Unhook style toggles (home feed, sidebar, end screen, comments, thumbnails, view counts, chips, shelves, autoplay) | Some can be covered with accessibility overlays (comments panel, home feed); most cannot (autoplay, annotations, view counts inside text) | Mostly cannot be done; not worth chasing |
| Block page reason line, block counts, thirty second pass warning | Block Activity and notifications | Direct |
| Settings in `chrome.storage.sync` | DataStore plus optional backup file export | Direct |
| No network calls of its own | Same; the VPN forwards DNS to the network's own resolvers, not to a server of ours | Direct |

Two consequences for the plan. First, the Android app is three services
(accessibility, DNS VPN, notification listener) plus a foreground monitor,
each of which an OEM can kill and the user can disable, so the health screen
that shows which of the four is alive is a core feature, not a debug page.
Second, everything the extension does by rewriting pages has to become
either a whole-app rule, a whole-domain rule, or a view id rule with a
maintenance budget; the per-path and per-video features stay in the browser
extension.

## All sources

AppBlock

- https://appblock.app/
- https://appblock.app/premium/
- https://appblock.app/help/android/
- https://appblock.app/how-to-set-up-appblock-on-android/
- https://appblock.app/how-to-use-quick-block-2/
- https://appblock.app/how-to-use-strict-mode-2/
- https://appblock.app/how-schedules-work-in-appblock-2/
- https://appblock.app/how-usage-limits-work-on-android/
- https://appblock.app/how-to-create-a-location-schedule/
- https://appblock.app/how-to-block-instagram-reels-youtube-shorts-or-snapchat-stories-on-android/
- https://appblock.app/how-to-block-adult-betting-or-distracting-websites-on-android/
- https://appblock.app/how-to-block-newly-installed-apps-2/
- https://appblock.app/how-to-block-websites-on-android/
- https://appblock.app/which-browsers-are-supported-by-appblock-2/
- https://appblock.app/what-are-the-limitations-of-website-blocking-on-android/
- https://appblock.app/how-can-i-block-only-notifications-from-selected-apps/
- https://appblock.app/can-i-set-a-pin-code-to-protect-appblocks-launch-2/
- https://appblock.app/a-guide-to-appblock-insights-2/
- https://appblock.app/my-accessibility-keeps-turning-off-what-can-i-do-2/
- https://appblock.app/specific-phone-brands/
- https://mobilesoft.freshdesk.com/support/solutions/articles/48001268949-i-am-not-able-to-uninstall-appblock
- https://apkpure.com/appblock-block-apps-sites/cz.mobilesoft.appblock
- https://en.wikipedia.org/wiki/AppBlock
- https://habitdoom.com/blog/android-app-blocker-that-cant-be-bypassed

Open-source apps

- https://github.com/duartebarbosadev/Scrolless
- https://github.com/undergroundairlines/sroll
- https://github.com/buenotty/Blockfy
- https://github.com/shubh07o/FocusBlock
- https://github.com/yunussorkac/ShortsBlocker
- https://github.com/atick-faisal/Shorts-Blocker and issue 74
- https://github.com/yadavnikhil03/AntiScroll
- https://github.com/ResolveCommunity/AwayDoomscrollin
- https://github.com/Moritz-Staat/block-instagram-reels issues 13 and 40
- https://codeberg.org/timelimit/opentimelimit-android
- https://f-droid.org/packages/io.timelimit.android.open/
- https://github.com/julian-klode/dns66
- https://github.com/M66B/NetGuard (FAQ.md, ADBLOCKING.md)
- https://github.com/IngoZenz/personaldnsfilter
- https://rethinkdns.com/faq
- https://github.com/ImranR98/Obtainium
- https://github.com/ngdathd/ForegroundActivity
- https://dev.to/rexa/how-to-block-apps-on-android-without-an-accessibilityservice-5gog
- https://dev.to/vishal_pathak_209/i-reverse-engineered-youtube-to-delete-just-the-shorts-here-is-the-code-3b84

Platform documentation

- https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- https://developer.android.com/reference/android/view/accessibility/AccessibilityWindowInfo
- https://developer.android.com/reference/android/app/usage/UsageEvents.Event
- https://developer.android.com/guide/components/activities/background-starts
- https://developer.android.com/about/versions/12/behavior-changes-all#untrusted-touch-events
- https://developer.android.com/about/versions/14/behavior-changes-14
- https://developer.android.com/about/versions/14/changes/fgs-types-required
- https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- https://developer.android.com/about/versions/14/behavior-changes-all
- https://developer.android.com/about/versions/15/behavior-changes-15
- https://developer.android.com/training/package-visibility
- https://developer.android.com/develop/connectivity/vpn
- https://developer.android.com/reference/android/service/notification/NotificationListenerService
- https://developer.android.com/guide/topics/admin/device-admin
- https://developers.google.com/android/work/device-admin-deprecation
- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/training/monitoring-device-state/doze-standby
- https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
- https://source.android.com/docs/security/features/apksigning
- https://developer.android.com/studio/publish/app-signing
- https://developer.android.com/about/versions/oreo/android-8.0-changes
- https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/AndroidManifest.xml
- https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/src/com/android/settings/Settings.java
- https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/src/com/android/settings/applications/specialaccess/deviceadmin/DeviceAdminAdd.java
- https://support.google.com/chrome/answer/10468685?hl=en&co=GENIE.Platform%3DAndroid
- https://www.chromium.org/developers/dns-over-https/
- https://adguard.com/kb/adguard-for-android/solving-problems/compatibility-issues/
- https://f-droid.org/docs/Reproducible_Builds/

Restricted settings and sideloading

- https://www.esper.io/blog/android-13-sideloading-restriction-harder-malware-abuse-accessibility-apis
- https://www.xda-developers.com/android-13-restricted-setting-notification-listener/
- https://www.androidauthority.com/android-15-restricted-settings-sideloading-3481098/
- https://dev.moe/en/3030
- https://9to5google.com/2023/10/18/google-play-protect-scan/
- https://thehackernews.com/2024/02/google-starts-blocking-sideloading-of.html
