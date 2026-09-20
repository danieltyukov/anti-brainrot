#!/usr/bin/env bash
# Emulator helpers for end to end testing of the Android app.
#   emu.sh install <apk>          install and grant every permission the app needs
#   emu.sh grant [pkg]            grant permissions (accessibility, overlay, notifications, battery)
#   emu.sh shot <file.png>        screenshot
#   emu.sh dump                   print the current UI tree (uiautomator)
#   emu.sh tap "<text>"           tap the first node whose text or content-desc contains <text>
#   emu.sh tapid "<resource-id>"  tap the first node with that resource id
#   emu.sh launch [pkg]           launch the app
set -euo pipefail
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
PKG="${PKG:-io.github.danieltyukov.antibrainrot.debug}"
SERVICE="io.github.danieltyukov.antibrainrot.service.BlockerAccessibilityService"
LISTENER="io.github.danieltyukov.antibrainrot.service.NotificationBlockerService"

grant() {
  local pkg="${1:-$PKG}"
  $ADB shell appops set "$pkg" SYSTEM_ALERT_WINDOW allow
  # Android 13+: sideloaded apps need "Allow restricted settings" before an accessibility service can bind.
  $ADB shell appops set "$pkg" ACCESS_RESTRICTED_SETTINGS allow >/dev/null 2>&1 || true
  $ADB shell pm grant "$pkg" android.permission.POST_NOTIFICATIONS || true
  $ADB shell dumpsys deviceidle whitelist "+$pkg" >/dev/null || true
  # Cycle the setting so an updated APK gets re-bound (a same-value write does not).
  $ADB shell settings delete secure enabled_accessibility_services >/dev/null
  $ADB shell settings put secure accessibility_enabled 0
  sleep 1
  $ADB shell settings put secure enabled_accessibility_services "$pkg/$SERVICE"
  $ADB shell settings put secure accessibility_enabled 1
  $ADB shell cmd notification allow_listener "$pkg/$LISTENER" || true
  echo "granted for $pkg"
}

dump() {
  # Never reuse a stale file: remove it first, retry while the UI settles.
  $ADB shell rm -f /sdcard/ui.xml >/dev/null 2>&1
  for attempt in 1 2 3 4; do
    if $ADB shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && $ADB shell test -s /sdcard/ui.xml 2>/dev/null; then
      $ADB shell cat /sdcard/ui.xml
      return 0
    fi
    sleep 1
  done
  echo "dump failed" >&2
  return 1
}

bounds_of() {
  # $1 = attribute, $2 = needle; prints "x y" of the node centre
  dump | python3 -c '
import re, sys
attr, needle = sys.argv[1], sys.argv[2]
xml = sys.stdin.read()
nodes = re.findall(r"<node [^>]*/?>", xml)
def centre(n):
    b = re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", n)
    if not b: return None
    x1, y1, x2, y2 = map(int, b.groups())
    if x2 - x1 < 4 or y2 - y1 < 4: return None
    return (x1 + x2) // 2, (y1 + y2) // 2
def value(n):
    m = re.search(attr + r"=\"([^\"]*)\"", n)
    return m.group(1).lower() if m else None
exact = [n for n in nodes if value(n) == needle.lower()]
partial = [n for n in nodes if value(n) and needle.lower() in value(n)]
for n in exact + partial:
    c = centre(n)
    if c:
        print(c[0], c[1])
        break
' "$1" "$2"
}

case "${1:-}" in
  install)
    $ADB install -r -g "$2" >/dev/null && echo "installed $2"
    grant "$PKG"
    ;;
  grant) grant "${2:-$PKG}" ;;
  shot) $ADB exec-out screencap -p > "$2" && echo "saved $2" ;;
  dump) dump ;;
  tap)
    xy=$(bounds_of text "$2"); [ -z "$xy" ] && xy=$(bounds_of content-desc "$2")
    [ -z "$xy" ] && { echo "not found: $2" >&2; exit 1; }
    $ADB shell input tap $xy && echo "tapped $2 at $xy"
    ;;
  tapid)
    xy=$(bounds_of resource-id "$2"); [ -z "$xy" ] && { echo "not found: $2" >&2; exit 1; }
    $ADB shell input tap $xy && echo "tapped $2 at $xy"
    ;;
  front) $ADB shell dumpsys window 2>/dev/null | grep -m1 -E "mCurrentFocus|mFocusedApp" | sed 's/.*{//; s/}.*//' ;;
  launch) $ADB shell am start -n "${2:-$PKG}/io.github.danieltyukov.antibrainrot.MainActivity" >/dev/null && echo "launched" ;;
  *) echo "usage: emu.sh install|grant|shot|dump|tap|tapid|launch"; exit 1 ;;
esac
