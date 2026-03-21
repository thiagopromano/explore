package com.thiromano.exact.lsp

import com.thiromano.exact.settings.ExpertSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerListener
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ServerCapabilities

class ElixirLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "Expert") {

    override fun isSupportedFile(file: VirtualFile): Boolean =
        file.extension in ElixirLspServerSupportProvider.SUPPORTED_EXTENSIONS

    override fun createCommandLine(): GeneralCommandLine {
        val expertPath = ExpertSettings.getInstance().expertPath
            .takeUnless { it.isNullOrBlank() }
            ?: "expert"

        return GeneralCommandLine(expertPath, "--stdio").apply {
            withWorkDirectory(project.basePath)
        }
    }

    override val lspServerListener = object : LspServerListener {
        override fun serverInitialized(params: InitializeResult) {
            logCapabilities(params.capabilities)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(ElixirLspServerDescriptor::class.java)

        private fun logCapabilities(c: ServerCapabilities) {
            LOG.info(buildString {
                appendLine("Expert LSP server initialized. Capabilities:")
                appendLine("  definitionProvider: ${c.definitionProvider}")
                appendLine("  typeDefinitionProvider: ${c.typeDefinitionProvider}")
                appendLine("  referencesProvider: ${c.referencesProvider}")
                appendLine("  documentSymbolProvider: ${c.documentSymbolProvider}")
                appendLine("  workspaceSymbolProvider: ${c.workspaceSymbolProvider}")
                appendLine("  hoverProvider: ${c.hoverProvider}")
                appendLine("  completionProvider: ${c.completionProvider}")
                appendLine("  codeActionProvider: ${c.codeActionProvider}")
                appendLine("  documentFormattingProvider: ${c.documentFormattingProvider}")
                appendLine("  renameProvider: ${c.renameProvider}")
                appendLine("  implementationProvider: ${c.implementationProvider}")
                appendLine("  declarationProvider: ${c.declarationProvider}")
                appendLine("  documentHighlightProvider: ${c.documentHighlightProvider}")
            })
        }
    }
}
