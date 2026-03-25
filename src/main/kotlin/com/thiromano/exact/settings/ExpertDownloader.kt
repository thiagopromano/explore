package com.thiromano.exact.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.platform.lsp.api.LspServerManager
import com.thiromano.exact.lsp.ElixirLspServerSupportProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

object ExpertDownloader {

    private const val DOWNLOAD_BASE_URL =
        "https://github.com/elixir-lang/expert/releases/download/nightly"

    val destinationFile: File
        get() = File(System.getProperty("user.home"), ".expert${File.separator}expert")

    fun getAssetName(): String? {
        val os = when {
            SystemInfo.isMac -> "darwin"
            SystemInfo.isLinux -> "linux"
            SystemInfo.isWindows -> "windows"
            else -> return null
        }
        val arch = when (System.getProperty("os.arch")) {
            "aarch64", "arm64" -> "arm64"
            "amd64", "x86_64" -> "amd64"
            else -> return null
        }
        val suffix = if (SystemInfo.isWindows) ".exe" else ""
        return "expert_${os}_${arch}${suffix}"
    }

    fun download(project: Project?, onSuccess: ((File) -> Unit)? = null, onError: (() -> Unit)? = null) {
        val assetName = getAssetName() ?: return
        val url = "$DOWNLOAD_BASE_URL/$assetName"
        val destFile = destinationFile

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Downloading Expert", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.text = "Downloading Expert Language Server..."

                destFile.parentFile.mkdirs()

                val connection = followRedirects(URI(url).toURL().openConnection() as HttpURLConnection)
                try {
                    val contentLength = connection.contentLengthLong
                    connection.inputStream.use { input ->
                        destFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var totalRead = 0L
                            while (true) {
                                indicator.checkCanceled()
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                totalRead += read
                                if (contentLength > 0) {
                                    indicator.fraction = totalRead.toDouble() / contentLength
                                }
                            }
                        }
                    }
                    destFile.setExecutable(true)
                } finally {
                    connection.disconnect()
                }

                ApplicationManager.getApplication().invokeLater {
                    onSuccess?.invoke(destFile)
                }
            }

            override fun onThrowable(error: Throwable) {
                ApplicationManager.getApplication().invokeLater {
                    onError?.invoke()
                }
            }
        })
    }

    fun applyAndRestart(project: Project, destFile: File) {
        val settings = ExpertSettings.getInstance()
        settings.expertPath = destFile.absolutePath
        LspServerManager.getInstance(project)
            .stopAndRestartIfNeeded(ElixirLspServerSupportProvider::class.java)
    }

    private fun followRedirects(connection: HttpURLConnection): HttpURLConnection {
        var conn = connection
        var redirects = 0
        while (redirects < 5) {
            conn.instanceFollowRedirects = false
            conn.connect()
            val code = conn.responseCode
            if (code in 301..303 || code == 307 || code == 308) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                conn = URI(location).toURL().openConnection() as HttpURLConnection
                redirects++
            } else {
                return conn
            }
        }
        return conn
    }
}
