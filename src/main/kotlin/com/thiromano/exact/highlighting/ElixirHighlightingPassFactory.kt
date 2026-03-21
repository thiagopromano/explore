package com.thiromano.exact.highlighting

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ElixirHighlightingPassFactoryRegistrar : TextEditorHighlightingPassFactoryRegistrar {
    override fun registerHighlightingPassFactory(
        registrar: TextEditorHighlightingPassRegistrar,
        project: Project
    ) {
        registrar.registerTextEditorHighlightingPass(
            ElixirHighlightingPassFactory(),
            null,
            null,
            false,
            -1
        )
    }
}

private class ElixirHighlightingPassFactory : TextEditorHighlightingPassFactory {
    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        val ext = file.virtualFile?.extension ?: return null
        if (ext !in ELIXIR_EXTENSIONS) return null
        val editorEx = editor as? EditorEx ?: return null
        return ElixirHighlightingPass(file, editorEx)
    }

    companion object {
        private val ELIXIR_EXTENSIONS = setOf("ex", "exs", "heex", "eex")
    }
}

/**
 * Reads TextMate tokens from the editor's highlighter and overlays Elixir-specific
 * TextAttributeKeys on top. This leverages the TextMate grammar's scope assignments
 * rather than reimplementing token detection with regex.
 */
private class ElixirHighlightingPass(
    private val file: PsiFile,
    private val editor: EditorEx
) : TextEditorHighlightingPass(file.project, editor.document, false) {

    private val highlights = mutableListOf<HighlightInfo>()

    override fun doCollectInformation(progress: ProgressIndicator) {
        highlights.clear()

        val highlighter = editor.highlighter
        val iterator = highlighter.createIterator(0)

        while (!iterator.atEnd()) {
            val scope = iterator.tokenType.toString()
            val key = ElixirTextAttributes.mapScopeToKey(scope)

            if (key != null) {
                val info = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
                    .range(iterator.start, iterator.end)
                    .textAttributes(key)
                    .descriptionAndTooltip("")
                    .createUnconditionally()
                highlights.add(info)
            }

            iterator.advance()
        }
    }

    override fun doApplyInformationToEditor() {
        UpdateHighlightersUtil.setHighlightersToEditor(
            myProject, myDocument, 0, file.textLength, highlights, colorsScheme, id
        )
    }

}
