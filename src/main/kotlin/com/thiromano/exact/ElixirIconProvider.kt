package com.thiromano.exact

import com.intellij.ide.IconProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.Icon

class ElixirIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (element is PsiFile) {
            val ext = element.virtualFile?.extension
            if (ext in ELIXIR_EXTENSIONS) {
                return ElixirIcons.ELIXIR
            }
        }
        return null
    }

    companion object {
        // .ex/.exs get their icon from ElixirFileType; only template files need this provider
        private val ELIXIR_EXTENSIONS = setOf("heex", "eex")
    }
}
