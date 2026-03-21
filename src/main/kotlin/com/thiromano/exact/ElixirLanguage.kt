package com.thiromano.exact

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class ElixirLanguage private constructor() : Language("Elixir") {
    companion object {
        @JvmField
        val INSTANCE = ElixirLanguage()
    }
}

/**
 * [LanguageFileType] registered with `.ex` and `.exs` extensions via `plugin.xml`.
 * This lets [com.intellij.psi.PsiFileFactory.createFileFromText] create temporary
 * files for documentation highlighting. The [ElixirEditorHighlighterProvider] delegates
 * to TextMate so that both editor and documentation highlighting use TextMate grammars.
 */
class ElixirFileType private constructor() : LanguageFileType(ElixirLanguage.INSTANCE) {
    override fun getName(): String = "Elixir"
    override fun getDescription(): String = "Elixir file"
    override fun getDefaultExtension(): String = "ex"
    override fun getIcon(): Icon = ElixirIcons.ELIXIR

    companion object {
        @JvmField
        val INSTANCE = ElixirFileType()
    }
}
