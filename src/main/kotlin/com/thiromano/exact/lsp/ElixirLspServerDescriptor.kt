package com.thiromano.exact.lsp

import com.thiromano.exact.settings.ExpertConfigurable
import com.thiromano.exact.settings.ExpertSettings
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerListener
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.*
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SymbolKind
import javax.swing.Icon

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

    override fun createInitializationOptions(): Any {
        val settings = ExpertSettings.getInstance()
        return JsonObject().apply {
            add("workspaceSymbols", JsonObject().apply {
                addProperty("minQueryLength", settings.minQueryLength)
            })
            add("additionalWatchedExtensions", JsonArray().apply {
                add(JsonPrimitive(".heex"))
                add(JsonPrimitive(".eex"))
            })
        }
    }

    override fun startServerProcess(): OSProcessHandler {
        try {
            return super.startServerProcess()
        } catch (e: ExecutionException) {
            val expertPath = ExpertSettings.getInstance().expertPath
                .takeUnless { it.isNullOrBlank() }
                ?: "expert"

            NotificationGroupManager.getInstance()
                .getNotificationGroup("Elixir")
                .createNotification(
                    "Expert Language Server not found",
                    "Could not start \"$expertPath\". Install Expert and ensure it is on your PATH, " +
                        "or configure the binary path in Settings.",
                    NotificationType.WARNING
                )
                .addAction(object : NotificationAction("Configure Expert Path") {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, ExpertConfigurable::class.java)
                        notification.expire()
                    }
                })
                .notify(project)

            throw e
        }
    }

    // Syntax highlighting uses TextMate grammars, not LSP semantic tokens.
    // Expert does not support: semantic tokens, type definition, implementation,
    // declaration, rename, folding, inlay hints, signature help, document highlights,
    // selection range, call/type hierarchy.
    override val lspCustomization = object : LspCustomization() {
        override val semanticTokensCustomizer = LspSemanticTokensDisabled
        override val goToTypeDefinitionCustomizer = LspGoToTypeDefinitionDisabled
        override val foldingRangeCustomizer = LspFoldingRangeDisabled
        override val inlayHintCustomizer = LspInlayHintDisabled
        override val documentHighlightsCustomizer = LspDocumentHighlightsDisabled
        override val signatureHelpCustomizer = LspSignatureHelpDisabled
        override val selectionRangeCustomizer = LspSelectionRangeDisabled

        override val symbolKindCustomizer = object : LspSymbolKindCustomizer() {
            override fun getIcon(symbolKind: SymbolKind): Icon = when (symbolKind) {
                SymbolKind.Module -> AllIcons.Nodes.Class
                SymbolKind.Struct -> AllIcons.Nodes.Record
                else -> super.getIcon(symbolKind) ?: AllIcons.Nodes.Class
            }
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
