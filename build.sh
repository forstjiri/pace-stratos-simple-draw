#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEFAULT_SDK_ROOT="$PROJECT_DIR/../.android-sdk"
if [[ -d "$DEFAULT_SDK_ROOT" ]]; then
    SDK_ROOT="${ANDROID_SDK_ROOT:-$DEFAULT_SDK_ROOT}"
else
    SDK_ROOT="${ANDROID_SDK_ROOT:-/tmp/opencode/android-sdk}"
fi
BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-35.0.0}"
BUILD_TOOLS_DIR="$SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION"
PLATFORM_DIR="$SDK_ROOT/platforms/android-22"
WORK_DIR="${WORK_DIR:-/tmp/opencode/kresliciapp-build}"
OUT_APK="${OUT_APK:-$PROJECT_DIR/kresliciapp-signed.apk}"
KEYSTORE="${KEYSTORE:-$HOME/.android/debug.keystore}"

fail() {
    printf 'Error: %s\n' "$1" >&2
    exit 1
}

[[ -d "$SDK_ROOT" ]] || fail "ANDROID_SDK_ROOT not found: $SDK_ROOT"
[[ -d "$BUILD_TOOLS_DIR" ]] || fail "build-tools not found: $BUILD_TOOLS_DIR"
[[ -f "$PLATFORM_DIR/android.jar" ]] || fail "platform android.jar not found: $PLATFORM_DIR/android.jar"
[[ -f "$KEYSTORE" ]] || fail "debug keystore not found: $KEYSTORE"

rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR/gen" "$WORK_DIR/obj" "$WORK_DIR/bin"

mapfile -t SOURCES < <(find "$PROJECT_DIR/src" -name '*.java' | sort)
[[ ${#SOURCES[@]} -gt 0 ]] || fail "no Java sources found in $PROJECT_DIR/src"

"$BUILD_TOOLS_DIR/aapt" package -f -m \
    -M "$PROJECT_DIR/AndroidManifest.xml" \
    -S "$PROJECT_DIR/res" \
    -J "$WORK_DIR/gen" \
    -I "$PLATFORM_DIR/android.jar"

javac --release 8 \
    -cp "$PLATFORM_DIR/android.jar:$WORK_DIR/gen" \
    -d "$WORK_DIR/obj" \
    "$WORK_DIR/gen/cz/kresliciapp/R.java" \
    "${SOURCES[@]}"

mapfile -t CLASSES < <(find "$WORK_DIR/obj" -name '*.class' | sort)

"$BUILD_TOOLS_DIR/d8" \
    --lib "$PLATFORM_DIR/android.jar" \
    --min-api 21 \
    --output "$WORK_DIR/bin" \
    "${CLASSES[@]}"

"$BUILD_TOOLS_DIR/aapt" package -f \
    -M "$PROJECT_DIR/AndroidManifest.xml" \
    -S "$PROJECT_DIR/res" \
    -I "$PLATFORM_DIR/android.jar" \
    -F "$WORK_DIR/bin/kresliciapp.unaligned.apk" \
    "$WORK_DIR/bin"

"$BUILD_TOOLS_DIR/zipalign" -f 4 \
    "$WORK_DIR/bin/kresliciapp.unaligned.apk" \
    "$WORK_DIR/bin/kresliciapp.apk"

mkdir -p "$(dirname "$OUT_APK")"

"$BUILD_TOOLS_DIR/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$OUT_APK" \
    "$WORK_DIR/bin/kresliciapp.apk"

"$BUILD_TOOLS_DIR/apksigner" verify "$OUT_APK"

printf 'Built APK: %s\n' "$OUT_APK"
