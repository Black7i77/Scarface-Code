package com.scarface.code.editor
import android.content.Context
import android.graphics.Typeface
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.*
import io.github.rosemoe.sora.langs.textmate.registry.*
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.registry.IThemeSource
import com.scarface.code.workspace.EditorBuffer

class EditorController(context: Context, val view: CodeEditor) {
    companion object { private var loaded = false }
    init {
        if (!loaded) {
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.applicationContext.assets))
            val path = "textmate/dark.json"
            ThemeRegistry.getInstance().loadTheme(ThemeModel(IThemeSource.fromInputStream(context.assets.open(path), path, null), "Scarface").apply { isDark = true })
            ThemeRegistry.getInstance().setTheme("Scarface")
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
            loaded = true
        }
        view.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance()).apply {
            setColor(EditorColorScheme.WHOLE_BACKGROUND, 0xff1e1e1e.toInt())
            setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, 0xff1e1e1e.toInt())
            setColor(EditorColorScheme.LINE_NUMBER, 0xff858585.toInt())
            setColor(EditorColorScheme.CURRENT_LINE, 0xff282828.toInt())
            setColor(EditorColorScheme.SELECTION_INSERT, 0xff00c7e6.toInt())
        }
        view.typefaceText = Typeface.MONOSPACE
        view.typefaceLineNumber = Typeface.MONOSPACE
        view.setTextSize(16f)
        view.tabWidth = 4
        view.isLineNumberEnabled = true
        view.isWordwrap = false
    }
    fun show(buffer: EditorBuffer) {
        val language = LanguageCatalog.forFilename(buffer.name)
        view.setEditorLanguage(if (language == null) EmptyLanguage() else
            TextMateLanguage.create("source.${language.id}", true).apply { setCompleterKeywords(language.keywords.toTypedArray()) })
        view.setText(buffer.text)
        val line = buffer.line.coerceIn(0, view.text.lineCount - 1)
        view.setSelection(line, buffer.column.coerceIn(0, view.text.getColumnCount(line)))
    }
    fun setTextSize(sp: Float) { view.setTextSize(sp.coerceIn(14f, 28f)) }
    fun release() = view.release()
}
