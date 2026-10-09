package com.scarface.code
import com.scarface.code.runtime.*
import org.junit.Assert.*
import org.junit.Test
class RuntimeTest {
    @Test fun keepsBoundedTailAndReportsTruncation() {
        val b = OutputBuffer(16); b.append("abcdefghijklmnop"); b.append("QRST")
        assertEquals("efghijklmnopQRST", b.text); assertTrue(b.truncated)
    }
    @Test fun rejectsStaleRuns() {
        val b = RunOutput(); b.begin("new"); b.append("old", "obsolete")
        assertEquals("", b.buffer.text); b.append("new", "current")
        assertEquals("current", b.buffer.text)
    }
    @Test fun javascriptLogsAndExceptions() {
        val out = StringBuilder()
        assertTrue(JsRunner.run("console.log('hello', 42)", "test.js") { _, t -> out.append(t) })
        assertEquals("hello 42\n", out.toString())
        out.clear()
        assertFalse(JsRunner.run("throw new Error('bad')", "test.js") { _, t -> out.append(t) })
        assertTrue(out.contains("bad"))
    }
    @Test fun javascriptHasNoJavaBridge() {
        val out = StringBuilder()
        assertTrue(JsRunner.run("console.log(typeof java, typeof Packages)", "test.js") { _, t -> out.append(t) })
        assertEquals("undefined undefined\n", out.toString())
    }
    @Test fun javascriptInfiniteLoopTimesOut() {
        val start = System.nanoTime()
        assertFalse(JsRunner.run("while(true) {}", "test.js", 50) { _, _ -> })
        assertTrue((System.nanoTime() - start) / 1000000 < 2000)
    }
}
