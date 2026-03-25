package com.thiromano.exact.lsp

import com.intellij.navigation.ChooseByNameContributorEx2
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.DumbAware
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.customization.LspWorkspaceSymbolNavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.WorkspaceSymbol
import org.eclipse.lsp4j.WorkspaceSymbolParams

/**
 * Contributes Elixir modules to the "Classes" tab in Search Everywhere.
 *
 * The platform's `LspGoToClassContributor` only accepts Class, Interface,
 * Enum, and Struct — not Module. Since Elixir modules use [SymbolKind.Module],
 * they don't appear in the "Classes" category without this contributor.
 */
class ElixirGoToClassContributor : ChooseByNameContributorEx2, DumbAware {

    override fun processNames(
        processor: Processor<in String>,
        parameters: FindSymbolParameters
    ) {
        for (item in getModuleSymbols(parameters.searchScope, parameters.completePattern)) {
            val name = item.name ?: continue
            if (!processor.process(name)) return
        }
    }

    override fun processNames(
        processor: Processor<in String>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ) {
        for (item in getModuleSymbols(scope, "")) {
            val name = item.name ?: continue
            if (!processor.process(name)) return
        }
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) {
        for (item in getModuleSymbols(parameters.searchScope, name)) {
            if (item.name == name) {
                if (!processor.process(item)) return
            }
        }
    }

    private fun getModuleSymbols(
        scope: GlobalSearchScope,
        pattern: String
    ): List<NavigationItem> {
        val project = scope.project ?: return emptyList()
        val servers = LspServerManager.getInstance(project)
            .getServersForProvider(ElixirLspServerSupportProvider::class.java)

        val results = mutableListOf<NavigationItem>()
        for (server in servers) {
            val params = WorkspaceSymbolParams(pattern)
            val response = try {
                server.sendRequestSync(5000) { ls ->
                    ls.workspaceService.symbol(params)
                }
            } catch (_: Exception) {
                null
            } ?: continue

            val symbols: List<WorkspaceSymbol> = when {
                response.isRight -> response.right?.filterIsInstance<WorkspaceSymbol>() ?: continue
                response.isLeft -> continue // SymbolInformation format not supported
                else -> continue
            }

            for (symbol in symbols) {
                if (symbol.kind != SymbolKind.Module) continue

                val uri = symbol.location?.left?.uri ?: symbol.location?.right?.uri ?: continue
                val file = server.descriptor.findFileByUri(uri) ?: continue
                if (!scope.contains(file)) continue

                results.add(LspWorkspaceSymbolNavigationItem(server, symbol))
            }
        }
        return results
    }
}
