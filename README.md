# Scarface Code — Android editor v0.1

An independent, offline Android code editor inspired by the dark workbench layout of VS Code. Built for phone touch controls and external keyboards. No Microsoft affiliation.

## Install

Download `Scarface-Code-v0.1.0.apk` to your Android phone and open it. If Android asks, allow installation from the app you used to open the APK. Android 7.0/API 24 or newer is required. This preview APK is debug-signed, not a Play Store release.

## Use

- The first launch opens `hello.py`. Tap **▶** to run it locally.
- **☰ / Files** opens the explorer. Choose **Open file**, **Open folder**, or **New file**.
- **⋮ → Save** writes the current file. New files use Android's Save As picker.
- A **●** on a tab means unsaved changes. Recovery drafts persist in private app storage.
- Tap a tab to switch; **×** offers Save/Discard/Keep editing for dirty files.
- **Search** opens find/replace. **⚙** changes font size, wrapping and output height.
- **■** stops execution. Output and errors appear in the bottom panel.
- Ctrl+S saves; Ctrl+F searches. The touch toolbar supplies brackets, Tab, arrows and undo/redo.

## Languages

27 lightweight, locally packaged language modes: Python, JavaScript, TypeScript, HTML, CSS, PHP, C, C++, C#, Java, Kotlin, Rust, Go, Swift, Dart, Bash, PowerShell, SQL, JSON, YAML, XML, Markdown, Ruby, Lua, R, TOML and Dockerfile. Other text files remain editable. Grammars are original lightweight token rules, not full parsers. Suggestions include language keywords and identifiers; semantic completion is not available.

**Run supports Python and plain JavaScript only.** Editing C++ does not install a C++ compiler. JavaScript uses Rhino; it has no browser DOM or Node.js environment. Python uses a bundled CPython interpreter through Chaquopy. Third-party pip packages and interactive stdin are not supplied. Programs have a 30-second time limit and bounded output. Stop kills the runtime process, leaving editor drafts intact.

## Privacy and file behavior

No login, telemetry or Android INTERNET permission. User code runs with this application's permissions, so run trusted scripts: Python is an interpreter, not a security sandbox, and can access private app data. Android document grants authorize access only to files/folders you choose. Uninstalling removes private recovery drafts.

Files use UTF-8; binary, malformed UTF-8 and files above 2 MiB are rejected. Normal editing preserves text content including line endings. Provider writes cannot universally be atomic: a provider failure may leave a partially written destination, while the unsaved editor draft remains available. Keep backups for important work.

## Build on Linux

Install JDK 17 and Android Studio/Android SDK. Set `ANDROID_HOME` to the SDK directory and install Android platform 35 plus build-tools 35.0.0. First builds require Internet to resolve dependencies; the installed app operates offline.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

Or use `./build-linux.sh`; it checks prerequisites and copies the APK to `dist/Scarface-Code-v0.1.0.apk`.

Host Python tests:

```bash
python3 -m unittest discover -s tests -v
```

Device tests (connect a USB-debugging phone or start an emulator):

```bash
./gradlew :app:connectedDebugAndroidTest
```

See `VALIDATION.md` for checks actually executed during delivery, including any device limitations.

## Project layout

`workspace/`: document access, buffers and draft recovery. `editor/`: Sora configuration, theme and language mapping. `runtime/`: disposable Python/JavaScript service and output lifecycle. `MainActivity.kt`: native workbench and UI actions. `assets/textmate/`: original syntax rules. `tests/` and Android test directories contain checks.

## Limits

Preview software. No general terminal, debugger, Git integration, VS Code extensions, cloud AI, arbitrary compiler installation or universal runtime support. Syntax rules do not cover every language construct. Phone layout and runtime process lifecycle need device testing where unavailable in the build environment. Folder providers differ in their behavior.

Original application code is MIT licensed. Dependencies retain their own licenses; see `THIRD_PARTY_NOTICES.md` and `licenses/`. The source project permits rebuilding with modified editor libraries; reverse engineering for debugging modifications to LGPL components is permitted. Sora library source archives are included with the source delivery.
