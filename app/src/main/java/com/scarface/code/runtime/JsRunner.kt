package com.scarface.code.runtime
import org.mozilla.javascript.*
object JsRunner {
    fun run(source: String, filename: String, timeoutMs: Long = 30000, emit: (String, String) -> Unit): Boolean {
        val deadline = System.nanoTime() + timeoutMs * 1000000
        val factory = object : ContextFactory() {
            override fun makeContext(): Context = super.makeContext().apply {
                optimizationLevel = -1
                languageVersion = Context.VERSION_ES6
                instructionObserverThreshold = 10000
                setClassShutter { false }
            }
            override fun observeInstructionCount(cx: Context, count: Int) {
                if (System.nanoTime() > deadline || Thread.currentThread().isInterrupted) throw Error("Execution timed out")
            }
        }
        return try {
            factory.call<Boolean> { cx ->
                val scope = cx.initSafeStandardObjects()
                val console = cx.newObject(scope)
                for (method in arrayOf("log", "info", "warn", "error")) {
                    ScriptableObject.putProperty(console, method, object : BaseFunction() {
                        override fun call(c: Context, s: Scriptable, thisObj: Scriptable, args: Array<out Any>): Any {
                            emit(if (method == "error" || method == "warn") "stderr" else "stdout", args.joinToString(" ") { Context.toString(it) } + "\n")
                            return Context.getUndefinedValue()
                        }
                    })
                }
                ScriptableObject.putProperty(scope, "console", console)
                cx.evaluateString(scope, source, filename, 1, null)
                true
            }
        } catch (e: Throwable) { emit("stderr", (e.message ?: e.javaClass.simpleName) + "\n"); false }
    }
}
