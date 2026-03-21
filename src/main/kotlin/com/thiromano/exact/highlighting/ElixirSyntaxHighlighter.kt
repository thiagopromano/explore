package com.thiromano.exact.highlighting

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.plugins.textmate.language.syntax.highlighting.TextMateSyntaxHighlighterFactory

/**
 * Delegates to TextMate's [TextMateSyntaxHighlighterFactory] by providing a fake `.ex` file,
 * so that documentation popups get Elixir syntax highlighting from the TextMate grammar.
 */
class ElixirSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    private val delegate = TextMateSyntaxHighlighterFactory()

    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        val fakeFile = LightVirtualFile("_.ex")
        return try {
            delegate.getSyntaxHighlighter(project, fakeFile)
        } catch (e: Exception) {
            notifyError("Failed to create Elixir syntax highlighter: ${e.message}", project)
            PlainSyntaxHighlighter()
        }
    }

    private fun notifyError(message: String, project: Project?) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Elixir")
            .createNotification(message, NotificationType.ERROR)
            .notify(project)
    }
}
