# Scarface Code Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android editor with the approved VS Code-inspired appearance, broad language editing, and local Python/JavaScript execution.

**Architecture:** Native Kotlin activity with a Sora editor, document-provider workspace, private recovery store, and dedicated Android runtime service process. Runtime cancellation terminates the service process so blocked user programs cannot freeze the editor. JavaScript uses Rhino interpreted mode; Python uses Chaquopy.

**Tech Stack:** Kotlin, Android SDK, Gradle, Sora 0.24.4 TextMate modules, Chaquopy 17.0, Rhino. Pin a compatible AGP/Kotlin/Gradle/Rhino set after checking official release metadata; do not guess dependency coordinates. Android minimum API 24, Java 17, arm64-v8a phone build, x86_64 test build where available.

**Spec:** Android-Code-Editor-Design.md (delivered alongside this plan).

## Global Constraints
- Independent branding; working name Scarface Code.
- Editing and bundled runtime use work offline, without login or telemetry.
- Open any text-based source file; unknown languages remain editable as plain text.
- Run only after an explicit Run action.
- No claim of complete VS Code compatibility or universal language execution.
- No general Linux shell or cloud AI assistant in v0.1.
- Preserve unsaved buffers after file errors and process recreation.
- Source and APK delivery must identify any unverified build or device behavior.

## Review Focus
- Provider permission revoked mid-session: show an error and retain the buffer.
- Unicode, CRLF, and a final newline: save content without unintended changes.
- Infinite loop or native Python sleep: Stop terminates the runtime within two seconds.
- Huge stdout or late output from an old run: cap output and ignore stale run IDs.
- Rotation, keyboard display, and process recreation: preserve active tab and drafts.

