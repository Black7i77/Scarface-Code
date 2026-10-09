package com.scarface.code.runtime
import android.content.*
import android.os.*
import java.io.File
import java.util.UUID

class RuntimeController(private val context: Context) {
    companion object {
        const val START = 1; const val STOP = 2; const val READY = 3; const val OUTPUT = 4; const val DONE = 5
        private var singleton: RuntimeController? = null
        fun get(context: Context): RuntimeController = singleton ?: RuntimeController(context.applicationContext).also { singleton = it }
    }
    val output = RunOutput()
    var running = false
        private set
    var listener: (() -> Unit)? = null
    private val main = Handler(Looper.getMainLooper())
    private var connection: ServiceConnection? = null
    private var remote: Messenger? = null
    private var runtimePid = 0
    private var sourceFile: File? = null
    private var watchdog: Runnable? = null
    private val receiver = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val id = msg.data.getString("id") ?: return
            if (id != output.runId || !running) return
            when (msg.what) {
                READY -> runtimePid = msg.data.getInt("pid")
                OUTPUT -> output.append(id, msg.data.getString("text") ?: "")
                DONE -> { output.append(id, "\n${msg.data.getString("text")}\n"); try { remote?.send(Message.obtain(null, STOP)) } catch (_: RemoteException) {}; finish() }
            }
            listener?.invoke()
        }
    })
    fun start(language: String, source: String, filename: String): String {
        check(!running) { "Stop the current run first" }
        require(language == "python" || language == "javascript") { "Run supports Python and JavaScript" }
        val id = UUID.randomUUID().toString()
        val file = File(context.filesDir, "runs/$id.txt"); file.parentFile!!.mkdirs(); file.writeText(source)
        sourceFile = file
        output.begin(id); output.append(id, "$filename • local $language\n\n"); running = true; runtimePid = 0
        val next = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                if (!running || output.runId != id || connection !== this) return
                remote = Messenger(binder)
                try { remote!!.send(Message.obtain(null, START).apply {
                    replyTo = receiver
                    data = Bundle().apply { putString("id", id); putString("language", language); putString("filename", filename) }
                }) } catch (e: RemoteException) { fail("Runtime connection failed") }
            }
            override fun onServiceDisconnected(name: ComponentName) { if (running && output.runId == id && connection === this) fail("Runtime stopped") }
            override fun onNullBinding(name: ComponentName) { if (running && output.runId == id && connection === this) fail("Runtime unavailable") }
            override fun onBindingDied(name: ComponentName) { if (running && output.runId == id && connection === this) fail("Runtime connection closed") }
        }
        connection = next
        if (!context.bindService(Intent(context, RuntimeService::class.java), next, Context.BIND_AUTO_CREATE)) fail("Could not start runtime")
        watchdog = Runnable { if (running && output.runId == id) { output.append(id, "\n[run timeout]\n"); stop() } }.also { main.postDelayed(it, 32000) }
        listener?.invoke()
        return id
    }
    fun stop() {
        if (!running) return
        val pid = runtimePid
        try { remote?.send(Message.obtain(null, STOP)) } catch (_: RemoteException) { }
        // Worker service uses this app's UID; never signal the editor process.
        if (pid > 0 && pid != Process.myPid()) Process.killProcess(pid)
        output.runId?.let { output.append(it, "\n[stopped]\n") }
        finish(); listener?.invoke()
    }
    private fun fail(text: String) { output.runId?.let { output.append(it, "\n[$text]\n") }; finish(); listener?.invoke() }
    private fun finish() {
        running = false
        watchdog?.let { main.removeCallbacks(it) }; watchdog = null
        connection?.let { try { context.unbindService(it) } catch (_: IllegalArgumentException) { } }
        connection = null; remote = null; runtimePid = 0
        sourceFile?.delete(); sourceFile = null
    }
}
