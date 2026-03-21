package com.thiromano.exact.highlighting

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.HighlighterClient
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.EditorHighlighterProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.plugins.textmate.language.syntax.highlighting.TextMateEditorHighlighterProvider

/**
 * Delegates to TextMate's [TextMateEditorHighlighterProvider] and wraps the resulting
 * highlighter to overlay Elixir-specific [com.intellij.openapi.editor.colors.TextAttributesKey]s.
 * This ensures both the main editor and documentation popups use the same custom colors.
 */
class ElixirEditorHighlighterProvider : EditorHighlighterProvider {
    private val delegate = TextMateEditorHighlighterProvider()

    override fun getEditorHighlighter(
        project: Project?,
        fileType: FileType,
        virtualFile: VirtualFile?,
        colors: EditorColorsScheme
    ): EditorHighlighter {
        val textMateHighlighter = delegate.getEditorHighlighter(project, fileType, virtualFile, colors)
        return ElixirEditorHighlighter(textMateHighlighter, colors)
    }
}

/**
 * Wraps a TextMate [EditorHighlighter] and overlays Elixir-specific text attributes
 * on each token based on its TextMate scope.
 */
private class ElixirEditorHighlighter(
    private val delegate: EditorHighlighter,
    private val colorsScheme: EditorColorsScheme
) : EditorHighlighter {

    override fun createIterator(startOffset: Int): HighlighterIterator {
        return ElixirHighlighterIterator(delegate.createIterator(startOffset), colorsScheme)
    }

    override fun setText(text: CharSequence) = delegate.setText(text)
    override fun setEditor(editor: HighlighterClient) = delegate.setEditor(editor)
    override fun setColorScheme(scheme: EditorColorsScheme) = delegate.setColorScheme(scheme)
    override fun beforeDocumentChange(event: DocumentEvent) = delegate.beforeDocumentChange(event)
    override fun documentChanged(event: DocumentEvent) = delegate.documentChanged(event)
}

private class ElixirHighlighterIterator(
    private val delegate: HighlighterIterator,
    private val colorsScheme: EditorColorsScheme
) : HighlighterIterator {

    override fun getTextAttributes(): TextAttributes {
        val base = delegate.textAttributes
        val key = ElixirTextAttributes.mapScopeToKey(tokenType.toString()) ?: return base
        val override = colorsScheme.getAttributes(key) ?: return base
        return TextAttributes.merge(base, override)
    }

    override fun getStart(): Int = delegate.start
    override fun getEnd(): Int = delegate.end
    override fun getTokenType(): IElementType = delegate.tokenType
    override fun advance() = delegate.advance()
    override fun retreat() = delegate.retreat()
    override fun atEnd(): Boolean = delegate.atEnd()
    override fun getDocument(): Document = delegate.document
}
