package com.thiromano.exact.lsp

import com.intellij.find.usages.api.PsiUsage
import com.intellij.find.usages.api.SearchTarget
import com.intellij.find.usages.api.Usage
import com.intellij.find.usages.api.UsageHandler
import com.intellij.find.usages.api.UsageSearchParameters
import com.intellij.find.usages.api.UsageSearcher
import com.intellij.find.usages.symbol.SymbolSearchTargetFactory
import com.intellij.model.Pointer
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.psi.PsiManager
import com.intellij.util.AbstractQuery
import com.intellij.util.Processor
import com.intellij.util.Query
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.ReferenceContext
import org.eclipse.lsp4j.ReferenceParams

class ElixirLspSymbolSearchTargetFactory : SymbolSearchTargetFactory<ElixirLspSymbol> {

    override fun searchTarget(project: Project, symbol: ElixirLspSymbol): SearchTarget? {
        val servers = LspServerManager.getInstance(project)
            .getServersForProvider(ElixirLspServerSupportProvider::class.java)
        if (servers.isEmpty()) return null

        return ElixirSearchTarget(
            project = project,
            file = symbol.file,
            position = symbol.selectionRange.start
        )
    }
}

class ElixirSearchTarget(
    val project: Project,
    val file: VirtualFile,
    val position: Position
) : SearchTarget {

    override fun createPointer(): Pointer<out SearchTarget> = Pointer.hardPointer(this)

    override fun presentation(): TargetPresentation {
        val name = "${file.name}:${position.line + 1}:${position.character + 1}"
        return TargetPresentation.builder(name).presentation()
    }

    override val usageHandler: UsageHandler get() = UsageHandler { "Elixir references" }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ElixirSearchTarget) return false
        return file == other.file && position == other.position
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + position.hashCode()
        return result
    }
}

class ElixirUsageSearcher : UsageSearcher {

    override fun collectSearchRequest(parameters: UsageSearchParameters): Query<out Usage>? {
        val target = parameters.target as? ElixirSearchTarget ?: return null
        return ElixirReferencesQuery(target)
    }
}

private class ElixirReferencesQuery(
    private val target: ElixirSearchTarget
) : AbstractQuery<Usage>() {

    override fun processResults(consumer: Processor<in Usage>): Boolean {
        val servers = LspServerManager.getInstance(target.project)
            .getServersForProvider(ElixirLspServerSupportProvider::class.java)

        for (server in servers) {
            val docId = server.getDocumentIdentifier(target.file)
            val params = ReferenceParams(docId, target.position, ReferenceContext(false))

            val locations = try {
                server.sendRequestSync(60_000) { ls ->
                    ls.textDocumentService.references(params)
                }
            } catch (_: Exception) {
                null
            }

            if (locations.isNullOrEmpty()) continue

            ReadAction.run<Nothing> {
                val psiManager = PsiManager.getInstance(target.project)
                for (location in locations) {
                    val file = server.descriptor.findFileByUri(location.uri) ?: continue
                    val psiFile = psiManager.findFile(file) ?: continue
                    val document = psiFile.fileDocument
                    val range = lspRangeToTextRange(document, location.range) ?: continue
                    consumer.process(PsiUsage.textUsage(psiFile, range))
                }
            }
        }
        return true
    }

    companion object {
        private fun lspRangeToTextRange(document: Document, range: org.eclipse.lsp4j.Range): TextRange? {
            val startLine = range.start.line
            val endLine = range.end.line
            if (startLine < 0 || endLine >= document.lineCount) return null
            val startOffset = document.getLineStartOffset(startLine) + range.start.character
            val endOffset = document.getLineStartOffset(endLine) + range.end.character
            if (startOffset > document.textLength || endOffset > document.textLength) return null
            return TextRange(startOffset, endOffset)
        }
    }
}
