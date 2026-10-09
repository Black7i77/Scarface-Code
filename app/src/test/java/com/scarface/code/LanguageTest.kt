package com.scarface.code
import com.scarface.code.editor.LanguageCatalog
import org.junit.Assert.*
import org.junit.Test
class LanguageTest {
    @Test fun promisedLanguagesHaveMappings() {
        val cases = mapOf("py" to "python", "js" to "javascript", "ts" to "typescript", "html" to "html", "css" to "css", "php" to "php", "c" to "c", "cpp" to "cpp", "cs" to "csharp", "java" to "java", "kt" to "kotlin", "rs" to "rust", "go" to "go", "swift" to "swift", "dart" to "dart", "sh" to "bash", "ps1" to "powershell", "sql" to "sql", "json" to "json", "yaml" to "yaml", "xml" to "xml", "md" to "markdown")
        for ((ext, language) in cases) assertEquals(language, LanguageCatalog.forFilename("test.$ext")?.id)
    }
    @Test fun uppercaseAndUnknownExtensions() {
        assertEquals("python", LanguageCatalog.forFilename("MAIN.PY")?.id)
        assertNull(LanguageCatalog.forFilename("test.unknown"))
    }
}
