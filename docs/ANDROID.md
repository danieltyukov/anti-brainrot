# AntiBrainrot for Android

The Android app, called AntiBrainrot on the phone, is an app blocker in the
spirit of AppBlock with the extension's discipline: every app and every
site on your list gets its own rule, blocked outright or a daily timer, adult
sites are filtered at the DNS level, and the whole thing runs under the same
friction timer, the same "tighten any time, loosen only while off" rule, the
same locked hours and ad hoc locks. A Progress tab keeps ninety days of
counters. It is meant to replace AppBlock for one person's use and is not on
Google Play; the APK is attached to every GitHub release.

## What it does

| Area | Mechanism |
| --- | --- |
| Blocked apps | Each app has a rule. Block: the accessibility service sees the app come to the front and puts the block screen over it, with no way in. Timer: a daily limit in minutes; opening the app shows a pause (countdown that only runs while the screen is in front, an intention line), then one session of the configured length, capped by what is left of the day, then a cooldown. The service meters the time the app is in front while the screen is on and blocks it for the rest of the day once the limit is used up |
| Block new app installs | Optional. The Play Store, Samsung, Amazon, Huawei, Xiaomi, OPPO and vivo stores, F-Droid, Aurora and the package installer are blocked while the filter is on. A package added anyway (adb, a browser download) gets a Block rule from the accessibility service's package receiver before it is opened. Rules are keyed by package name and survive uninstalls, so a reinstalled app comes back blocked or timed |
| Hidden notifications | Notification listener drops notifications from blocked apps, and from timed apps outside a session, while the filter is on |
| Sites | Each site has a rule too, blocked or a daily timer, and subdomains follow it. The accessibility service reads the address bar of Chrome, Firefox, Samsung Internet, Brave, Edge, Opera, Vivaldi, DuckDuckGo and their variants and applies the rule the same way as for an app: block screen, pause, sessions, metering of the time the page is in front. Blocked sites are also answered NXDOMAIN by the DNS filter, so they are stopped in every app |
| Adult sites | The DNS-only VPN: only the two fake resolver addresses are routed into the tunnel; blocked names get NXDOMAIN, everything else is forwarded to the network's own resolver. 15,000 domains plus keyword rules and an allow list |
| Friction timer | Turning the filter off runs the countdown on the Home screen; leaving the screen cancels it |
| Locked hours, Lock for N hours | The service checks every 15 seconds and on every app switch; during a lock the switch is forced on and cannot be turned off |
| Strict mode | While the filter is on, the Settings pages that could disable the app (its App info page, the accessibility page, the device admin page) and the uninstall dialog are closed as they open |
| Prevent uninstall | Optional. The app registers as a device admin with no policies; Android refuses to uninstall an active admin, from the launcher, Settings or adb, until it is deactivated, and strict mode leaves that page. Turning it off is a loosening |
| Session notifications | While a timed app or site is in front, an ongoing notification shows the time left; a heads-up warning fires one minute before the block screen returns |
| Progress | Ninety days of daily counters: time the filter was on while the service ran, block screens (per app too), time in timed apps (per app), sessions and their minutes. Shown as totals over 7, 30 or 90 days, bar charts per day, a streak of days with the filter on for at least an hour, the most used timed apps and the most blocked apps |

What it does not do: anything inside other apps beyond the browser address
bar (no Shorts or Reels detection; block or time the whole app instead),
educational-only mode (the YouTube app exposes no category), path-level
website rules (rules are per host), location or Wi-Fi conditions, widgets.

## Screens

- Home: the filter card (state, unlock delay, Lock for N hours, the friction
  timer with its ring), today's three numbers, and locked hours.
- Progress: the history, see the table above. Empty until the filter has run.
- Apps: the pause, session and cooldown settings, the install block, every
  installed app with its rule, and rules for apps not installed right now.
  Tapping an app opens the rule editor.
- Sites: the site rules with suggestions for the usual feeds, the adult
  list switch and the allow list.
- More: the reason line, strict mode, prevent uninstall, theme, permissions, about.

## Compared with AppBlock

AppBlock's Quick Block and Schedules map to the filter switch plus Lock for N
hours and Locked hours. Its usage limits map to the pass budget and cooldown.
Its strict mode maps to strict mode plus the loosening rule, which is stricter
than a PIN: there is no secret to type, only time to wait. Its website
blocking reads the URL bar of supported browsers; this app filters names for
every app on the device instead, which catches in-app browsers and unsupported
browsers but cannot see paths. Its rules are per app
maps directly. Location and Wi-Fi conditions, launch-count limits and
allow-only mode are not implemented. Full survey: `docs/research/mobile.md`.

## Permissions

- Accessibility service (required): foreground detection, the block screen trigger, usage metering for timed apps.
- Display over other apps (required): the block screen.
- Notifications: the site filter's status notification.
- Notification access: hides notifications of blocked apps.
- Unrestricted battery: keeps the services alive.
- VPN consent: asked when a site filter is switched on.

Sideloaded apps on Android 13 and later: the accessibility switch is greyed
out until you open App info, tap the three dots and choose Allow restricted
settings. After an app update Android keeps the service listed as enabled but
does not start it again until the switch is cycled or the phone restarts; the
Setup screen and the Home screen say so when that happens.

## Build and test

```
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest
./scripts/emu.sh install app/build/outputs/apk/debug/app-debug.apk   # grants every permission over adb
./scripts/emu.sh launch
```

`scripts/emu.sh` also has `shot`, `dump`, `tap "<text>"` and `front`.

## Known limits

- Browsers with their own DNS over HTTPS setting bypass a local DNS filter,
  and so does Private DNS in the system settings; the Sites tab warns when
  Private DNS is on.
- Filter-on time is counted by the accessibility service in 15 second ticks
  and written once a minute, so it only covers time the service was running.
- Strict mode leaves any Settings page that shows the app's name, which
  includes the accessibility service list itself while the filter is on.
  Manage other services while the filter is off.
- Site rules are read from the address bar, so they cover the listed
  browsers and in-app browsers that reuse them, not every WebView. Blocked
  sites are covered everywhere through DNS; timed sites are not, since they
  must load during a session.
- Some phones (Xiaomi, Huawei, Samsung with aggressive battery settings) kill
  accessibility services; enable unrestricted battery and, where offered,
  autostart.
