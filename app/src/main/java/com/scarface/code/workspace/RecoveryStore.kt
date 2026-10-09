package com.scarface.code.workspace
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class RecoverySnapshot(val buffers: List<EditorBuffer> = emptyList(), val activeId: String? = null, val warning: String? = null)
class RecoveryStore(private val dir: File, private val maxBytes: Int = 64 * 1024 * 1024) {
    companion object { private val revisions = mutableMapOf<String, Long>() }
    fun save(buffers: List<EditorBuffer>, activeId: String?, revision: Long = System.nanoTime()) = synchronized(RecoveryStore::class.java) {
        if (revision < (revisions[dir.absolutePath] ?: Long.MIN_VALUE)) return@synchronized

        dir.mkdirs()
        val array = JSONArray()
        buffers.forEach { b -> array.put(JSONObject().put("id", b.id).put("uri", b.uri ?: JSONObject.NULL)
            .put("name", b.name).put("text", b.text).put("saved", b.savedText).put("line", b.line).put("column", b.column)) }
        val bytes = JSONObject().put("buffers", array).put("active", activeId ?: JSONObject.NULL).toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= maxBytes) { "Recovery drafts exceed 64 MiB. Save and close some files." }
        val temp = File(dir, "recovery.tmp")
        temp.outputStream().use { it.write(bytes); it.fd.sync() }
        check(temp.renameTo(File(dir, "recovery.json"))) { "Could not save recovery draft" }
        revisions[dir.absolutePath] = revision
    }
    fun load(): RecoverySnapshot = synchronized(RecoveryStore::class.java) { try {
        val file = File(dir, "recovery.json")
        if (!file.exists()) RecoverySnapshot() else if (file.length() > maxBytes.toLong()) run { preserveUnreadable(file); RecoverySnapshot(warning = "Recovery file exceeds 64 MiB; a recovery backup was preserved") } else {
            val root = JSONObject(file.readText())
            val array = root.getJSONArray("buffers")
            val list = (0 until array.length()).map { i ->
                val b = array.getJSONObject(i)
                EditorBuffer(b.getString("id"), if (b.isNull("uri")) null else b.getString("uri"),
                    b.getString("name"), b.getString("text"), b.getString("saved"), b.optInt("line"), b.optInt("column"))
            }
            RecoverySnapshot(list, if (root.isNull("active")) null else root.getString("active"))
        }
    } catch (e: Exception) { preserveUnreadable(File(dir, "recovery.json")); RecoverySnapshot(warning = "Recovery drafts could not be loaded: ${e.message}") } }
    private fun preserveUnreadable(file: File) {
        if (file.exists()) {
            // A same-directory rename preserves bytes without copying large corrupt snapshots.
            file.renameTo(File(dir, "recovery-unreadable-${System.currentTimeMillis()}.json"))
        }
    }
}
