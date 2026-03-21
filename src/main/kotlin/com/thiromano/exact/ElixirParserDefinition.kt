package com.thiromano.exact

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.EmptyLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Minimal [ParserDefinition] for Elixir — exists solely so that
 * [com.intellij.lang.documentation.QuickDocHighlightingHelper] applies
 * syntax highlighting in documentation popups. It checks
 * `LanguageParserDefinitions.forLanguage(language) != null` before
 * highlighting code blocks.
 *
 * Actual file parsing and highlighting is handled by TextMate and LSP.
 */
class ElixirParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = EmptyLexer()
    override fun createParser(project: Project?): PsiParser = PsiParser { _, builder ->
        val mark = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        mark.done(FILE)
        builder.treeBuilt
    }
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createElement(node: ASTNode?): PsiElement =
        throw UnsupportedOperationException("Not used")
    override fun createFile(viewProvider: FileViewProvider): PsiFile =
        ElixirDummyFile(viewProvider)

    companion object {
        private val FILE = IFileElementType(ElixirLanguage.INSTANCE)
    }
}

private class ElixirDummyFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ElixirLanguage.INSTANCE) {
    override fun getFileType(): FileType = ElixirFileType.INSTANCE
}
