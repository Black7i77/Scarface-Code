package com.scarface.code
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import android.view.View
import android.view.ViewGroup
import io.github.rosemoe.sora.widget.CodeEditor
import androidx.test.platform.app.InstrumentationRegistry
import com.scarface.code.runtime.RuntimeController
import com.scarface.code.workspace.DocumentStore
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun run(language: String, code: String): String {
        val done = CountDownLatch(1)
        lateinit var runtime: RuntimeController
        instrumentation.runOnMainSync {
            runtime = RuntimeController.get(context)
            runtime.listener = { if (!runtime.running) done.countDown() }
            runtime.start(language, code, if (language == "python") "test.py" else "test.js")
        }
        assertTrue("Program did not finish", done.await(35, TimeUnit.SECONDS))
        return runtime.output.buffer.text
    }
    @Test fun pythonAndJavascriptProduceRealOutput() {
        assertTrue(run("python", "print('python works')").contains("python works"))
        assertTrue(run("javascript", "console.log('javascript works')").contains("javascript works"))
    }
    @Test fun runtimeErrorsReachOutput() {
        assertTrue(run("python", "1/0").contains("ZeroDivisionError"))
        assertTrue(run("javascript", "throw new Error('test-error')").contains("test-error"))
    }
    @Test fun stopNativeSleepAndRunAgain() {
        lateinit var runtime: RuntimeController
        instrumentation.runOnMainSync {
            runtime = RuntimeController.get(context)
            runtime.start("python", "import time; print('sleeping'); time.sleep(100)", "sleep.py")
        }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (!runtime.output.buffer.text.contains("sleeping") && System.nanoTime() < deadline) Thread.sleep(50)
        assertTrue(runtime.output.buffer.text.contains("sleeping"))
        val start = System.nanoTime()
        instrumentation.runOnMainSync { runtime.stop() }
        assertFalse(runtime.running)
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 2000)
        assertTrue(run("python", "print('restart works')").contains("restart works"))
    }
    @Test fun documentRoundTripAndFailedRead() {
        val file = File(context.cacheDir, "device-text.py")
        val store = DocumentStore(context); val uri = Uri.fromFile(file)
        store.write(uri, "🐺\r\nhello\n")
        assertEquals("🐺\r\nhello\n", store.read(uri))
        file.delete()
        assertThrows(Exception::class.java) { store.read(uri) }
    }
    @Test fun unsavedTextSurvivesActivityRecreation() {
        fun editor(view: View): CodeEditor? {
            if (view is CodeEditor) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) editor(view.getChildAt(i))?.let { return it }
            return null
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> editor(activity.findViewById(android.R.id.content))!!.setText("unsaved rotation 🐺") }
            scenario.recreate()
            scenario.onActivity { activity -> assertEquals("unsaved rotation 🐺", editor(activity.findViewById(android.R.id.content))!!.text.toString()) }
        }
    }
}
