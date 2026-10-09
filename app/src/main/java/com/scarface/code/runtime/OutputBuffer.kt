package com.scarface.code.runtime
class OutputBuffer(private val limit: Int = 64 * 1024) {
    private val value = StringBuilder()
    var truncated = false
        private set
    val text: String get() = value.toString()
    fun append(text: String) {
        value.append(text)
        if (value.length > limit) { value.delete(0, value.length - limit); truncated = true }
    }
}
class RunOutput {
    var runId: String? = null
        private set
    var buffer = OutputBuffer()
        private set
    fun begin(id: String) { runId = id; buffer = OutputBuffer() }
    fun append(id: String, text: String) { if (id == runId) buffer.append(text) }
}
