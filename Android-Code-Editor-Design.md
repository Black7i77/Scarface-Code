# Android Code Editor — v0.1 design

## Goal
Create an installable Android code editor for Scott's phone, visually inspired by the supplied VS Code screenshots. Working name: Scarface Code. This is an independent application with its own icon and branding.

## Approach
Use a native Kotlin Android application with an embedded editor surface and packaged language assets. Compared with a plain text widget, this supports richer syntax and suggestions; compared with a remotely hosted editor, it keeps editing available offline. Exact editor and runtime dependencies must be verified during implementation planning.

## Layout
Use charcoal backgrounds, cyan accents, monospace code, line numbers, syntax colours, editor tabs, an explorer drawer, and a resizable bottom output panel. Portrait mode uses collapsible panels; landscape mode shows the explorer beside the editor. Provide scalable text and generous touch targets. Include a coding toolbar with Tab, brackets, quotes, arrows, undo, and redo.

## Editing and languages
Open any text-based source file. Provide syntax support for Python, JavaScript, TypeScript, HTML, CSS, PHP, C, C++, C#, Java, Kotlin, Rust, Go, Swift, Dart, Bash, PowerShell, SQL, JSON, YAML, XML, and Markdown, subject to verified packaged grammars. Unknown languages remain editable as plain text. Offer word and keyword suggestions, bracket pairing, indentation, search/replace, undo/redo, and multiple tabs. Suggestions are not advertised as full semantic IntelliSense for every language.

## Files and recovery
Use Android's document picker for user-selected files and project folders. Maintain editable buffers separately from saved files. Show dirty-tab indicators. Confirm closing unsaved files and report save failures without losing the buffer. Keep private recovery drafts and restore them after process recreation. Persist granted document permissions when supported; handle revoked permissions gracefully.

## Execution
Run JavaScript and Python locally through embedded runtimes, with no server required. Run only after an explicit Run action. Show stdout, stderr, errors, and completion status in the bottom panel. Execute away from the UI thread, cap retained output, and provide Stop with a bounded timeout. Keep runtime workers separate from file editing and expose only explicitly supported capabilities. Final runtime selection and termination mechanisms require verification before code is written.

The bottom panel initially provides real program output and runtime controls. A general Linux shell, arbitrary desktop compilers, native package managers, debugger integration, and VS Code extensions are later work; do not present simulated shell output as a functioning terminal.

## Components
Separate workspace/document access, editor buffers, editor UI, recovery storage, language configuration, and runtime workers. Keep runtime state independent from document saving. Editing and bundled runtime use work offline, without login or telemetry.

## Verification and deliverables
Test document save failures and buffer recovery, tab dirty state, JavaScript and Python output/errors, cancellation, output limits, and portrait/landscape layout. Build the Android project and produce source plus an APK if an Android SDK and dependencies are available. Report build or device-validation limitations explicitly. Include setup instructions and actual source in the conversation when delivering code.

## v0.1 limits
No claim of complete VS Code compatibility, universal language execution, full per-language semantic autocomplete, or desktop extension support. No cloud AI assistant in this version. Large files may require a documented size limit determined by performance testing.
