# dotline

A fast, private Android home screen with a black, dot-matrix look.

## What it does

- **Home (Bento):** round-dot clock, live battery and next-alarm tiles, four app tiles, search pill and a four-app dock.
- **All apps:** swipe up. Real app icons, a big letter showing where you are, and an A–Z strip to jump. Pull down to close.
- **Search:** type a few letters and press Enter to open the top match. Handles prefixes, word starts ("kite" finds Yellow Kite), initials ("ps" finds Play Store) and accents.
- **Quick page:** swipe right for a dot calendar, free storage, battery health and four quick-open apps.
- **Long-press any app:** app info, add or remove from home, dock or quick open, hide, uninstall (Android asks to confirm).
- **Settings:** long-press empty home. Accent colour, round or rounded-square icons, 12/24-hour clock, quick page on or off, hidden apps.

## Private by design

- **No internet permission.** The app cannot send anything off the phone.
- No notification access, accessibility service, device admin, analytics, ads or tracking libraries.
- The only permission opens Android's own uninstall confirmation.
- Settings are stored only on the phone (DataStore, app-private).
- `scripts/check-permissions.sh` fails if any other permission appears in the built APK.

## Build

Requirements: JDK 17+ and the Android SDK (platform 36).

```bash
./gradlew assembleDebug                 # debug APK
./gradlew assembleRelease               # shrunk release APK (~2 MB)
scripts/check-permissions.sh            # privacy gate (debug APK by default)
scripts/check-permissions.sh app/build/outputs/apk/release/app-release.apk
```

Release signing reads `~/.config/dotline/keystore.properties` (or the path in `DOTLINE_KEYSTORE_PROPERTIES`). The key is never stored in this repo. Without it, release builds are unsigned.

## Tests

```bash
./gradlew testDebugUnitTest             # search, sections, pins, calendar, sizes
./gradlew connectedDebugAndroidTest     # UI tests on a connected device or emulator
```

## Performance (Pixel 5 emulator, software graphics)

- About 27 MB of RAM at rest.
- Smoother than the stock Pixel launcher under the same swipe test (median frame 53 ms vs 73 ms; both are slow only because the emulator has no GPU).

## Undo

On the phone: Settings → Apps → Default apps → Home screen → pick your previous launcher.

## Credits

Dot-matrix font: [Doto](https://github.com/oliverlalan/Doto), SIL Open Font License 1.1 (see `licenses/OFL-Doto.txt`).
