# Anti-Brainrot for Android

The Android app brings the extension's rules to the phone: YouTube Shorts and
the other short-video feeds are closed inside their apps, distracting apps sit
behind a pause and a timed pass, adult and distracting sites are filtered at
the DNS level, and the whole thing runs under the same friction timer, the
same "tighten any time, loosen only while off" rule, the same locked hours and
ad hoc locks. It is meant to replace AppBlock for one person's use and is not
on Google Play; the APK is attached to every GitHub release.

## What it does

| Area | Mechanism |
| --- | --- |
| YouTube Shorts (always), Instagram Reels, Facebook Reels, Snapchat Spotlight | Accessibility service recognises the fullscreen feed by the view ids the apps use and presses Back; after three immediate reopenings it goes Home |
| Blocked apps | Accessibility service sees the app come to the front and puts the block screen over it. Pause mode: countdown that only runs while the screen is in front, an intention line, a pass of N minutes from a daily budget, then a cooldown. Block mode: no pass |
| Hidden notifications | Notification listener drops notifications from blocked apps while the filter is on and no pass is active |
| Adult sites, distracting sites | A DNS-only VPN: only the two fake resolver addresses are routed into the tunnel; blocked names get NXDOMAIN, everything else is forwarded to the network's own resolver. 15,000 domains plus keyword rules, the extension's presets as whole hosts, your own hosts and an allow list |
| Friction timer | Turning the filter off runs the countdown on the Home screen; leaving the screen cancels it |
| Locked hours, Lock for N hours | The service checks every 15 seconds and on every app switch; during a lock the switch is forced on and cannot be turned off |
| Strict mode | While the filter is on, the Settings pages that could disable the app (its App info page, the accessibility page) are closed as they open |
| Stats | Today's blocks, passes and closed feeds |

What it does not do: educational-only mode (the YouTube app exposes no
category), path-level website rules (a DNS filter sees hosts only), location
or Wi-Fi conditions, per-app time metering (passes are charged up front
instead), widgets.

## Compared with AppBlock

AppBlock's Quick Block and Schedules map to the filter switch plus Lock for N
hours and Locked hours. Its usage limits map to the pass budget and cooldown.
Its strict mode maps to strict mode plus the loosening rule, which is stricter
than a PIN: there is no secret to type, only time to wait. Its website
blocking reads the URL bar of supported browsers; this app filters names for
every app on the device instead, which catches in-app browsers and unsupported
browsers but cannot see paths. Its content blocking (Reels, Shorts, Spotlight)
maps directly. Location and Wi-Fi conditions, launch-count limits and
allow-only mode are not implemented. Full survey: `docs/research/mobile.md`.

## Permissions

- Accessibility service (required): foreground detection, feed closing, the block screen trigger.
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
The `fakeyoutube` module is a stand-in with a YouTube fork's package name and
the Shorts view ids, so the detector can be exercised on emulators where the
real app refuses to run. Release builds are signed in CI from the
`ANDROID_KEYSTORE_B64` and `ANDROID_KEYSTORE_PASSWORD` secrets; locally a
`keystore.properties` next to the Gradle files does the same.

## Known limits

- Browsers with their own DNS over HTTPS setting bypass a local DNS filter.
- View ids change with app updates. The counters on the Home screen show
  whether feeds are still being closed; the ids live in one list in
  `BlockerAccessibilityService.kt`.
- Some phones (Xiaomi, Huawei, Samsung with aggressive battery settings) kill
  accessibility services; enable unrestricted battery and, where offered,
  autostart.
