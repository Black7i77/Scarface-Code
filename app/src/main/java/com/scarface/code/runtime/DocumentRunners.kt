package com.scarface.code.runtime

import android.database.sqlite.SQLiteDatabase
import org.json.JSONTokener
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.InputSource
import java.io.StringReader

/** Local, disposable SQL execution and document validation. No network or persistent DB. */
object DocumentRunners {
    fun run(language: String, source: String, emit: (String, String) -> Unit): Boolean {
        return try {
            when (language) {
                "json" -> {
                    val value = JSONTokener(source).nextValue()
                    // Reject trailing content rather than silently accepting two documents.
                    val tokenizer = JSONTokener(source)
                    tokenizer.nextValue()
                    require(tokenizer.nextClean() == '\u0000') { "Unexpected content after JSON document" }
                    emit("stdout", "Valid JSON (${value.javaClass.simpleName})\n")
                }
                "xml" -> {
                    val factory = SAXParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
                    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                    val reader = factory.newSAXParser().xmlReader
                    reader.entityResolver = org.xml.sax.EntityResolver { _, _ -> InputSource(StringReader("")) }
                    reader.contentHandler = org.xml.sax.helpers.DefaultHandler()
                    reader.parse(InputSource(StringReader(source)))
                    emit("stdout", "Valid XML\n")
                }
                "sql" -> runSql(source, emit)
                else -> error("Unsupported language")
            }
            true
        } catch (e: Exception) {
            emit("stderr", "${e.javaClass.simpleName}: ${e.message ?: "Invalid input"}\n")
            false
        }
    }

    private fun runSql(source: String, emit: (String, String) -> Unit) {
        val db = SQLiteDatabase.create(null)
        try {
            // SQLite handles statement syntax. Simple splitting is not a full SQL parser:
            // semicolons inside quoted strings are supported; triggers are not supported here.
            val statements = splitStatements(source)
            require(statements.isNotEmpty()) { "No SQL statements" }
            require(statements.size <= 100) { "Too many SQL statements (limit 100)" }
            for (statement in statements) {
                val keyword = statement.trimStart().takeWhile { it.isLetter() }.uppercase()
                if (keyword in setOf("SELECT", "WITH", "PRAGMA", "EXPLAIN")) {
                    db.rawQuery(statement, null).use { cursor ->
                        emit("stdout", cursor.columnNames.joinToString(" | ") + "\n")
                        var rows = 0
                        while (cursor.moveToNext() && rows < 100) {
                            emit("stdout", (0 until cursor.columnCount).joinToString(" | ") { i -> cursor.getString(i) ?: "NULL" } + "\n")
                            rows++
                        }
                        if (cursor.moveToNext()) emit("stdout", "[rows truncated at 100]\n")
                        emit("stdout", "$rows row(s) shown\n")
                    }
                } else {
                    db.execSQL(statement)
                    emit("stdout", "OK: $keyword\n")
                }
            }
        } finally { db.close() }
    }

    private fun splitStatements(source: String): List<String> {
        val parts = mutableListOf<String>()
        val buffer = StringBuilder()
        var quote = '\u0000'
        var lineComment = false
        var blockComment = false
        var i = 0
        while (i < source.length) {
            val c = source[i]
            val next = source.getOrNull(i + 1)
            if (lineComment) {
                if (c == '\n') { lineComment = false; buffer.append(' ') }
            } else if (blockComment) {
                if (c == '*' && next == '/') { blockComment = false; i++ }
            } else if (quote != '\u0000') {
                buffer.append(c)
                if (c == quote) {
                    if (next == quote) { buffer.append(next); i++ } else quote = '\u0000'
                }
            } else if (c == '-' && next == '-') { lineComment = true; i++ }
            else if (c == '/' && next == '*') { blockComment = true; i++ }
            else if (c == '\'' || c == '"' || c == '`') { quote = c; buffer.append(c) }
            else if (c == ';') { if (buffer.isNotBlank()) parts.add(buffer.toString().trim()); buffer.clear() }
            else buffer.append(c)
            i++
        }
        require(quote == '\u0000' && !blockComment) { "Unterminated SQL quote or comment" }
        if (buffer.isNotBlank()) parts.add(buffer.toString().trim())
        return parts
    }
}
