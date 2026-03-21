package com.thiromano.exact.lsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.LspServerSupportProvider.LspServerStarter

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

    companion object {
        val SUPPORTED_EXTENSIONS = setOf("ex", "exs", "heex", "eex")
    }
}
