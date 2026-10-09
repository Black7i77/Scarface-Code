package com.scarface.code.workspace
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

data class DocumentEntry(val uri: Uri, val name: String, val directory: Boolean)
class DocumentStore(private val context: Context) {
    companion object { const val MAX_BYTES = 2 * 1024 * 1024 }
    fun name(uri: Uri): String = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: uri.lastPathSegment ?: "untitled.txt"
    fun read(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream(); val chunk = ByteArray(8192)
            while (true) { val count = input.read(chunk); if (count < 0) break
                require(out.size() + count <= MAX_BYTES) { "File exceeds the 2 MiB editor limit" }; out.write(chunk, 0, count) }
            out.toByteArray()
        } ?: error("Could not open file")
        return TextCodec.decode(bytes)
    }

    fun readBinaryPreview(uri: Uri, limit: Int = 4096): ByteArray {
        require(limit in 1..65536) {
            "Invalid preview size"
        }

        return context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(1024)

            while (out.size() < limit) {
                val count = input.read(
                    buffer,
                    0,
                    minOf(buffer.size, limit - out.size())
                )

                if (count < 0) break
                if (count == 0) continue

                out.write(buffer, 0, count)
            }

            out.toByteArray()
        } ?: error("Could not open binary preview")
    }

    fun write(uri: Uri, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "File exceeds the 2 MiB editor limit; split it before saving" }
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes); it.flush() } ?: error("Could not write file")
    }
    fun listChildren(uri: Uri, root: Boolean = true): List<DocumentEntry> {
        val parentId = if (root) DocumentsContract.getTreeDocumentId(uri) else DocumentsContract.getDocumentId(uri)
        val queryUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, parentId)
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
        val entries = mutableListOf<DocumentEntry>()
        context.contentResolver.query(queryUri, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) entries.add(DocumentEntry(
                DocumentsContract.buildDocumentUriUsingTree(uri, cursor.getString(0)), cursor.getString(1) ?: "unnamed",
                cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR))
        } ?: error("Folder provider returned no listing")
        return entries.sortedWith(compareBy<DocumentEntry> { !it.directory }.thenBy { it.name.lowercase() })
    }
}
