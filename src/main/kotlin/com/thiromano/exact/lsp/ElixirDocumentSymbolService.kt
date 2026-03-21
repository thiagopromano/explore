package com.thiromano.exact.lsp

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerManager
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolParams
import java.util.concurrent.ConcurrentHashMap

/**
 * Caches document symbols from the Expert LSP server per file.
 *
 * ## Why this exists
 *
 * IntelliJ's LSP integration uses `textDocument/documentSymbol` only for the
 * Structure view. It does **not** use document symbols to identify declarations
 * in the editor — so IntelliJ cannot distinguish "cursor is on a definition"
 * from "cursor is on a reference." This service bridges that gap by fetching
 * and caching document symbols, which [ElixirImplicitReferenceProvider]
 * then consults to identify declaration positions.
 *
 * ## How it works
 *
 * Symbols are fetched proactively in the background when files are opened or
 * modified, so consumers (called from the EDT) only read from cache and never
 * block on LSP requests. Because the LSP server may still be initializing when
 * the service starts, a retry loop polls until the first successful response
 * before caching symbols for all open files.
 */
@Service(Service.Level.PROJECT)
class ElixirDocumentSymbolService(private val project: Project) : Disposable {

    private val cache = ConcurrentHashMap<String, List<DocumentSymbol>>()

    init {
        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    if (file.extension in ElixirLspServerSupportProvider.SUPPORTED_EXTENSIONS) {
                        fetchInBackground(file)
                    }
                }

                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    cache.remove(file.url)
                }
            }
        )

        EditorFactory.getInstance().eventMulticaster
            .addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    val file = FileDocumentManager.getInstance().getFile(event.document) ?: return
                    if (file.extension !in ElixirLspServerSupportProvider.SUPPORTED_EXTENSIONS) return
                    fetchInBackground(file)
                }
            }, this)

        // The LSP server may still be initializing when this service starts,
        // so we poll until it responds successfully.
        fetchAllOpenFilesWhenReady()
    }

    private fun fetchAllOpenFilesWhenReady() {
        ApplicationManager.getApplication().executeOnPooledThread {
            for (attempt in 1..MAX_INIT_RETRIES) {
                Thread.sleep(RETRY_DELAY_MS)
                val openFiles = FileEditorManager.getInstance(project).openFiles
                val elixirFiles = openFiles.filter {
                    it.extension in ElixirLspServerSupportProvider.SUPPORTED_EXTENSIONS
                }
                if (elixirFiles.isEmpty()) return@executeOnPooledThread

                val testFile = elixirFiles.first()
                val server = LspServerManager.getInstance(project)
                    .getServersForProvider(ElixirLspServerSupportProvider::class.java)
                    .firstOrNull() ?: continue

                val params = DocumentSymbolParams(server.getDocumentIdentifier(testFile))
                val response = try {
                    server.sendRequestSync(TIMEOUT_MS) { ls ->
                        ls.textDocumentService.documentSymbol(params)
                    }
                } catch (_: Exception) { null }

                if (response != null) {
                    val symbols = response.mapNotNull { either ->
                        if (either.isRight) either.right else null
                    }
                    cache[testFile.url] = symbols
                    for (file in elixirFiles.drop(1)) {
                        fetchInBackground(file)
                    }
                    return@executeOnPooledThread
                }
            }
            LOG.warn("Failed to fetch document symbols after $MAX_INIT_RETRIES retries")
        }
    }

    /**
     * Returns cached symbols for the file, or empty if not yet fetched.
     * Never blocks — safe to call from EDT.
     */
    fun getSymbols(file: VirtualFile): List<DocumentSymbol> {
        return cache[file.url] ?: emptyList()
    }

    private fun fetchInBackground(file: VirtualFile) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val server = LspServerManager.getInstance(project)
                .getServersForProvider(ElixirLspServerSupportProvider::class.java)
                .firstOrNull() ?: return@executeOnPooledThread

            val params = DocumentSymbolParams(server.getDocumentIdentifier(file))

            val response = try {
                server.sendRequestSync(TIMEOUT_MS) { ls ->
                    ls.textDocumentService.documentSymbol(params)
                }
            } catch (_: Exception) {
                null
            } ?: return@executeOnPooledThread

            val symbols = response.mapNotNull { either ->
                if (either.isRight) either.right else null
            }

            cache[file.url] = symbols
        }
    }

    override fun dispose() {
        cache.clear()
    }

    companion object {
        private val LOG = Logger.getInstance(ElixirDocumentSymbolService::class.java)
        private const val TIMEOUT_MS = 5000
        private const val RETRY_DELAY_MS = 2000L
        private const val MAX_INIT_RETRIES = 15

        fun getInstance(project: Project): ElixirDocumentSymbolService = project.service()
    }
}
