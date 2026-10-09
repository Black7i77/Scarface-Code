# Validation — 2026-10-09

## Executed successfully

- Gradle `:app:testDebugUnitTest :app:assembleDebug`: **BUILD SUCCESSFUL**.
- 21 JVM tests, zero failures/errors: buffers, dirty/save state, independent tabs, UTF-8/CRLF, file limits, concurrent/revision-ordered recovery, output limits/stale run IDs, JavaScript logging/errors/no Java bridge/timeout, language mapping, and loading/tokenizing the packaged TextMate grammars.
- Seven host Python runner tests: Unicode stdout, stderr, syntax/runtime errors, fresh globals, SystemExit handling and output limits. All passed.
- `:app:assembleDebugAndroidTest`: device-test APK compiled and packaged.
- `apksigner verify --verbose`: APK signature verified (v2).
- APK manifest: package `com.scarface.code`, min SDK 24, target SDK 35; no INTERNET permission. Only AndroidX's signature-protected dynamic-receiver permission is requested.
- Source review: an independent reviewer inspected files, recovery, runtime lifecycle and state. Important source findings were addressed. Recovery ordering/size and grammar behavior have host coverage; lifecycle/provider behaviors have device tests included.

## Not verified on a device

No phone or emulator was connected (`adb devices` listed none), and no hardware emulator acceleration was available. The connected-device test task was attempted and canceled while waiting. Installation, app launch, phone screenshots, keyboard/portrait/landscape rendering, SAF provider/revocation behavior, rotation during Save As, actual Python-to-Java output integration, and actual process termination timing need Android device verification. Host tests do not prove these device behaviors.

## Known build warnings

- D8 reported Kotlin metadata rewriting warnings for Sora's unused DSL helpers. Debug dex packaging completed; no shrinking is enabled. A future tooling update should align AGP/D8 with that library's Kotlin metadata.
- Python bytecode precompilation was skipped because build-host Python 3.13 was absent. Python source and the bundled 3.13 interpreter were packaged; runtime compilation is supported by Chaquopy.
- Native Python libraries were packaged without extra symbol stripping. This increases APK size.
- Android legacy-inset APIs and Rhino optimization API produce deprecation warnings.

This is a debug-signed v0.1 preview, not a device-validated public release.
