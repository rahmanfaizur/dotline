#!/usr/bin/env bash
# Privacy gate: fails if the built APK asks for any permission outside the allow-list.
# Usage: scripts/check-permissions.sh [path/to/app.apk]
set -euo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
AAPT2="$(ls -d "$SDK"/build-tools/*/aapt2 | sort -V | tail -1)"

# Permissions Dotline is allowed to have. Anything else fails the check.
# (AndroidX adds DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, an app-private
#  permission that only lets the app's own components talk to each other.)
ALLOWED=(
  # Opens Android's "Uninstall this app?" screen; the user must confirm there.
  "android.permission.REQUEST_DELETE_PACKAGES"
  # Sets the lock-screen wallpaper when the user taps Set; it can't read any wallpaper or file.
  "android.permission.SET_WALLPAPER"
  # Shows the optional player card in the pull-down panel; off by default, Android asks first.
  "android.permission.POST_NOTIFICATIONS"
  # Swipe down on home pulls down the notification panel; it can only open the panel.
  "android.permission.EXPAND_STATUS_BAR"
  "dev.rahmanfaizur.dotline.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
  "dev.rahmanfaizur.dotline.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
)

mapfile -t FOUND < <("$AAPT2" dump permissions "$APK" | sed -n "s/^uses-permission: name='\(.*\)'.*/\1/p")

echo "APK: $APK"
echo "Permissions requested: ${#FOUND[@]}"
bad=0
for p in "${FOUND[@]}"; do
  ok=0
  for a in "${ALLOWED[@]}"; do [[ "$p" == "$a" ]] && ok=1; done
  if [[ $ok == 1 ]]; then echo "  ok   $p"; else echo "  FAIL $p"; bad=1; fi
done

if [[ $bad == 1 ]]; then
  echo "Privacy check FAILED: unexpected permission(s) above."
  exit 1
fi
echo "Privacy check passed: no INTERNET or other sensitive permissions."
