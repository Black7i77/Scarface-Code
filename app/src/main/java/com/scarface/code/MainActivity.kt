package com.scarface.code

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.scarface.code.editor.*
import com.scarface.code.runtime.RuntimeController
import com.scarface.code.workspace.*
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.SelectionMovement
import java.util.concurrent.Executors
import android.widget.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
/** Native phone workbench. Documents and runtime workers have separate lifecycles. */
class MainActivity : ComponentActivity() {
    companion object { private val io = Executors.newSingleThreadExecutor() }
    private val cyan = Color.rgb(0, 199, 230)
    private val bg = Color.rgb(30, 30, 30)
    private val panel = Color.rgb(24, 24, 24)
    private val muted = Color.rgb(145, 151, 158)
    private val main = Handler(Looper.getMainLooper())
    private val session: EditorSession by viewModels()
    private val workspace get() = session.workspace
    private lateinit var documents: DocumentStore
    private lateinit var recovery: RecoveryStore
    private lateinit var editor: CodeEditor
    private lateinit var editing: EditorController
    private lateinit var tabs: LinearLayout
    private lateinit var workPane: LinearLayout
    private lateinit var status: TextView
    private lateinit var explorer: LinearLayout
    private lateinit var explorerTitle: TextView
    private lateinit var explorerList: LinearLayout
    private lateinit var output: TextView
    private lateinit var outputScroll: ScrollView
    private lateinit var outputPanel: LinearLayout
    private lateinit var runtime: RuntimeController
    private lateinit var runButton: TextView
    private lateinit var stopButton: TextView
    private var showing = false
    private var projectUri: Uri? = null
    private var currentFolder: Uri? = null
    private val folderStack = mutableListOf<Uri>()
    private var fontSize = 16f
    private var saveAsBuffer: String?
        get() = session.saveAsBuffer
        set(value) { session.saveAsBuffer = value }
    private var tabsSignature = ""
    private var outputUpdateScheduled = false
    private val recoveryTask = Runnable { persist() }
    private val outputTask = Runnable { outputUpdateScheduled = false; refreshOutput() }

