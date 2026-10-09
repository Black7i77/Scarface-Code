#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
if ! command -v javac >/dev/null 2>&1; then
    echo 'Install JDK 17 first: sudo apt install openjdk-17-jdk' >&2
    exit 1
fi
if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" && ! -f local.properties ]]; then
    echo 'Set ANDROID_HOME to your Android SDK folder first.' >&2
    exit 1
fi
chmod +x gradlew
./gradlew :app:testDebugUnitTest :app:assembleDebug
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/Scarface-Code-v0.1.0.apk
printf '%s\n' 'APK ready: dist/Scarface-Code-v0.1.0.apk'
