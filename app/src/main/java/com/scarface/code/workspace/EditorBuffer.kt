package com.scarface.code.workspace
import java.util.UUID

data class EditorBuffer(
    val id: String = UUID.randomUUID().toString(),
    var uri: String? = null,
    var name: String,
    var text: String,
    var savedText: String = text,
    var line: Int = 0,
    var column: Int = 0
) { val dirty: Boolean get() = text != savedText }

class WorkspaceController {
    val buffers = mutableListOf<EditorBuffer>()
    var activeId: String? = null
        private set
    val active: EditorBuffer? get() = buffers.firstOrNull { it.id == activeId }
    fun open(name: String, text: String, uri: String? = null): EditorBuffer {
        if (uri != null) buffers.firstOrNull { it.uri == uri }?.let { select(it.id); return it }
        return EditorBuffer(name = name, text = text, uri = uri).also { buffers.add(it); activeId = it.id }
    }
    fun update(id: String, text: String) { buffers.firstOrNull { it.id == id }?.text = text }
    fun select(id: String) { require(buffers.any { it.id == id }); activeId = id }
    fun close(id: String, discard: Boolean = false): Boolean {
        val b = buffers.firstOrNull { it.id == id } ?: return true
        if (b.dirty && !discard) return false
        val index = buffers.indexOf(b); buffers.remove(b)
        if (activeId == id) activeId = buffers.getOrNull((index - 1).coerceAtLeast(0))?.id
        return true
    }
    fun markSaved(id: String, savedText: String) { buffers.firstOrNull { it.id == id }?.savedText = savedText }
    fun restore(snapshot: RecoverySnapshot) {
        buffers.clear(); buffers.addAll(snapshot.buffers)
        activeId = snapshot.activeId?.takeIf { id -> buffers.any { it.id == id } } ?: buffers.firstOrNull()?.id
    }
}
