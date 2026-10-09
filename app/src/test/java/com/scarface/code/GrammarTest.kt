package com.scarface.code
import com.scarface.code.editor.LanguageCatalog
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.FileResolver
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class GrammarTest {
    @Test fun packagedGrammarsLoadAndTokenizeRealCode() {
        val base = File("src/main/assets")
        FileProviderRegistry.getInstance().addFileProvider(FileResolver { path -> File(base, path).takeIf { it.isFile }?.inputStream() })
        val registry = GrammarRegistry.getInstance()
        registry.loadGrammars("textmate/languages.json")
        for (language in LanguageCatalog.languages) assertNotNull(language.title, registry.findGrammar("source.${language.id}"))
        val tokens = registry.findGrammar("source.python")!!.tokenizeLine("def hello():").tokens
        assertTrue(tokens.any { it.scopes.contains("keyword.control") })
        assertTrue(tokens.any { it.scopes.contains("entity.name.function") })
        val stringTokens = registry.findGrammar("source.javascript")!!.tokenizeLine("const value = \"hello\";").tokens
        assertTrue(stringTokens.any { it.scopes.contains("string.quoted.double") })
    }
}
