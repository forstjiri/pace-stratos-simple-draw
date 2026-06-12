# Pace / Stratos Simple Draw

Small fullscreen drawing app for Amazfit Pace and Stratos.

## Features

- launcher-visible app icon
- neon drawing strokes
- swipeable color toolbar
- three stroke widths
- eraser
- drawing persistence across relaunches
- back button exits the app
- edge swipe blocking to reduce accidental dismiss

## Project Layout

- `src/` Java sources
- `res/` Android resources
- `AndroidManifest.xml` app manifest
- `build.sh` simple APK build script

## Build

Requirements:

- Java / `javac`
- Android SDK with:
  - platform `android-22`
  - build-tools `35.0.0`
- debug keystore at `~/.android/debug.keystore`

If the SDK is installed in `../.android-sdk`, the script uses it automatically.
Otherwise set `ANDROID_SDK_ROOT`.

```bash
./build.sh
```

The signed APK is written to:

```bash
./kresliciapp-signed.apk
```

## Emulator

This project was tested with a round Android 5.1 emulator profile (`320x320`).

## Notes

This is a simple standalone project intended for Huami-based watches and emulator testing.
