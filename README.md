# dotline

A fast, private Android home screen with a black, dot-matrix look.

> Status: early development (chunk 0: an empty home screen that can be set as the default launcher).

## Principles

- **Private by design.** No internet permission, no notification access, no accessibility service, no analytics, no ads. `scripts/check-permissions.sh` fails the build check if any permission sneaks in.
- **Light.** Kotlin and Jetpack Compose, no heavy libraries. Target: under 80 MB of RAM.
- **Yours.** Your real app icons, your favourites, your accent colour.

## Build

Requirements: JDK 17+ and the Android SDK (platform 36).

```bash
./gradlew assembleDebug
scripts/check-permissions.sh
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## Undo

On the phone: Settings → Apps → Default apps → Home screen → pick your previous launcher.