    private val openFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { retain(uri); loadFile(uri) }
    }
    private val openFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            retain(uri); projectUri = uri; currentFolder = uri; folderStack.clear()
            getPreferences(0).edit().putString("project", uri.toString()).apply()
            showExplorer(true); listFolder(uri)
        }
    }
    private val createFile = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val id = saveAsBuffer; saveAsBuffer = null
        if (uri != null && id != null) {
            retain(uri)
            val b = workspace.buffers.firstOrNull { it.id == id }
            if (b != null) { writeBuffer(b, uri, true) }
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        documents = DocumentStore(applicationContext)
        recovery = RecoveryStore(filesDir)
        runtime = RuntimeController.get(applicationContext)
        if (state?.containsKey("pendingSaveAs") == true) saveAsBuffer = state.getString("pendingSaveAs")
        if (!session.loaded) { val restored = recovery.load(); workspace.restore(restored); session.loaded = true; restored.warning?.let { warning -> main.post { error(warning) } } }
        val prefs = getPreferences(0)
        projectUri = prefs.getString("project", null)?.let(Uri::parse)
        currentFolder = projectUri
        fontSize = prefs.getFloat("font", 16f)
        buildWorkbench()
        if (workspace.buffers.isEmpty() && !prefs.getBoolean("started", false)) {
            workspace.open("hello.py", "# Welcome to Scarface Code\n# Edit offline. Tap ▶ to run Python.\n\nname = \"Scott\"\nprint(f\"Hello, {name}! 🐺\")\n\nfor number in range(1, 6):\n    print(f\"Code on Android: {number}\")\n").savedText = ""
            prefs.edit().putBoolean("started", true).apply()
        }
        showActive()
        session.updates.observe(this) { if (!isDestroyed) { refreshTabs(true); updateStatus() } }
        runtime.listener = { queueOutput() }
        refreshOutput()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    explorer.visibility == View.VISIBLE -> showExplorer(false)
                    runtime.running -> AlertDialog.Builder(this@MainActivity).setTitle("Program is running")
                        .setMessage("Stop the program before leaving?").setPositiveButton("Stop and leave") { _, _ -> runtime.stop(); persist(); finish() }
                        .setNegativeButton("Keep editing", null).show()
                    else -> { persist(); finish() }
                }
            }
        })
    }

    private fun buildWorkbench() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        // Android 15 edge-to-edge: keep all controls outside system bars and keyboard.
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else { view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom) }
            insets
        }
        val top = row(panel)
        top.addView(action("☰", "Toggle explorer") { toggleExplorer() })
        top.addView(label("SCARFACE CODE", 13f, Color.WHITE).apply { typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, dp(48), 1f))
        top.addView(action("⌕", "Find and replace") { findDialog() })
        top.addView(action("⋮", "More actions") { menu(it) })
        root.addView(top)
        val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val rail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(panel) }
        rail.addView(action("▱", "Files") { toggleExplorer() })
        rail.addView(action("⌕", "Search") { findDialog() })
        rail.addView(action("▶", "Run code") { runCurrent() })
        rail.addView(action(">_", "Output panel") { toggleOutput() })
        rail.addView(Space(this), LinearLayout.LayoutParams(1, 0, 1f))
        rail.addView(action("⚙", "Settings") { settings() })
        body.addView(rail, LinearLayout.LayoutParams(dp(48), -1))
        val frame = FrameLayout(this)
        val work = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; workPane = work
        tabs = row(panel)
        val tabScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(tabs) }
        work.addView(tabScroll, LinearLayout.LayoutParams(-1, dp(48)))
        editor = CodeEditor(this)
        try { editing = EditorController(this, editor) } catch (e: Exception) {
            error("Language setup: ${e.message}")
            throw e
        }
        editing.setTextSize(fontSize)
        editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
            if (!showing) {
                workspace.active?.let { workspace.update(it.id, editor.text.toString()) }
                refreshTabs(); updateStatus(); queueRecovery()
            }
        }
        editor.subscribeEvent(SelectionChangeEvent::class.java) { _, _ -> updateStatus() }
        work.addView(editor, LinearLayout.LayoutParams(-1, 0, 1f))
        val keys = row(Color.rgb(36, 36, 36))
        listOf("Tab", "{", "}", "(", ")", "[", "]", "\"", "'", "=", ";", "←", "→", "↶", "↷").forEach { key ->
            keys.addView(action(key, when(key) { "↶" -> "Undo"; "↷" -> "Redo"; else -> key }) {
                when(key) {
                    "Tab" -> editor.commitText("    ", false)
                    "←" -> editor.moveSelection(SelectionMovement.LEFT)
                    "→" -> editor.moveSelection(SelectionMovement.RIGHT)
                    "↶" -> editor.undo()
                    "↷" -> editor.redo()
                    else -> editor.commitText(key)
                }
                editor.requestFocus()
            })
        }
        work.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(keys) }, LinearLayout.LayoutParams(-1, dp(48)))
        val outputHeader = row(panel)
        outputHeader.addView(label("OUTPUT", 11f, cyan).apply { typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, dp(48), 1f))
        runButton = action("▶", "Run Python or JavaScript") { runCurrent() }.apply {
            setOnLongClickListener { runCurrent(debug = true); true }
        }
        stopButton = action("■", "Stop program") { runtime.stop() }
        outputHeader.addView(runButton); outputHeader.addView(stopButton)
        outputHeader.addView(action("⌫", "Clear output") { if (!runtime.running) { runtime.output.begin("clear"); refreshOutput() } })
        outputHeader.addView(action("⌃", "Toggle output") { toggleOutput() })
        work.addView(outputHeader)
        outputPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(panel) }
        output = label("Ready • Python and JavaScript run locally", 12f, Color.rgb(196, 210, 220)).apply {
            typeface = Typeface.MONOSPACE; setTextIsSelectable(true); gravity = Gravity.TOP or Gravity.START; setPadding(dp(12), dp(8), dp(8), dp(8))
        }
        outputScroll = ScrollView(this).apply { addView(output, FrameLayout.LayoutParams(-1, -2)) }
        outputPanel.addView(outputScroll, LinearLayout.LayoutParams(-1, -1))
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        work.addView(outputPanel, LinearLayout.LayoutParams(-1, dp(if (landscape) 80 else 120)))
        frame.addView(work, FrameLayout.LayoutParams(-1, -1))
        explorer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(37, 37, 38)); elevation = dp(12).toFloat(); visibility = View.GONE }
        val explorerHeader = row(Color.rgb(37, 37, 38))
        explorerTitle = label("EXPLORER", 12f, muted)
        explorerHeader.addView(explorerTitle, LinearLayout.LayoutParams(0, dp(48), 1f))
        explorerHeader.addView(action("×", "Close explorer") { showExplorer(false) })
        explorer.addView(explorerHeader)
        explorer.addView(textAction("＋ New file") { newFile() })
        explorer.addView(textAction("Open file…") { openFile.launch(arrayOf("*/*")) })
        explorer.addView(textAction("Open folder…") { openFolder.launch(null) })
        explorerList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        explorer.addView(ScrollView(this).apply { addView(explorerList) }, LinearLayout.LayoutParams(-1, 0, 1f))
        frame.addView(explorer, FrameLayout.LayoutParams(dp(250), -1, Gravity.START))
        body.addView(frame, LinearLayout.LayoutParams(0, -1, 1f))
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        status = label("UTF-8", 11f, Color.WHITE).apply { setBackgroundColor(Color.rgb(0, 104, 145)) }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(28)))
        setContentView(root)
    }

    private fun action(text: String, description: String, onClick: (View) -> Unit): TextView = label(text, 19f, cyan).apply {
        gravity = Gravity.CENTER; contentDescription = description
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)); isFocusable = true
        setOnClickListener(onClick)
        val value = android.util.TypedValue(); theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
        setBackgroundResource(value.resourceId)
    }
    private fun textAction(text: String, click: () -> Unit) = label(text, 14f, Color.rgb(205, 210, 214)).apply {
        minHeight = dp(48); setOnClickListener { click() }; isFocusable = true
    }
    private fun label(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, dp(8), 0)
    }
    private fun row(color: Int) = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(color) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun captureCursor() { workspace.active?.let { it.line = editor.cursor.leftLine; it.column = editor.cursor.leftColumn } }
    private fun showActive() {
        showing = true
        workspace.active?.let { editing.show(it); editor.isEditable = true } ?: run { editor.setText(""); editor.isEditable = false }
        showing = false
        refreshTabs(true); updateStatus(); queueRecovery()
    }
    private fun refreshTabs(force: Boolean = false) {
        val signature = workspace.buffers.joinToString { "${it.id}:${it.name}:${it.dirty}" } + workspace.activeId
        if (!force && signature == tabsSignature) return
        tabsSignature = signature; tabs.removeAllViews()
        if (workspace.buffers.isEmpty()) { tabs.addView(textAction("＋ Create or open a file") { newFile() }); return }
        workspace.buffers.forEach { b ->
            val tab = row(if (b.id == workspace.activeId) bg else panel)
            val title = label("${if (b.dirty) "● " else ""}${b.name}", 13f, if (b.id == workspace.activeId) Color.WHITE else muted)
            title.minHeight = dp(48); title.setOnClickListener { captureCursor(); workspace.select(b.id); showActive() }
            tab.addView(title)
            tab.addView(action("×", "Close ${b.name}") { closeTab(b) })
            tabs.addView(tab)
        }
    }
    private fun closeTab(b: EditorBuffer) {
        fun close() { captureCursor(); workspace.close(b.id, true); showActive() }
        if (b.dirty) AlertDialog.Builder(this).setTitle("Unsaved changes in ${b.name}")
            .setMessage("Keep editing, save, or discard these changes?")
            .setPositiveButton("Save") { _, _ -> save(b) }
            .setNeutralButton("Discard") { _, _ -> close() }.setNegativeButton("Keep editing", null).show()
        else close()
    }
    private fun updateStatus() {
        val b = workspace.active
        status.text = if (b == null) "Scarface Code  •  Offline" else
            "Ln ${editor.cursor.leftLine + 1}, Col ${editor.cursor.leftColumn + 1}  •  UTF-8  •  ${LanguageCatalog.forFilename(b.name)?.title ?: "Plain text"}"
    }
    private fun newFile() {
        val input = EditText(this).apply { setSingleLine(); setText("main.py"); selectAll() }
        AlertDialog.Builder(this).setTitle("New file").setView(input).setPositiveButton("Create") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isEmpty() || name.contains('/') || name.contains('\\')) { error("Use a filename such as main.py"); return@setPositiveButton }
            captureCursor(); workspace.open(name, ""); showActive(); showExplorer(false)
        }.setNegativeButton("Cancel", null).show()
    }
    private fun retain(uri: Uri) {
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        catch (_: SecurityException) { try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {} }
    }

    private fun showBinaryInspector(name: String, bytes: ByteArray) {
        val text = android.text.SpannableStringBuilder()

        for (offset in bytes.indices step 16) {
            text.append(
                "%08X  ".format(offset)
            )

            val end = minOf(offset + 16, bytes.size)

            for (index in offset until end) {
                val value = bytes[index].toInt() and 0xFF
                val start = text.length

                text.append("%02X ".format(value))

                if (value < 32 || value > 126) {
                    text.setSpan(
                        android.text.style.ForegroundColorSpan(
                            Color.rgb(255, 95, 95)
                        ),
                        start,
                        text.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            for (index in end until offset + 16) {
                text.append("   ")
            }

            text.append(" | ")

            for (index in offset until end) {
                val value = bytes[index].toInt() and 0xFF
                text.append(
                    if (value in 32..126) value.toChar() else '.'
                )
            }

            text.append("\\n")
        }

        val viewer = TextView(this).apply {
            setText(text)
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(16, 22, 30))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setTextIsSelectable(true)
        }

        val scroll = HorizontalScrollView(this).apply {
            addView(
                ScrollView(this@MainActivity).apply {
                    addView(viewer)
                }
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Binary Inspector — $name")
            .setMessage(
                "Read-only byte preview. " +
                "Binary content does not necessarily mean encryption."
            )
            .setView(scroll)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun loadFile(uri: Uri) {
        io.execute {
            try { val text = documents.read(uri); val name = documents.name(uri)
                main.post { if (!isDestroyed) { captureCursor(); workspace.open(name, text, uri.toString()); showActive(); showExplorer(false) } }
            } catch (e: Exception) {
                val message = e.message ?: "Unknown error"

                if (
                    message.contains("Binary files cannot") ||
                    e is java.nio.charset.CharacterCodingException
                ) {
                    try {
                        val preview = documents.readBinaryPreview(uri)
                        val filename = documents.name(uri)

                        main.post {
                            if (!isDestroyed) {
                                showBinaryInspector(filename, preview)
                            }
                        }
                    } catch (previewError: Exception) {
                        main.post {
                            if (!isDestroyed) {
                                error("Could not inspect file: ${previewError.message}")
                            }
                        }
                    }
                } else {
                    main.post {
                        if (!isDestroyed) {
                            error("Could not open file: $message")
                        }
                    }
                }
            }
        }
    }
    private fun save(b: EditorBuffer? = workspace.active, saveAs: Boolean = false) {
        if (b == null) return
        if (saveAsBuffer != null) { error("Finish the current Save As first"); return }
        if (saveAs || b.uri == null) { saveAsBuffer = b.id; createFile.launch(b.name) }
        else writeBuffer(b, Uri.parse(b.uri))
    }
    private fun writeBuffer(b: EditorBuffer, uri: Uri, rename: Boolean = false) {
        val id = b.id; val text = b.text
        io.execute {
            try { documents.write(uri, text); val name = if (rename) documents.name(uri) else b.name
                main.post { workspace.markSaved(id, text); workspace.buffers.firstOrNull { it.id == id }?.let { it.name = name; if (rename) it.uri = uri.toString() }; session.changed()
                    if (!isDestroyed) { refreshTabs(true); updateStatus(); Toast.makeText(this, "Saved $name", Toast.LENGTH_SHORT).show() }; queueRecovery() }
            } catch (e: Exception) { main.post { error("Save failed; your draft is retained. ${e.message}") } }
        }
    }
    private fun listFolder(uri: Uri) {
        explorerTitle.text = "EXPLORER  •  loading…"
        io.execute {
            try { val entries = documents.listChildren(uri, uri == projectUri)
                main.post { if (!isDestroyed && currentFolder == uri) {
                    explorerTitle.text = "EXPLORER"; explorerList.removeAllViews()
                    if (folderStack.isNotEmpty()) explorerList.addView(textAction("↑ Parent folder") {
                        currentFolder = folderStack.removeAt(folderStack.lastIndex); listFolder(currentFolder!!) })
                    entries.forEach { entry -> explorerList.addView(textAction("${if (entry.directory) "▸" else "◇"}  ${entry.name}") {
                        if (entry.directory) { folderStack.add(uri); currentFolder = entry.uri; listFolder(entry.uri) } else loadFile(entry.uri)
                    }) }
                    if (entries.isEmpty()) explorerList.addView(label("Folder is empty", 13f, muted))
                } }
            } catch (e: Exception) { main.post { explorerTitle.text = "EXPLORER"; error("Folder access failed: ${e.message}") } }
        }
    }
    private fun toggleExplorer() {
        showExplorer(explorer.visibility != View.VISIBLE)
        if (explorer.visibility == View.VISIBLE) currentFolder?.let(::listFolder)
    }
    private fun showExplorer(open: Boolean) {
        explorer.visibility = if (open) View.VISIBLE else View.GONE
        val params = workPane.layoutParams as FrameLayout.LayoutParams
        params.leftMargin = if (open && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) dp(250) else 0
        workPane.layoutParams = params
    }
    private fun toggleOutput() { outputPanel.visibility = if (outputPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE }

    private fun previewHtml(source: String) {
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.mixedContentMode =
                WebSettings.MIXED_CONTENT_NEVER_ALLOW

            webViewClient = WebViewClient()

            loadDataWithBaseURL(
                "https://appassets.androidplatform.net/",
                source,
                "text/html",
                "UTF-8",
                null
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Scarface Code — HTML Preview")
            .setView(webView)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnDismissListener {
            webView.stopLoading()
            webView.destroy()
        }

        dialog.show()
    }

    private fun runCurrent(debug: Boolean = false) {
        if (runtime.running) { error("Stop the current program first"); return }
        val b = workspace.active ?: return
        if (b.name.endsWith(".html", ignoreCase = true) ||
            b.name.endsWith(".htm", ignoreCase = true)) {
            if (debug) {
                error("HTML debugging is not available yet")
                return
            }
            previewHtml(b.text)
            return
        }
        val language = LanguageCatalog.forFilename(b.name)?.id ?: "unknown"
        if (language !in setOf("python", "javascript", "sql", "json", "xml")) { error("No offline execution engine is bundled for this language yet."); return }
        if (b.name.substringAfterLast('.').lowercase() == "jsx") { error("JSX requires transpilation; run plain JavaScript in a .js file."); return }
        if (debug && language != "python") { error("Debug trace currently supports Python only"); return }
        outputPanel.visibility = View.VISIBLE
        try { runtime.start(language, b.text, b.name, debug) } catch (e: Exception) { error(e.message ?: "Could not run") }
    }
    private fun queueOutput() {
        if (!outputUpdateScheduled) { outputUpdateScheduled = true; main.postDelayed(outputTask, 50) }
    }
    private fun refreshOutput() {
        if (!::output.isInitialized || isDestroyed) return
        val buffer = runtime.output.buffer
        output.text = if (buffer.text.isEmpty()) "Ready • Python and JavaScript run locally" else
            (if (buffer.truncated) "[earlier output truncated]\n" else "") + buffer.text
        runButton.isEnabled = !runtime.running; runButton.alpha = if (runtime.running) .4f else 1f
        stopButton.isEnabled = runtime.running; stopButton.alpha = if (runtime.running) 1f else .35f
        outputScroll.post { outputScroll.fullScroll(View.FOCUS_DOWN) }
    }
    private fun findDialog() {
        if (workspace.active == null) return
        val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(16), 0) }
        val find = EditText(this).apply { hint = "Find text"; setSingleLine() }
        val replace = EditText(this).apply { hint = "Replace with"; setSingleLine() }
        fields.addView(find); fields.addView(replace)
        val dialog = AlertDialog.Builder(this).setTitle("Find / replace").setView(fields)
            .setPositiveButton("Find next", null).setNeutralButton("Replace all", null).setNegativeButton("Close") { _, _ -> editor.searcher.stopSearch() }.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val query = find.text.toString(); if (query.isNotEmpty()) {
                    editor.searcher.search(query, EditorSearcher.SearchOptions(false, false)); editor.postDelayed({ editor.searcher.gotoNext() }, 150)
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val query = find.text.toString(); if (query.isNotEmpty()) {
                    editor.searcher.search(query, EditorSearcher.SearchOptions(false, false)); editor.postDelayed({ editor.searcher.replaceAll(replace.text.toString()) }, 150)
                }
            }
        }
        dialog.show()
    }
    private fun menu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        listOf("New file", "Open file", "Open folder", "Save", "Save As", "Find / replace", "Language mode", "Settings", "About", "Third-party licenses").forEachIndexed { index, text -> popup.menu.add(0, index, index, text) }
        popup.setOnMenuItemClickListener { item -> when(item.itemId) {
            0 -> newFile(); 1 -> openFile.launch(arrayOf("*/*")); 2 -> openFolder.launch(null); 3 -> save(); 4 -> save(saveAs = true)
            5 -> findDialog(); 6 -> languageDialog(); 7 -> settings(); 8 -> AlertDialog.Builder(this).setTitle("Scarface Code 0.1")
                .setMessage("Offline Android code editor\n\n27 lightweight language modes. Local Python and JavaScript runtimes.\n\nIndependent project inspired by VS Code. No Microsoft affiliation.\n\nRun your own trusted scripts: Python code can access this app’s private files. No network permission is requested.")
                .setPositiveButton("OK", null).show()
            9 -> licenseDialog()
        }; true }; popup.show()
    }
    private fun licenseDialog() {
        val files = assets.list("licenses") ?: emptyArray()
        AlertDialog.Builder(this).setTitle("Third-party licenses").setItems(files) { _, index ->
            val text = assets.open("licenses/${files[index]}").bufferedReader().use { it.readText() }
            val content = label(text, 12f, Color.LTGRAY).apply { setTextIsSelectable(true); gravity = Gravity.TOP; setPadding(dp(16), dp(12), dp(16), dp(12)) }
            AlertDialog.Builder(this).setTitle(files[index]).setView(ScrollView(this).apply { addView(content) }).setPositiveButton("Close", null).show()
        }.show()
    }
    private fun languageDialog() {
        val b = workspace.active ?: return
        AlertDialog.Builder(this).setTitle("Set language by filename extension")
            .setItems(LanguageCatalog.languages.map { it.title }.toTypedArray()) { _, which ->
                val language = LanguageCatalog.languages[which]
                if (b.uri != null) { error("Use Save As with .${language.extensions.first()} to change this file’s language"); return@setItems }
                captureCursor(); b.name = b.name.substringBeforeLast('.', b.name) + "." + language.extensions.first(); showActive()
            }.show()
    }
    private fun settings() {
        AlertDialog.Builder(this).setTitle("Editor settings").setItems(arrayOf("Larger text", "Smaller text", "Toggle word wrap", "Output height: small", "Output height: large")) { _, index ->
            when (index) {
                0 -> fontSize = (fontSize + 2).coerceAtMost(28f)
                1 -> fontSize = (fontSize - 2).coerceAtLeast(14f)
                2 -> editor.isWordwrap = !editor.isWordwrap
                3 -> outputPanel.layoutParams = outputPanel.layoutParams.apply { height = dp(80) }
                4 -> outputPanel.layoutParams = outputPanel.layoutParams.apply { height = dp(200) }
            }
            editing.setTextSize(fontSize); getPreferences(0).edit().putFloat("font", fontSize).apply()
        }.show()
    }
    private fun queueRecovery() { main.removeCallbacks(recoveryTask); main.postDelayed(recoveryTask, 500) }
    private fun persist() {
        if (::editor.isInitialized && !isDestroyed) captureCursor()
        val snapshot = workspace.buffers.map { it.copy() }; val active = workspace.activeId; val revision = System.nanoTime()
        io.execute { try { recovery.save(snapshot, active, revision) } catch (e: Exception) { main.post { error("Could not save recovery draft: ${e.message}") } } }
    }
    private fun error(message: String) { if (!isDestroyed && !isFinishing) AlertDialog.Builder(this).setTitle("Scarface Code").setMessage(message).setPositiveButton("OK", null).show() }
    override fun onPause() { main.removeCallbacks(recoveryTask); persist(); super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("pendingSaveAs", saveAsBuffer); captureCursor(); try { recovery.save(workspace.buffers.map { it.copy() }, workspace.activeId) } catch (e: Exception) { error("Could not save recovery draft: ${e.message}") }; super.onSaveInstanceState(outState) }
    override fun onDestroy() { main.removeCallbacks(recoveryTask); main.removeCallbacks(outputTask); runtime.listener = null; editing.release(); super.onDestroy() }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.isCtrlPressed) when(keyCode) { KeyEvent.KEYCODE_S -> { save(); return true }; KeyEvent.KEYCODE_F -> { findDialog(); return true } }
        return super.onKeyDown(keyCode, event)
    }
}