## Files and interfaces
Project root: `scarface-code/`.
- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/wrapper/*`: pinned reproducible build.
- `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`: Android configuration; no INTERNET permission.
- `app/src/main/java/com/scarface/code/MainActivity.kt`: activity, document picker, layout wiring.
- `.../workspace/EditorBuffer.kt`, `WorkspaceController.kt`: tab state and save transitions.
- `.../workspace/DocumentStore.kt`: URI read/write and project traversal.
- `.../workspace/RecoveryStore.kt`: private atomic draft storage.
- `.../editor/EditorController.kt`, `LanguageCatalog.kt`: Sora integration and extension mapping.
- `.../runtime/RuntimeController.kt`, `RuntimeService.kt`, `JsRunner.kt`, `OutputBuffer.kt`: worker lifecycle and bounded output.
- `app/src/main/python/runner.py`: Python execution, stdout/stderr forwarding, exception reporting.
- `app/src/main/assets/textmate/`: grammar registry, dark theme, licensed grammars.
- `app/src/main/res/`: layouts, drawable icon, colours, strings, scalable touch controls.
- `app/src/test/`, `app/src/androidTest/`, `tests/test_runner.py`: model, runtime, and device tests.
- `README.md`, `THIRD_PARTY_NOTICES.md`, `LICENSE`: build/usage, limitations, dependency attribution.

### Task 1: Workspace model and build
**Produces:** `EditorBuffer(id: String, uri: String?, name: String, text: String, savedText: String)` with computed `dirty`; `WorkspaceController.open`, `update`, `select`, `close`, and `markSaved` keyed by buffer ID.
- [ ] Verify dependency versions and licenses from official sources. Install SDK/build tools if reachable; record exact unavailable hosts if blocked. Generate Gradle wrapper, Kotlin Android module, JUnit configuration and minimum API 24.
- [ ] Write failing `WorkspaceControllerTest`: two tabs retain independent edits; reverting text clears dirty; failed save leaves dirty; closing a dirty tab requires explicit discard; identical names with distinct IDs remain distinct.
- [ ] Run `./gradlew :app:testDebugUnitTest`; confirm tests fail for missing workspace implementation.
- [ ] Implement model and controller. Reject close without discard when dirty. Mark saved only after a successful document write.
- [ ] Run unit tests and `./gradlew :app:assembleDebug`; require successful exit or clearly record environment blocking build execution.
- [ ] Commit task files locally.

### Task 2: Documents and recovery
**Consumes:** workspace buffer model.
**Produces:** `DocumentStore.read(uri): String`, `write(uri, text): Unit`, `listChildren(treeUri): List<DocumentEntry>`; `RecoveryStore.save(buffers, activeId)` and `load(): RecoverySnapshot`.
- [ ] Write failing tests for recovery round-trip with Unicode and CRLF, missing/corrupt recovery file, and draft retained after simulated provider-write failure. Instrument URI tests for granted and revoked access.
- [ ] Run the relevant unit/device tests and confirm failure before implementation.
- [ ] Implement SAF file/folder access, persistent URI grants, UTF-8 text handling, and atomic private recovery snapshots. Enforce a 2 MiB editable-file limit with a visible message; detect binary content. Preserve buffer text on any provider exception. Record that SAF provider writes cannot universally guarantee atomic replacement.
- [ ] Wire Open, Open Folder, New, Save, Save As, and unsaved-close dialogs to the controller. Save recovery after edits with debounce and when leaving the activity.
- [ ] Run unit and provider tests; verify process recreation restores content and selected tab. Commit.

### Task 3: Editor and phone layout
**Consumes:** workspace APIs and document entries.
**Produces:** `LanguageCatalog.forFilename(name): LanguageDefinition?`; `EditorController.show(buffer)`, `setTextSize(sp)`, `release()` with edit callbacks into workspace.
- [ ] Write failing catalog tests mapping every language promised in the spec, uppercase extensions, and unknown extension fallback. Write instrumentation checks for tab switch preserving text and explorer toggling without losing focus state.
- [ ] Run tests and confirm failure.
- [ ] Integrate Sora TextMate, package and attribute verified grammars and a charcoal/cyan theme. Apply line numbers, bracket completion, indentation, word/keyword suggestions, undo/redo and search/replace. Do not claim semantic language-server completion.
- [ ] Build portrait explorer drawer and landscape explorer column, horizontal tabs, bottom output panel, status bar, and coding toolbar. Use at least 48 dp touch targets and selectable font sizes 14–28 sp. Add keyboard shortcuts for Save and Find.
- [ ] Run unit/device tests. Inspect portrait and landscape screenshots with keyboard open; ensure editor and Run/Stop controls remain reachable. Commit.

### Task 4: Local runtimes
**Consumes:** immutable code text, filename and language at Run time.
**Produces:** `RuntimeController.start(language, source, filename): runId`, `stop()`, events tagged with run ID; `OutputBuffer.append(text)` retains at most 64 KiB and indicates truncation.
- [ ] Write failing output tests for truncation and stale run IDs. Write Python tests for print, stderr, syntax/runtime exception and fresh globals on every run. Write runtime device tests for JavaScript logging/errors, Python logging/errors, infinite loops, native sleep cancellation, service death and successful restart after Stop.
- [ ] Run test groups and confirm failure.
- [ ] Implement a non-exported service in `:runtime`, with Messenger IPC and chunked output. Run one program at a time off the service main thread. Use run IDs to discard late messages. Keep source transmission below binder limits through a private temporary source file.
- [ ] Embed Rhino with interpreted mode and safe standard globals plus console.log/error; expose no Java host bridge, DOM, Node APIs or package loader. Embed Chaquopy with Python stdout/stderr adapters and exception formatting. Document available Python standard-library functionality and that stdin is not interactive in v0.1.
- [ ] Implement Stop as a service-main-thread command which kills only the runtime process. Use a watchdog fallback for unresponsive IPC, with a maximum two-second cancellation interval; validate actual device behavior rather than assuming thread interruption works. Set a 30-second run timeout. Handle runtime death as a recoverable result.
- [ ] Run runtime tests; verify Stop for both a busy loop and Python sleep while editor typing remains responsive. Commit.

### Task 5: Integration and delivery
**Consumes:** all earlier components.
- [ ] Add integration tests: Run uses the current unsaved buffer without silently saving; unsupported runtime language shows a clear message; save failure and runtime crash preserve drafts; rotate while running without duplicate execution.
- [ ] Run tests and confirm newly added tests fail where behavior is missing.
- [ ] Complete integration, distinct icon, sample files, README, build/install instructions and third-party notices. Clearly distinguish editing support from runnable languages and runtime output from a general shell.
- [ ] Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`, `python3 -m pytest tests/test_runner.py`, and `./gradlew :app:connectedDebugAndroidTest` if a device/emulator is available. Examine merged manifest for absent INTERNET permission. Inspect final phone layouts.
- [ ] Perform inline code review for unbounded output, data-loss transitions, URI access, stale listeners, service cancellation and grammar licensing. Fix findings and rerun affected checks.
- [ ] Package source, build instructions, test results and APK if successfully built. Save deliverables and show the main application source in chat as requested. Do not describe an unbuilt APK or untested launch as verified. Commit final changes locally; do not publish.

## Environment findings
The current workspace has Java 17. No configured Android SDK, SDK manager, or Gradle executable was found in the initial check. APK generation depends on obtaining the required tools and Maven artifacts through the permitted network. Source delivery remains possible if those downloads are blocked; that limitation must be reported.

## Self-review
All spec features have owning tasks: documents/recovery in Task 2; visuals and language editing in Task 3; runtime lifecycle in Task 4; delivery and integrated checks in Task 5. All five review-focus conditions have tests assigned. Runtime cancellation uses process boundaries rather than unsupported Python multiprocessing. Dependencies and language asset licenses are verified before integration.
