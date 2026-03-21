package com.thiromano.exact.lsp

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.platform.lsp.api.LspServerManager
import org.eclipse.lsp4j.ExecuteCommandParams

class RestartExpertAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        LspServerManager.getInstance(project)
            .stopAndRestartIfNeeded(ElixirLspServerSupportProvider::class.java)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}

class ReindexExpertAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val servers = LspServerManager.getInstance(project)
            .getServersForProvider(ElixirLspServerSupportProvider::class.java)
        val server = servers.firstOrNull() ?: return

        server.sendNotification { languageServer ->
            languageServer.workspaceService.executeCommand(
                ExecuteCommandParams("Reindex", emptyList())
            )
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
