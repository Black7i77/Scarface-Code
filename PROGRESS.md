# Execution ledger — Android-Code-Editor-Plan.md
New project isolated in scarface-code on feat/android-editor; no existing user repository is modified.
Pre-flight: Tasks 1–3 share buffer IDs; Tasks 3–4 exchange immutable text. Interfaces agree.
Ruling: Native Sora editor replaces the embedded editor surface in the spec — the plan selects native Android touch integration. Cost: editor APIs require compile verification.
Build prerequisites downloading; SDK endpoint reachable.
Ruling: Android JVM tests were invoked before implementation but dependency resolution prevented compilation; test sources are written first and remain pending until toolchain bootstrap finishes. Python runner RED missing module → GREEN 7/7 host tests.
Root cause of Gradle bootstrap failure: daemon retained a proxy port from an earlier command. Build uses a fresh daemon with the current command proxy; project files do not contain proxy settings.
Review: independent reviewer found Save As picker-state loss, premature destination assignment, stale recovery ordering, inconsistent recovery size limits, nested SAF query bug, DONE/death race, stale binding callbacks and uncaught lifecycle persistence errors. Fix pass: retained session plus saved picker ID; commit destination after successful write; shared revision-ordered bounded recovery; DocumentsContract query; DONE acknowledgment and onUnbind shutdown; run/connection guards; persistence error reporting. Device tests remain unavailable without a connected device/emulator.
Final evidence: build SUCCESSFUL; 21/21 JVM tests + 7/7 host Python tests pass. Grammar load/tokenization confirmed. Signature verifies; no INTERNET permission. Instrumentation APK builds, but connected device tests/launch/layout QA remain unexecuted (no attached device, attempted waiting task canceled).
Final deferred minor: D8 Kotlin metadata warnings for upstream Sora DSL; dex packaging succeeded. Existing legacy API deprecation/native-strip/Python-bytecode warnings documented in VALIDATION.md.
Source kept on local feature branch; no external publication or repository push was requested.
