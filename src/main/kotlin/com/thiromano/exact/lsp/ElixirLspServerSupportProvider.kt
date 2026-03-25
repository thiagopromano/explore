package com.thiromano.exact.lsp

import com.thiromano.exact.ElixirIcons
import com.thiromano.exact.settings.ExpertConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.LspServerSupportProvider.LspServerStarter
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem

class ElixirLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerStarter
    ) {
        if (file.extension in SUPPORTED_EXTENSIONS) {
            serverStarter.ensureServerStarted(
                ElixirLspServerDescriptor(project)
            )
        }
    }

    override fun createLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile?) =
        LspServerWidgetItem(lspServer, currentFile, ElixirIcons.ELIXIR, ExpertConfigurable::class.java)

    companion object {
        val SUPPORTED_EXTENSIONS = setOf("ex", "exs", "heex", "eex")
    }
}
