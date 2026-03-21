package com.thiromano.exact.lsp

import com.intellij.model.Pointer
import com.intellij.model.Symbol
import com.intellij.model.psi.ImplicitReferenceProvider
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

/**
 * Intercepts implicit reference resolution for Elixir declaration positions.
 *
 * ## Why this exists
 *
 * IntelliJ's `LspImplicitReferenceProvider` always returns a reference for
 * every position — even declarations. The implicit reference system uses a
 * first-non-null-wins strategy, so the LSP provider's reference takes
 * priority in the GTDU flow, causing Cmd+Click on a definition to navigate
 * to itself (a no-op) instead of showing usages.
 *
 * ## How it works
 *
 * Registered with `order="first"` so it runs before `LspImplicitReferenceProvider`.
 * For declaration positions (identified via [ElixirDocumentSymbolService]),
 * returns an [ElixirDeclarationSelfReference] that resolves to an
 * [ElixirLspSymbol]. This blocks the LSP provider and causes the GTDU flow
 * to fall through to the "show usages" path via
 * [ElixirLspSymbolSearchTargetFactory].
 *
 * For non-declaration positions, returns `null` so the LSP handles navigation
 * normally.
 */
class ElixirImplicitReferenceProvider : ImplicitReferenceProvider {

    override fun getImplicitReference(element: PsiElement, offset: Int): PsiSymbolReference? {
        val file = element.containingFile?.virtualFile ?: return null
        if (file.extension !in ElixirLspServerSupportProvider.SUPPORTED_EXTENSIONS) return null

        val project = element.project
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return null
        val absoluteOffset = element.textRange.startOffset + offset

        val symbolService = ElixirDocumentSymbolService.getInstance(project)
        val symbols = symbolService.getSymbols(file)
        if (symbols.isEmpty()) return null

        val position = offsetToPosition(document, absoluteOffset)
        val matchingSymbol = findSymbolAt(symbols, position) ?: return null

        val selectionRange = matchingSymbol.selectionRange
        val startOffset = positionToOffset(document, selectionRange.start)
        val endOffset = positionToOffset(document, selectionRange.end)
        val rangeInFile = TextRange(startOffset, endOffset)
        val rangeInElement = rangeInFile.shiftLeft(element.textRange.startOffset)

        if (rangeInElement.startOffset < 0 || rangeInElement.endOffset > element.textLength) {
            return null
        }

        return ElixirDeclarationSelfReference(
            element = element,
            rangeInElement = rangeInElement,
            symbol = ElixirLspSymbol(
                name = matchingSymbol.name,
                file = file,
                selectionRange = selectionRange
            )
        )
    }

    companion object {
        private fun offsetToPosition(document: Document, offset: Int): Position {
            val line = document.getLineNumber(offset)
            val lineStart = document.getLineStartOffset(line)
            return Position(line, offset - lineStart)
        }

        private fun positionToOffset(document: Document, position: Position): Int {
            val lineStart = document.getLineStartOffset(position.line)
            return lineStart + position.character
        }

        private fun findSymbolAt(
            symbols: List<DocumentSymbol>,
            position: Position
        ): DocumentSymbol? {
            for (symbol in symbols) {
                if (positionInRange(position, symbol.selectionRange)) return symbol
                val child = findSymbolAt(symbol.children ?: emptyList(), position)
                if (child != null) return child
            }
            return null
        }

        private fun positionInRange(pos: Position, range: Range): Boolean {
            if (pos.line < range.start.line || pos.line > range.end.line) return false
            if (pos.line == range.start.line && pos.character < range.start.character) return false
            if (pos.line == range.end.line && pos.character >= range.end.character) return false
            return true
        }
    }
}

/**
 * A reference that resolves to a declaration symbol.
 * Blocks the platform's LSP reference so GTDU falls through to "show usages."
 */
private class ElixirDeclarationSelfReference(
    private val element: PsiElement,
    private val rangeInElement: TextRange,
    private val symbol: ElixirLspSymbol
) : PsiSymbolReference {
    override fun getElement(): PsiElement = element
    override fun getRangeInElement(): TextRange = rangeInElement
    override fun resolveReference(): Collection<Symbol> = listOf(symbol)
    override fun resolvesTo(target: Symbol): Boolean = target == symbol
}

/**
 * A [Symbol] representing an Elixir declaration derived from LSP document symbols.
 *
 * Used by [ElixirImplicitReferenceProvider] (which emits it as a self-reference)
 * and [ElixirLspSymbolSearchTargetFactory] (which converts it into an
 * `LspSearchTarget` for the "show usages" flow).
 *
 * Identity is based on name, file, and selection range so that references
 * for the same position produce equal symbols.
 */
class ElixirLspSymbol(
    val name: String,
    val file: VirtualFile,
    val selectionRange: Range
) : Symbol {
    override fun createPointer(): Pointer<out Symbol> = Pointer.hardPointer(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ElixirLspSymbol) return false
        return name == other.name &&
            file == other.file &&
            selectionRange == other.selectionRange
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + selectionRange.hashCode()
        return result
    }
}
