package com.scarface.code.runtime
import android.app.Service
import android.content.Intent
import android.os.*
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import java.util.concurrent.Executors

class RuntimeService : Service() {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var running = false
    private var timeout: Runnable? = null
    private val messenger = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                RuntimeController.START -> execute(msg)
                RuntimeController.STOP -> Process.killProcess(Process.myPid())
            }
        }
    })
    override fun onBind(intent: Intent): IBinder = messenger.binder
    private fun execute(msg: Message) {
        val client = msg.replyTo ?: return
        val data = msg.data
        val id = data.getString("id") ?: return
        fun send(kind: Int, text: String = "", success: Boolean = false) {
            try { client.send(Message.obtain(null, kind).apply { this.data = Bundle().apply {
                putString("id", id); putString("text", text); putBoolean("success", success); putInt("pid", Process.myPid())
            } }) } catch (_: RemoteException) { Process.killProcess(Process.myPid()) }
        }
        if (running) { send(RuntimeController.DONE, "Runtime already busy", false); return }
        running = true
        send(RuntimeController.READY)
        timeout = Runnable { send(RuntimeController.OUTPUT, "\n[30-second timeout]\n"); Process.killProcess(Process.myPid()) }.also { main.postDelayed(it, 30000) }
        worker.execute {
            var success = false
            var used = 0
            var truncated = false
            val emit: (String, String) -> Unit = { _, value ->
                val remaining = (65536 - used).coerceAtLeast(0)
                val content = value.take(remaining)
                used += content.length
                content.chunked(2048).forEach { send(RuntimeController.OUTPUT, it) }
                if (value.length > remaining && !truncated) { truncated = true; send(RuntimeController.OUTPUT, "\n[output truncated]\n") }
            }
            try {
                val file = File(filesDir, "runs/$id.txt")
                val source = file.readText()
                when (data.getString("language")) {
                    "javascript" -> success = JsRunner.run(source, data.getString("filename") ?: "script.js", emit = emit)
                    "sql", "json", "xml" -> success = DocumentRunners.run(data.getString("language") ?: "", source, emit)
                    "python" -> {
                        if (!Python.isStarted()) Python.start(AndroidPlatform(applicationContext))
                        val module = Python.getInstance().getModule("runner")
                        val callback = object : PythonOutput { override fun emit(channel: String, text: String) { emit(channel, text) } }
                        success = module.callAttr("execute", source, data.getString("filename") ?: "script.py", module.callAttr("java_emitter", callback), data.getBoolean("debug", false)).toBoolean()
                    }
                    else -> emit("stderr", "This language has no bundled runtime\n")
                }
            } catch (e: Throwable) { emit("stderr", (e.message ?: e.javaClass.simpleName) + "\n") }
            main.post {
                timeout?.let { main.removeCallbacks(it) }; timeout = null; running = false
                send(RuntimeController.DONE, if (success) "[finished]" else "[failed]", success)
                // The client acknowledges DONE with STOP after receiving it.
                // Keep a fallback in case the client disappeared without acknowledging.
                timeout = Runnable { Process.killProcess(Process.myPid()) }.also { main.postDelayed(it, 2000) }
            }
        }
    }
    interface PythonOutput { fun emit(channel: String, text: String) }
    override fun onUnbind(intent: Intent?): Boolean { Process.killProcess(Process.myPid()); return false }
    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }
}
